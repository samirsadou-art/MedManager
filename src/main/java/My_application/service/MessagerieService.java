package My_application.service;

import My_application.dao.DAOFactory;
import My_application.dao.MessageDao;
import My_application.entities.MessageInterne;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.entities.patient;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Messagerie interne du cabinet : un fil « Tout le cabinet » et un fil
 * privé entre deux membres. Elle remplace l'ancien chat TCP : tout passe
 * par la base, chaque poste relit le dernier message toutes les 3 secondes.
 * <p>
 * Réservée aux comptes secrétaire et médecin connectés. Les messages de plus
 * de {@value #MOIS_CONSERVATION} mois sont supprimés : ils peuvent parler de
 * patients et ne doivent pas s'accumuler.
 */
public class MessagerieService {

    public static final int LONGUEUR_MAX = 2000;
    /** Messages affichés par fil. */
    public static final int LIMITE_FIL = 200;
    public static final int MOIS_CONSERVATION = 12;
    /** Un membre est « en ligne » s'il a donné signe de vie depuis moins d'une minute. */
    public static final int SECONDES_PRESENCE = 60;

    static final String NON_CONNECTE = "Connectez-vous pour utiliser la messagerie.";

    /** Un fil dans la liste de gauche : « Tout le cabinet » ou un membre. */
    public static final class Conversation {
        private final Integer idAutre;
        private final String libelle;
        private final String detail;
        private final boolean enLigne;
        private final int nonLus;

        Conversation(Integer idAutre, String libelle, String detail, boolean enLigne, int nonLus) {
            this.idAutre = idAutre;
            this.libelle = libelle;
            this.detail = detail;
            this.enLigne = enLigne;
            this.nonLus = nonLus;
        }

        /** Membre de la conversation privée, ou null pour « Tout le cabinet ». */
        public Integer getIdAutre() {
            return idAutre;
        }

        public String getLibelle() {
            return libelle;
        }

        /** Rôle du membre (ou description du fil commun). */
        public String getDetail() {
            return detail;
        }

        public boolean isEnLigne() {
            return enLigne;
        }

        public int getNonLus() {
            return nonLus;
        }

        public boolean isTousLeCabinet() {
            return idAutre == null;
        }
    }

    private final DAOFactory daos;
    private final Supplier<Integer> utilisateurConnecte;
    private final ControleAcces acces;
    private final Clock horloge;

    public MessagerieService(DAOFactory daos, Supplier<Integer> utilisateurConnecte) {
        this(daos, utilisateurConnecte, ControleAcces.AUCUN, Clock.systemDefaultZone());
    }

    public MessagerieService(DAOFactory daos, Supplier<Integer> utilisateurConnecte, ControleAcces acces,
            Clock horloge) {
        this.daos = daos;
        this.utilisateurConnecte = utilisateurConnecte;
        this.acces = acces;
        this.horloge = horloge;
    }

    private int moi() {
        acces.exiger(Role.SECRETAIRE, Role.MEDECIN);
        Integer id = utilisateurConnecte.get();
        if (id == null) {
            throw new MetierException(NON_CONNECTE);
        }
        return id;
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge).truncatedTo(ChronoUnit.SECONDS);
    }

    /** Membres avec qui échanger : comptes actifs (sauf administrateurs), sans soi-même. */
    private List<User> membres(int moi) {
        List<User> l = new ArrayList<>();
        for (User u : daos.getUtilisateurDao().findAll()) {
            if (u.getIdent() != moi && u.isActif() && u.getLogin() != null && u.getRole() != Role.ADMIN) {
                l.add(u);
            }
        }
        l.sort(Comparator.comparing((User u) -> u.getRole() == Role.MEDECIN ? 0 : 1)
                .thenComparing(u -> (u.getNom() + " " + u.getPrenom()).toUpperCase(Locale.FRENCH)));
        return l;
    }

    /** « Tout le cabinet » puis chaque membre (médecins d'abord), avec présence et non-lus. */
    public List<Conversation> conversations() {
        int moi = moi();
        Map<Integer, Integer> nonLus = daos.getMessageDao().nonLus(moi);
        Set<Integer> enLigne = daos.getPresenceDao().enLigne(SECONDES_PRESENCE);
        List<Conversation> l = new ArrayList<>();
        l.add(new Conversation(null, "Tout le cabinet", "Message à tous les membres", false,
                valeur(nonLus, MessageDao.TOUT_LE_CABINET)));
        for (User u : membres(moi)) {
            l.add(new Conversation(u.getIdent(), u.getNom() + " " + u.getPrenom(), u.getRole().getLibelle(),
                    enLigne.contains(u.getIdent()), valeur(nonLus, u.getIdent())));
        }
        return l;
    }

    private static int valeur(Map<Integer, Integer> m, int cle) {
        Integer n = m.get(cle);
        return n == null ? 0 : n;
    }

    /** Total des messages non lus, tous fils confondus. */
    public static int total(List<Conversation> conversations) {
        int n = 0;
        for (Conversation c : conversations) {
            n += c.getNonLus();
        }
        return n;
    }

    /** Derniers messages du fil (null : tout le cabinet), marqués lus. */
    public List<MessageInterne> lire(Integer autre) {
        int moi = moi();
        List<MessageInterne> l = daos.getMessageDao().conversation(moi, autre, LIMITE_FIL);
        daos.getMessageDao().marquerLus(moi, autre, maintenant());
        return l;
    }

    /**
     * Envoie un message dans le fil (null : tout le cabinet), en citant
     * éventuellement un patient.
     */
    public MessageInterne envoyer(Integer autre, String texte, patient cite) {
        int moi = moi();
        String t = texte == null ? "" : texte.trim();
        if (t.isEmpty()) {
            throw new MetierException("Le message est vide.");
        }
        if (t.length() > LONGUEUR_MAX) {
            throw new MetierException("Le message est trop long (" + LONGUEUR_MAX + " caractères au plus).");
        }
        if (autre != null) {
            boolean connu = false;
            for (User u : membres(moi)) {
                connu |= u.getIdent() == autre;
            }
            if (!connu) {
                throw new MetierException("Ce membre n'a plus de compte actif : le message n'est pas envoyé.");
            }
        }
        MessageInterne m = new MessageInterne();
        m.setIdExpediteur(moi);
        m.setIdDestinataire(autre);
        m.setTexte(t);
        m.setPatient(cite);
        m.setEnvoyeLe(maintenant());
        daos.getMessageDao().insert(m);
        return m;
    }

    /** Numéro du dernier message du cabinet : un poste ne relit les fils que s'il change. */
    public long dernierMessage() {
        return daos.getMessageDao().dernier();
    }

    /** Signe de vie de ce poste (toutes les 30 s). */
    public void signalerPresence(String poste) {
        daos.getPresenceDao().signaler(moi(), poste);
    }

    /** Fermeture de GestiMed : le membre n'apparaît plus en ligne. */
    public void quitter() {
        Integer id = utilisateurConnecte.get();
        if (id != null) {
            daos.getPresenceDao().quitter(id);
        }
    }

    /** Supprime les messages de plus de {@value #MOIS_CONSERVATION} mois ; renvoie leur nombre. */
    public int purger() {
        return daos.getMessageDao().purgerAvant(maintenant().minusMonths(MOIS_CONSERVATION));
    }
}
