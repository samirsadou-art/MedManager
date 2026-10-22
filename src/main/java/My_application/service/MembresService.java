package My_application.service;

import My_application.dao.DAOFactory;
import My_application.dao.DoublonException;
import My_application.dao.MedecinDao;
import My_application.dao.UniteDeTravail;
import My_application.dao.UtilisateurDao;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.entities.medecin;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Membres du cabinet (table {@code utilisateur}) : réservé aux comptes
 * administrateurs. Les identifiants de connexion se règlent ensuite avec
 * {@link AuthentificationService#definirIdentifiants}.
 * <p>
 * Règles : un médecin est rattaché à sa fiche {@code medecin} (active) ; une
 * secrétaire ou un administrateur ne l'est pas ; le cabinet garde toujours au
 * moins un administrateur actif. Un compte créé ou réinitialisé depuis la page
 * Membres reçoit un mot de passe provisoire, à changer à la première
 * connexion. Une fiche médecin n'est jamais supprimée : elle est désactivée
 * quand le médecin quitte le cabinet (ses consultations restent).
 */
public class MembresService {

    private final DAOFactory daos;
    private final UtilisateurDao utilisateurDao;
    private final JournalAcces journal;
    private final MedecinDao medecinDao;
    private final ControleAcces acces;
    private final Clock horloge;

    public MembresService(DAOFactory daos, JournalAcces journal, ControleAcces acces) {
        this(daos, journal, acces, Clock.systemDefaultZone());
    }

    public MembresService(DAOFactory daos, JournalAcces journal, ControleAcces acces, Clock horloge) {
        this.daos = daos;
        this.utilisateurDao = daos.getUtilisateurDao();
        this.medecinDao = daos.getMedecinDao();
        this.journal = journal;
        this.acces = acces;
        this.horloge = horloge;
    }

    /** Tous les membres, actifs ou non, par nom. */
    public List<User> lister() {
        acces.exiger(Role.ADMIN);
        return utilisateurDao.findAll();
    }

    /** Ajoute un membre, sans identifiants de connexion. */
    public User ajouter(String nom, String prenom, Role role, Integer idMedecin) {
        acces.exiger(Role.ADMIN);
        final User u = new User(0, obligatoire(nom, "nom").toUpperCase(java.util.Locale.FRENCH),
                PatientService.capitaliser(obligatoire(prenom, "prénom")), role.getLibelle());
        u.setRole(role);
        u.setIdMedecin(medecinRattache(role, idMedecin));
        return daos.enTransaction(new UniteDeTravail<User>() {
            @Override
            public User executer() {
                User cree = utilisateurDao.insert(u);
                journal.noter(JournalAcces.Action.CREATION_COMPTE, null,
                        "membre n° " + cree.getIdent() + " (" + cree + ", " + role.getLibelle() + ")");
                return cree;
            }
        });
    }

    /** Change le rôle d'un membre (et son médecin rattaché). */
    public void definirRole(final int idUtilisateur, final Role role, Integer idMedecin) {
        acces.exiger(Role.ADMIN);
        User u = utilisateurDao.findById(idUtilisateur);
        if (u == null) {
            throw new MetierException("L'utilisateur n° " + idUtilisateur + " est introuvable.");
        }
        if (u.getRole() == Role.ADMIN && role != Role.ADMIN) {
            verifierQuIlResteUnAdministrateur(utilisateurDao, idUtilisateur);
        }
        final Integer medecin = medecinRattache(role, idMedecin);
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                utilisateurDao.definirRole(idUtilisateur, role, medecin);
                journal.noter(JournalAcces.Action.MODIFICATION_COMPTE, null,
                        "rôle de l'utilisateur n° " + idUtilisateur + " : " + role.getLibelle());
                return null;
            }
        });
    }

    /**
     * Crée un membre avec son identifiant et un mot de passe provisoire, à
     * changer à sa première connexion. Pour un médecin : {@code idMedecin}
     * désigne une fiche existante, sinon {@code nouvelleFiche} est créée
     * dans la même transaction.
     */
    public User creerCompte(String nom, String prenom, final Role role, Integer idMedecin,
            final medecin nouvelleFiche, String login, char[] motDePasse, char[] confirmation) {
        acces.exiger(Role.ADMIN);
        final User u = new User(0, obligatoire(nom, "nom").toUpperCase(Locale.FRENCH),
                PatientService.capitaliser(obligatoire(prenom, "prénom")), role.getLibelle());
        u.setRole(role);
        final boolean avecFiche = role == Role.MEDECIN && idMedecin == null && nouvelleFiche != null;
        if (avecFiche) {
            verifierFiche(nouvelleFiche);
        } else {
            u.setIdMedecin(medecinRattache(role, idMedecin));
        }
        final String identifiant = AuthentificationService.normaliserLogin(login);
        if (utilisateurDao.findCompte(identifiant) != null) {
            throw new MetierException("L'identifiant « " + identifiant + " » est déjà utilisé.");
        }
        AuthentificationService.verifierNouveauMotDePasse(motDePasse, confirmation);
        final String empreinte = MotDePasse.hacher(motDePasse);
        try {
            return daos.enTransaction(new UniteDeTravail<User>() {
                @Override
                public User executer() {
                    if (avecFiche) {
                        nouvelleFiche.setActif(true);
                        medecin m = medecinDao.insert(nouvelleFiche);
                        u.setIdMedecin(m.getIdmedecin());
                        journal.noter(JournalAcces.Action.MODIFICATION_COMPTE, null,
                                "fiche médecin n° " + m.getIdmedecin() + " (" + m + ") créée");
                    }
                    User cree = utilisateurDao.insert(u);
                    utilisateurDao.definirIdentifiants(cree.getIdent(), identifiant, empreinte);
                    utilisateurDao.definirMotDePasse(cree.getIdent(), empreinte, true);
                    cree.setLogin(identifiant);
                    cree.setDoitChanger(true);
                    journal.noter(JournalAcces.Action.CREATION_COMPTE, null, "membre n° " + cree.getIdent()
                            + " (" + cree + ", " + role.getLibelle() + ", identifiant " + identifiant + ")");
                    return cree;
                }
            });
        } catch (DoublonException e) {
            throw new MetierException(avecFiche ? DOUBLON_ORDRE : "L'identifiant « " + identifiant
                    + " » est déjà utilisé.");
        }
    }

    /** Nom, prénom, rôle et médecin rattaché d'un membre. */
    public void modifier(final int idUtilisateur, String nom, String prenom, final Role role, Integer idMedecin) {
        acces.exiger(Role.ADMIN);
        final User u = existant(idUtilisateur);
        final Role avant = u.getRole();
        final Integer medecinAvant = u.getIdMedecin();
        if (avant == Role.ADMIN && role != Role.ADMIN && u.isActif()) {
            verifierQuIlResteUnAdministrateur(utilisateurDao, idUtilisateur);
        }
        u.setNom(obligatoire(nom, "nom").toUpperCase(Locale.FRENCH));
        u.setPrenom(PatientService.capitaliser(obligatoire(prenom, "prénom")));
        u.setQuailite(role.getLibelle());
        final Integer medecin = medecinRattache(role, idMedecin);
        if (role == Role.MEDECIN && u.isActif() && !medecin.equals(medecinAvant)) {
            verifierFicheActive(medecin);
        }
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                utilisateurDao.modifier(u);
                if (role != avant || !java.util.Objects.equals(medecin, medecinAvant)) {
                    utilisateurDao.definirRole(idUtilisateur, role, medecin);
                }
                journal.noter(JournalAcces.Action.MODIFICATION_COMPTE, null,
                        "membre n° " + idUtilisateur + " (" + u + ", " + role.getLibelle() + ")");
                return null;
            }
        });
    }

    /**
     * Remplace le mot de passe d'un membre par un mot de passe provisoire
     * (oubli) : il devra le changer à sa prochaine connexion.
     */
    public void motDePasseProvisoire(final int idUtilisateur, char[] motDePasse, char[] confirmation) {
        acces.exiger(Role.ADMIN);
        User u = existant(idUtilisateur);
        if (u.getLogin() == null) {
            throw new MetierException("Ce membre n'a pas encore d'identifiant de connexion.");
        }
        AuthentificationService.verifierNouveauMotDePasse(motDePasse, confirmation);
        final String empreinte = MotDePasse.hacher(motDePasse);
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                utilisateurDao.definirMotDePasse(idUtilisateur, empreinte, true);
                journal.noter(JournalAcces.Action.MODIFICATION_COMPTE, null,
                        "mot de passe provisoire pour l'utilisateur n° " + idUtilisateur);
                return null;
            }
        });
    }

    /** Active ou désactive un compte (un compte désactivé ne peut plus se connecter). */
    public void definirActif(final int idUtilisateur, final boolean actif) {
        acces.exiger(Role.ADMIN);
        User u = existant(idUtilisateur);
        if (!actif && u.getRole() == Role.ADMIN) {
            verifierQuIlResteUnAdministrateur(utilisateurDao, idUtilisateur);
        }
        if (actif && u.getRole() == Role.MEDECIN && u.getIdMedecin() != null) {
            verifierFicheActive(u.getIdMedecin());
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

    /** Toutes les fiches médecin, actives ou non. */
    public List<medecin> medecins() {
        acces.exiger(Role.ADMIN);
        return medecinDao.findAll();
    }

    /** Enregistre les coordonnées d'une fiche médecin existante. */
    public void modifierMedecin(final medecin m) {
        acces.exiger(Role.ADMIN);
        if (medecinDao.findById(m.getIdmedecin()) == null) {
            throw new MetierException("Le médecin n° " + m.getIdmedecin() + " est introuvable.");
        }
        verifierFiche(m);
        try {
            daos.enTransaction(new UniteDeTravail<Void>() {
                @Override
                public Void executer() {
                    medecinDao.modifier(m);
                    journal.noter(JournalAcces.Action.MODIFICATION_COMPTE, null,
                            "fiche médecin n° " + m.getIdmedecin() + " (" + m + ") modifiée");
                    return null;
                }
            });
        } catch (DoublonException e) {
            throw new MetierException(DOUBLON_ORDRE);
        }
    }

    /**
     * Active ou désactive une fiche médecin. Un médecin désactivé n'est plus
     * proposé dans l'agenda ; c'est refusé tant qu'il a des rendez-vous à
     * venir ou un compte actif.
     */
    public void definirMedecinActif(final int idMedecin, final boolean actif) {
        acces.exiger(Role.ADMIN);
        medecin m = medecinDao.findById(idMedecin);
        if (m == null) {
            throw new MetierException("Le médecin n° " + idMedecin + " est introuvable.");
        }
        if (!actif) {
            int prevus = daos.getRendezVousDao().compterPrevus(idMedecin, LocalDate.now(horloge));
            if (prevus > 0) {
                throw new MetierException(m + " a encore " + prevus + " rendez-vous à venir : "
                        + "déplacez-les ou annulez-les d'abord.");
            }
            for (User u : utilisateurDao.findAll()) {
                if (u.isActif() && u.getRole() == Role.MEDECIN && Integer.valueOf(idMedecin).equals(u.getIdMedecin())) {
                    throw new MetierException("Le compte de " + u + " est rattaché à cette fiche : "
                            + "désactivez-le d'abord.");
                }
            }
        }
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                medecinDao.definirActif(idMedecin, actif);
                journal.noter(JournalAcces.Action.MODIFICATION_COMPTE, null,
                        (actif ? "activation" : "désactivation") + " de la fiche médecin n° " + idMedecin);
                return null;
            }
        });
    }

    static final String DOUBLON_ORDRE = "Ce numéro d'ordre est déjà celui d'un autre médecin.";

    private User existant(int idUtilisateur) {
        User u = utilisateurDao.findById(idUtilisateur);
        if (u == null) {
            throw new MetierException("L'utilisateur n° " + idUtilisateur + " est introuvable.");
        }
        return u;
    }

    private void verifierFicheActive(int idMedecin) {
        medecin m = medecinDao.findById(idMedecin);
        if (m != null && !m.isActif()) {
            throw new MetierException("La fiche de " + m + " est désactivée : activez-la d'abord.");
        }
    }

    /** Nom et prénom obligatoires, titre « Dr » par défaut, champs vides ramenés à null. */
    private static void verifierFiche(medecin m) {
        String nom = m.getNom() == null ? "" : m.getNom().trim();
        String prenom = m.getPrenom() == null ? "" : m.getPrenom().trim();
        if (nom.isEmpty() || prenom.isEmpty()) {
            throw new MetierException("Le nom et le prénom du médecin sont obligatoires.");
        }
        m.setNom(nom.toUpperCase(Locale.FRENCH));
        m.setPrenom(PatientService.capitaliser(prenom));
        m.setTitre(vide(m.getTitre()) == null ? "Dr" : m.getTitre().trim());
        m.setOrdmedcin(vide(m.getOrdmedcin()));
        m.setSpecialite(vide(m.getSpecialite()));
        m.setAdresse(vide(m.getAdresse()));
        m.setTelephone1(vide(m.getTelephone1()));
        m.setTelephone2(vide(m.getTelephone2()));
        m.setEmail(vide(m.getEmail()));
        m.setNom_cabinet(vide(m.getNom_cabinet()));
    }

    private static String vide(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    /**
     * Refuse de retirer le dernier administrateur actif : sans lui, plus
     * personne ne pourrait gérer les comptes depuis l'application.
     */
    static void verifierQuIlResteUnAdministrateur(UtilisateurDao dao, int idQuiPart) {
        for (User autre : dao.findAll()) {
            if (autre.getIdent() != idQuiPart && autre.getRole() == Role.ADMIN && autre.isActif()) {
                return;
            }
        }
        throw new MetierException("C'est le dernier compte administrateur actif : "
                + "créez ou activez d'abord un autre administrateur.");
    }

    private Integer medecinRattache(Role role, Integer idMedecin) {
        if (role != Role.MEDECIN) {
            return null;
        }
        if (idMedecin == null) {
            throw new MetierException("Un compte médecin doit être rattaché à une fiche médecin.");
        }
        if (daos.getMedecinDao().findById(idMedecin) == null) {
            throw new MetierException("Le médecin n° " + idMedecin + " est introuvable.");
        }
        return idMedecin;
    }

    private static String obligatoire(String valeur, String champ) {
        String v = valeur == null ? "" : valeur.trim();
        if (v.isEmpty()) {
            throw new MetierException("Le " + champ + " du membre est obligatoire.");
        }
        return v;
    }
}
