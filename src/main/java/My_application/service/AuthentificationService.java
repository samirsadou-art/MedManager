package My_application.service;

import My_application.dao.DAOFactory;
import My_application.dao.DataAccessException;
import My_application.dao.UniteDeTravail;
import My_application.dao.UtilisateurDao;
import My_application.entities.Compte;
import My_application.entities.User;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Connexion des utilisateurs (écran de connexion) et gestion de leurs identifiants.
 * <ul>
 * <li>mots de passe stockés sous forme d'empreinte PBKDF2 ({@link MotDePasse}) ;</li>
 * <li>même message pour un identifiant inconnu, un compte désactivé ou un
 * mauvais mot de passe : on ne révèle pas quels identifiants existent ;</li>
 * <li>après {@value #TENTATIVES_MAX} échecs de suite sur un identifiant, il
 * faut attendre {@code ATTENTE} avant de réessayer ;</li>
 * <li>connexions réussies et échouées inscrites au journal des accès.</li>
 * </ul>
 * Première utilisation : tant qu'aucun compte n'existe, l'utilisateur par
 * défaut ({@code -Dgestimed.utilisateur}, 1) choisit son identifiant et son
 * mot de passe.
 */
public class AuthentificationService {

    static final int TENTATIVES_MAX = 5;
    static final Duration ATTENTE = Duration.ofSeconds(30);
    static final String REFUS = "Identifiant ou mot de passe incorrect.";
    static final String BASE_NON_MIGREE = "La base de données n'est pas à jour pour l'écran de connexion.\n"
            + "Exécutez une fois le script sql/migration_002_authentification.sql (voir la commande en tête du fichier).";

    /** Codes MySQL/MariaDB : colonne inconnue, table inconnue. */
    private static final int COLONNE_INCONNUE = 1054;
    private static final int TABLE_INCONNUE = 1146;

    private final DAOFactory daos;
    private final UtilisateurDao utilisateurDao;
    private final Session session;
    private final JournalAcces journal;
    private final Clock horloge;
    private final Map<String, Echecs> echecs = new HashMap<>();

    private static final class Echecs {
        int nombre;
        Instant bloqueJusqua = Instant.MIN;
    }

    public AuthentificationService(DAOFactory daos, Session session, JournalAcces journal) {
        this(daos, session, journal, Clock.systemDefaultZone());
    }

    public AuthentificationService(DAOFactory daos, Session session, JournalAcces journal, Clock horloge) {
        this.daos = daos;
        this.utilisateurDao = daos.getUtilisateurDao();
        this.session = session;
        this.journal = journal;
        this.horloge = horloge;
    }

    /** Vrai tant qu'aucun utilisateur n'a d'identifiants : il faut créer le premier compte. */
    public boolean premiereUtilisation() {
        try {
            return !utilisateurDao.existeUnCompte();
        } catch (DataAccessException e) {
            throw siBaseNonMigree(e);
        }
    }

    /** Utilisateur dont on crée le compte à la première utilisation. */
    public User utilisateurInitial() {
        User u = utilisateurDao.findById(session.getIdUtilisateurParDefaut());
        if (u == null) {
            throw new MetierException("L'utilisateur n° " + session.getIdUtilisateurParDefaut()
                    + " est introuvable dans la base (voir sql/donnees_initiales.sql).");
        }
        return u;
    }

    /** Crée le premier compte et ouvre la session ; refusé dès qu'un compte existe. */
    public User creerPremierCompte(final String login, final char[] motDePasse, final char[] confirmation) {
        if (!premiereUtilisation()) {
            throw new MetierException("Un compte existe déjà : connectez-vous avec vos identifiants.");
        }
        final User u = utilisateurInitial();
        final String identifiant = normaliserLogin(login);
        verifierNouveauMotDePasse(motDePasse, confirmation);
        final String empreinte = MotDePasse.hacher(motDePasse);
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                utilisateurDao.definirIdentifiants(u.getIdent(), identifiant, empreinte);
                u.setLogin(identifiant);
                session.ouvrir(u);
                journal.noter(JournalAcces.Action.CREATION_COMPTE, null, identifiant);
                journal.noter(JournalAcces.Action.CONNEXION, null, null);
                return null;
            }
        });
        return u;
    }

    /**
     * Vérifie les identifiants et ouvre la session.
     *
     * @throws MetierException si la connexion est refusée (message affichable)
     */
    public synchronized User connecter(String login, char[] motDePasse) {
        String identifiant = login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
        if (identifiant.isEmpty() || motDePasse == null || motDePasse.length == 0) {
            throw new MetierException("Saisissez votre identifiant et votre mot de passe.");
        }
        Instant maintenant = horloge.instant();
        Echecs e = echecs.get(identifiant);
        if (e != null && maintenant.isBefore(e.bloqueJusqua)) {
            long secondes = Math.max(1, Duration.between(maintenant, e.bloqueJusqua).getSeconds());
            throw new MetierException("Trop de tentatives échouées. Réessayez dans " + secondes + " secondes.");
        }
        try {
            Compte compte = utilisateurDao.findCompte(identifiant);
            boolean accepte;
            if (compte == null) {
                // Même coût de calcul qu'un vrai compte : la durée ne trahit pas l'identifiant.
                MotDePasse.verifier(motDePasse, EmpreinteFactice.VALEUR);
                accepte = false;
            } else {
                accepte = MotDePasse.verifier(motDePasse, compte.getEmpreinte()) && compte.getUtilisateur().isActif();
            }
            if (!accepte) {
                echouer(identifiant, maintenant);
                journal.noter(JournalAcces.Action.ECHEC_CONNEXION, null, "identifiant : " + identifiant);
                throw new MetierException(REFUS);
            }
            echecs.remove(identifiant);
            User u = compte.getUtilisateur();
            session.ouvrir(u);
            journal.noter(JournalAcces.Action.CONNEXION, null, null);
            return u;
        } catch (DataAccessException ex) {
            throw siBaseNonMigree(ex);
        }
    }

    /** Définit (ou remplace) l'identifiant et le mot de passe d'un utilisateur (outil GestionComptes). */
    public void definirIdentifiants(int idUtilisateur, String login, char[] motDePasse, char[] confirmation) {
        if (utilisateurDao.findById(idUtilisateur) == null) {
            throw new MetierException("L'utilisateur n° " + idUtilisateur + " est introuvable.");
        }
        String identifiant = normaliserLogin(login);
        Compte existant = utilisateurDao.findCompte(identifiant);
        if (existant != null && existant.getUtilisateur().getIdent() != idUtilisateur) {
            throw new MetierException("L'identifiant « " + identifiant + " » est déjà utilisé.");
        }
        verifierNouveauMotDePasse(motDePasse, confirmation);
        final String empreinte = MotDePasse.hacher(motDePasse);
        final int id = idUtilisateur;
        final String l = identifiant;
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                utilisateurDao.definirIdentifiants(id, l, empreinte);
                journal.noter(JournalAcces.Action.MODIFICATION_COMPTE, null,
                        "identifiants de l'utilisateur n° " + id + " (" + l + ")");
                return null;
            }
        });
    }

    /** Active ou désactive un compte (un compte désactivé ne peut plus se connecter). */
    public void definirActif(final int idUtilisateur, final boolean actif) {
        if (utilisateurDao.findById(idUtilisateur) == null) {
            throw new MetierException("L'utilisateur n° " + idUtilisateur + " est introuvable.");
        }
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                utilisateurDao.definirActif(idUtilisateur, actif);
                journal.noter(JournalAcces.Action.MODIFICATION_COMPTE, null,
                        (actif ? "activation" : "désactivation") + " de l'utilisateur n° " + idUtilisateur);
                return null;
            }
        });
    }

    /** Identifiant en minuscules, de 3 à 40 caractères parmi a-z, 0-9, « . », « _ » et « - ». */
    static String normaliserLogin(String login) {
        String l = login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
        if (!l.matches("[a-z0-9._-]{3,40}")) {
            throw new MetierException("L'identifiant doit faire de 3 à 40 caractères : lettres sans accent, "
                    + "chiffres, point, tiret ou souligné.");
        }
        return l;
    }

    private static void verifierNouveauMotDePasse(char[] motDePasse, char[] confirmation) {
        MotDePasse.verifierRobustesse(motDePasse);
        if (confirmation == null || !java.util.Arrays.equals(motDePasse, confirmation)) {
            throw new MetierException("Les deux mots de passe saisis sont différents.");
        }
    }

    private void echouer(String identifiant, Instant maintenant) {
        Echecs e = echecs.get(identifiant);
        if (e == null) {
            e = new Echecs();
            echecs.put(identifiant, e);
        }
        e.nombre++;
        if (e.nombre >= TENTATIVES_MAX) {
            e.nombre = 0;
            e.bloqueJusqua = maintenant.plus(ATTENTE);
        }
    }

    /** Traduit « colonne ou table inconnue » (migration non faite) en message clair. */
    private static RuntimeException siBaseNonMigree(DataAccessException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException) {
                int code = ((SQLException) t).getErrorCode();
                if (code == COLONNE_INCONNUE || code == TABLE_INCONNUE) {
                    return new MetierException(BASE_NON_MIGREE);
                }
            }
        }
        return e;
    }

    /** Empreinte calculée une fois, au premier identifiant inconnu. */
    private static final class EmpreinteFactice {
        static final String VALEUR = MotDePasse.hacher("gestimed-factice-0".toCharArray());
    }
}
