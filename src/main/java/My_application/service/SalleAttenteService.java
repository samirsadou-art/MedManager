package My_application.service;

import My_application.dao.DAOFactory;
import My_application.dao.DoublonException;
import My_application.dao.RendezVousDao;
import My_application.dao.SalleAttenteDao;
import My_application.dao.UniteDeTravail;
import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.entities.patient;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Salle d'attente : une file par médecin, partagée par la secrétaire et les
 * médecins.
 * <ul>
 * <li>La secrétaire pointe l'arrivée d'un patient qui a rendez-vous
 * ({@link #arrivee}), ou fait entrer un patient sans rendez-vous avec le
 * médecin choisi ({@link #arriveeSansRendezVous}). Elle peut remonter une
 * urgence ou noter qu'un patient est reparti sans être vu.</li>
 * <li>Le médecin appelle le patient suivant ({@link #appelerSuivant}),
 * une consultation à la fois : tant qu'un patient est en consultation chez
 * lui, il ne peut pas en appeler un autre. L'enregistrement de la
 * consultation ({@link #cloreConsultation}, appelé par
 * {@code DossierService}), ou sa fin sans consultation enregistrée
 * ({@link #terminerConsultation}), avec les frais qu'il a fixés, renvoie le
 * patient en salle d'attente « à régler » : il quitte la file du médecin
 * mais reste au cabinet.</li>
 * <li>La secrétaire encaisse les frais et libère le patient, qui quitte le
 * cabinet ({@link #liberer}).</li>
 * <li>Ordre de la file : urgences, puis patients avec rendez-vous selon
 * l'heure prévue, puis patients sans rendez-vous selon l'heure d'arrivée.</li>
 * </ul>
 * Chaque changement publie un événement (table {@code evenement}) que les
 * autres postes relisent pour se mettre à jour.
 */
public class SalleAttenteService {

    /** Au-delà (minutes), l'attente est signalée. */
    public static final int ATTENTE_LONGUE = 30;
    /** Durée de conservation des événements entre postes. */
    static final int JOURS_EVENEMENTS = 7;

    public static final String EVENEMENT_SALLE = "SALLE";
    public static final String EVENEMENT_APPEL = "APPEL";

    static final String MODIFIE_AILLEURS = "La salle d'attente vient d'être modifiée sur un autre poste. "
            + "Actualisez l'affichage.";

    /** Montant maximal accepté pour des frais ou un paiement, en dinars. */
    public static final int MONTANT_MAX = 1000000;

    /**
     * Ordre de la file d'un médecin (voir la description de la classe) ; à
     * l'accueil de la secrétaire, les patients à régler viennent en tête.
     */
    public static final Comparator<Passage> ORDRE = ordre(true);
    /** Chez le médecin : le patient en consultation, la file, puis ceux qui attendent leur sortie. */
    public static final Comparator<Passage> ORDRE_MEDECIN = ordre(false);

    private static Comparator<Passage> ordre(boolean aReglerEnTete) {
        return (a, b) -> {
            if (rang(a, aReglerEnTete) != rang(b, aReglerEnTete)) {
                return Integer.compare(rang(a, aReglerEnTete), rang(b, aReglerEnTete));
            }
            return comparerDansLaFile(a, b);
        };
    }

    private static int comparerDansLaFile(Passage a, Passage b) {
        if (a.isUrgence() != b.isUrgence()) {
            return a.isUrgence() ? -1 : 1;
        }
        if (a.isSansRendezVous() != b.isSansRendezVous()) {
            return a.isSansRendezVous() ? 1 : -1;
        }
        if (!a.isSansRendezVous() && a.getHeurePrevue() != null && b.getHeurePrevue() != null
                && !a.getHeurePrevue().equals(b.getHeurePrevue())) {
            return a.getHeurePrevue().compareTo(b.getHeurePrevue());
        }
        return a.getArriveeLe().compareTo(b.getArriveeLe());
    }

    private static int rang(Passage p, boolean aReglerEnTete) {
        switch (p.getEtat()) {
            case Passage.A_REGLER:
                return aReglerEnTete ? 0 : 3;
            case Passage.EN_CONSULTATION:
                return 1;
            default:
                return 2;
        }
    }

    /** Bilan d'une journée. */
    public static final class Bilan {
        private int vus;
        private int enAttente;
        private int sansRendezVous;
        private int partis;
        private int absents;
        private int aRegler;
        private long encaisse;
        private long attenteMoyenne;

        public int getVus() {
            return vus;
        }

        public int getEnAttente() {
            return enAttente;
        }

        public int getSansRendezVous() {
            return sansRendezVous;
        }

        public int getPartis() {
            return partis;
        }

        public int getAbsents() {
            return absents;
        }

        /** Patients consultés pas encore libérés par la secrétaire. */
        public int getARegler() {
            return aRegler;
        }

        /** Total encaissé dans la journée, en dinars. */
        public long getEncaisse() {
            return encaisse;
        }

        /** Attente moyenne avant l'appel, en minutes (0 si personne n'a été appelé). */
        public long getAttenteMoyenne() {
            return attenteMoyenne;
        }
    }

    private final DAOFactory daos;
    private final SalleAttenteDao salleDao;
    private final RendezVousDao rendezVousDao;
    private final RendezVousService rendezVous;
    private final ControleAcces acces;
    private final Clock horloge;

    public SalleAttenteService(DAOFactory daos, RendezVousService rendezVous) {
        this(daos, rendezVous, ControleAcces.AUCUN, Clock.systemDefaultZone());
    }

    public SalleAttenteService(DAOFactory daos, RendezVousService rendezVous, ControleAcces acces, Clock horloge) {
        this.daos = daos;
        this.salleDao = daos.getSalleAttenteDao();
        this.rendezVousDao = daos.getRendezVousDao();
        this.rendezVous = rendezVous;
        this.acces = acces;
        this.horloge = horloge;
    }

    public LocalDate aujourdhui() {
        return LocalDate.now(horloge);
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge).truncatedTo(ChronoUnit.SECONDS);
    }

    public LocalDateTime getMaintenant() {
        return LocalDateTime.now(horloge);
    }

    // ----- lecture -----

    /** Passages du jour, tous états confondus. */
    public List<Passage> passagesDuJour() {
        return salleDao.findDuJour(aujourdhui());
    }

    /**
     * File des médecins : patients en attente ou en consultation, chez ce
     * médecin ou chez tous si null, dans l'ordre de la file.
     */
    public List<Passage> file(Integer idMedecin) {
        return file(passagesDuJour(), idMedecin, false);
    }

    /**
     * Accueil de la secrétaire : la file, plus les patients dont la
     * consultation est terminée et qui restent à régler (en tête).
     */
    public List<Passage> presents(Integer idMedecin) {
        return file(passagesDuJour(), idMedecin, true);
    }

    /**
     * Accueil du médecin : sa file, puis les patients dont il a terminé la
     * consultation et qui attendent que la secrétaire les libère.
     */
    public List<Passage> salleDuMedecin(Integer idMedecin) {
        List<Passage> l = file(passagesDuJour(), idMedecin, true);
        l.sort(ORDRE_MEDECIN);
        return l;
    }

    /** Patient en consultation chez ce médecin (null : médecin du poste), ou null s'il n'y en a pas. */
    public Passage enConsultation(Integer idMedecin) {
        return enConsultation(passagesDuJour(), rendezVous.medecinPour(idMedecin));
    }

    private static Passage enConsultation(List<Passage> passages, int idMedecin) {
        for (Passage p : passages) {
            if (p.getEtat() == Passage.EN_CONSULTATION && p.getIdMedecin() == idMedecin) {
                return p;
            }
        }
        return null;
    }

    /** Frais proposés au médecin quand il termine une consultation : les derniers qu'il a fixés. */
    public Integer fraisProposes(int idMedecin) {
        return salleDao.dernierFrais(idMedecin);
    }

    /** Même chose sur des passages déjà lus. */
    public static List<Passage> file(List<Passage> passages, Integer idMedecin, boolean avecARegler) {
        List<Passage> l = new ArrayList<>();
        for (Passage p : passages) {
            boolean garde = avecARegler ? p.isPresent() : p.isDansLaFile();
            if (garde && (idMedecin == null || idMedecin == p.getIdMedecin())) {
                l.add(p);
            }
        }
        l.sort(ORDRE);
        return l;
    }

    /** Dernier événement publié par un poste (0 s'il n'y en a aucun). */
    public long dernierEvenement() {
        return daos.getEvenementDao().dernier();
    }

    /** Supprime les événements de plus de 7 jours ; renvoie leur nombre. */
    public int purgerEvenements() {
        return daos.getEvenementDao().purgerAvant(maintenant().minusDays(JOURS_EVENEMENTS));
    }

    // ----- secrétaire -----

    /** Rendez-vous d'aujourd'hui, prévu ou noté absent par erreur : le patient peut être pointé arrivé. */
    public boolean peutEtrePointe(Rendez_Vous r) {
        return r != null && r.getIdRendezVous() != null && aujourdhui().equals(r.getDate())
                && (r.getEtat() == Rendez_Vous.ETAT_PREVU || r.getEtat() == Rendez_Vous.ETAT_ABSENT);
    }

    /** Le patient du rendez-vous est arrivé : il entre dans la file de son médecin. */
    public Passage arrivee(final Rendez_Vous r) {
        acces.exiger(Role.SECRETAIRE, Role.MEDECIN);
        if (r == null || r.getIdRendezVous() == null) {
            throw new MetierException("Aucun rendez-vous sélectionné.");
        }
        if (!r.getDate().equals(aujourdhui())) {
            throw new MetierException("Seul un rendez-vous d'aujourd'hui peut être pointé.");
        }
        final int avant = r.getEtat();
        if (avant != Rendez_Vous.ETAT_PREVU && avant != Rendez_Vous.ETAT_ABSENT) {
            throw new MetierException("Ce patient est déjà pointé.");
        }
        final Passage p = nouveauPassage(r.getPatient(), r.getIdMedecin() == null
                ? rendezVous.medecinPour(null) : r.getIdMedecin());
        p.setIdRendezVous(r.getIdRendezVous());
        p.setHeurePrevue(r.getHeure());
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                if (!rendezVousDao.changerEtat(r.getIdRendezVous(), avant, Rendez_Vous.ETAT_ARRIVE)) {
                    throw new MetierException(MODIFIE_AILLEURS);
                }
                inserer(p);
                return null;
            }
        });
        r.setEtat(Rendez_Vous.ETAT_ARRIVE);
        return p;
    }

    /** Un patient sans rendez-vous entre dans la file du médecin choisi (null : celui du poste). */
    public Passage arriveeSansRendezVous(patient malade, Integer idMedecin) {
        acces.exiger(Role.SECRETAIRE, Role.MEDECIN);
        if (malade == null) {
            throw new MetierException("Aucun patient sélectionné.");
        }
        final Passage p = nouveauPassage(malade, rendezVous.medecinPour(idMedecin));
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                inserer(p);
                return null;
            }
        });
        return p;
    }

    private Passage nouveauPassage(patient malade, int idMedecin) {
        Passage p = new Passage();
        p.setPatient(malade);
        p.setIdMedecin(idMedecin);
        p.setJour(aujourdhui());
        p.setArriveeLe(maintenant());
        p.setEtat(Passage.EN_ATTENTE);
        return p;
    }

    private void inserer(Passage p) {
        try {
            salleDao.insert(p);
        } catch (DoublonException e) {
            throw new MetierException(p.getPatient().getNom() + " " + p.getPatient().getPrenom()
                    + " est déjà dans la salle d'attente.");
        }
        daos.getEvenementDao().publier(EVENEMENT_SALLE, p.getIdPassage());
    }

    /** Remonte un patient en tête de la file (urgence), ou le remet à sa place. */
    public void urgence(Passage p, boolean urgence) {
        acces.exiger(Role.SECRETAIRE, Role.MEDECIN);
        if (p.getEtat() != Passage.EN_ATTENTE) {
            throw new MetierException("Seul un patient qui attend peut être remonté dans la file.");
        }
        boolean avant = p.isUrgence();
        p.setUrgence(urgence);
        try {
            enregistrer(p, Passage.EN_ATTENTE, EVENEMENT_SALLE);
        } catch (RuntimeException e) {
            p.setUrgence(avant);
            throw e;
        }
    }

    /** Le patient est reparti sans être vu : son rendez-vous éventuel passe à « absent ». */
    public void partiSansConsultation(final Passage p) {
        acces.exiger(Role.SECRETAIRE, Role.MEDECIN);
        if (p.getEtat() != Passage.EN_ATTENTE) {
            throw new MetierException("Seul un patient qui attend peut être noté comme parti.");
        }
        p.setEtat(Passage.PARTI);
        p.setSortieLe(maintenant());
        try {
            daos.enTransaction(new UniteDeTravail<Void>() {
                @Override
                public Void executer() {
                    enregistrer(p, Passage.EN_ATTENTE, EVENEMENT_SALLE);
                    if (p.getIdRendezVous() != null) {
                        rendezVousDao.changerEtat(p.getIdRendezVous(), Rendez_Vous.ETAT_ARRIVE,
                                Rendez_Vous.ETAT_ABSENT);
                    }
                    return null;
                }
            });
        } catch (RuntimeException e) {
            p.setEtat(Passage.EN_ATTENTE);
            p.setSortieLe(null);
            throw e;
        }
    }

    /**
     * Le patient a réglé (ou rien, si {@code montantRegle} est null) et quitte
     * le cabinet : il sort de la salle d'attente.
     */
    public void liberer(Passage p, Integer montantRegle) {
        acces.exiger(Role.SECRETAIRE, Role.MEDECIN);
        if (p.getEtat() != Passage.A_REGLER) {
            throw new MetierException("Seul un patient dont la consultation est terminée peut être libéré.");
        }
        verifierMontant(montantRegle);
        p.setEtat(Passage.TERMINE);
        p.setSortieLe(maintenant());
        p.setMontantRegle(montantRegle);
        p.setRegleLe(montantRegle == null ? null : p.getSortieLe());
        try {
            enregistrer(p, Passage.A_REGLER, EVENEMENT_SALLE);
        } catch (RuntimeException e) {
            p.setEtat(Passage.A_REGLER);
            p.setSortieLe(null);
            p.setMontantRegle(null);
            p.setRegleLe(null);
            throw e;
        }
    }

    /**
     * Lit un montant saisi en dinars : « 2000 », « 2 000 », « 2000 DA ».
     * Vide : null (non précisé).
     */
    public static Integer montant(String saisie) {
        String t = saisie == null ? "" : saisie.replaceAll("(?i)\\s|\u00a0|\u202f|da|dzd", "");
        if (t.isEmpty()) {
            return null;
        }
        if (!t.matches("\\d{1,7}")) {
            throw new MetierException("Montant invalide : saisissez un nombre entier de dinars (ex. 2000).");
        }
        Integer m = Integer.valueOf(t);
        verifierMontant(m);
        return m;
    }

    static void verifierMontant(Integer m) {
        if (m != null && (m < 0 || m > MONTANT_MAX)) {
            throw new MetierException("Le montant doit être compris entre 0 et " + MONTANT_MAX + " DA.");
        }
    }

    /** Derniers frais fixés par ce médecin, proposés pour la consultation suivante ; null sinon. */
    public static Integer dernierFrais(DAOFactory daos, int idMedecin) {
        return daos.getSalleAttenteDao().dernierFrais(idMedecin);
    }

    /**
     * Fin de journée : les rendez-vous du jour encore « prévus » dont l'heure
     * est passée sont marqués absents. Renvoie leur nombre.
     */
    public int finDeJournee() {
        acces.exiger(Role.SECRETAIRE, Role.MEDECIN);
        int n = 0;
        for (Rendez_Vous r : rendezVous.duJour(aujourdhui())) {
            if (rendezVous.peutEtreMarqueAbsent(r)) {
                rendezVous.marquerAbsent(r);
                n++;
            }
        }
        return n;
    }

    /** Bilan du jour : patients vus, absents, sans rendez-vous, attente moyenne. */
    public Bilan bilan() {
        Bilan b = new Bilan();
        long totalAttente = 0;
        int appeles = 0;
        for (Passage p : passagesDuJour()) {
            switch (p.getEtat()) {
                case Passage.TERMINE:
                    b.vus++;
                    break;
                case Passage.A_REGLER:
                    b.vus++;
                    b.aRegler++;
                    break;
                case Passage.PARTI:
                    b.partis++;
                    break;
                case Passage.EN_ATTENTE:
                    b.enAttente++;
                    break;
                default:
                    break;
            }
            if (p.isSansRendezVous()) {
                b.sansRendezVous++;
            }
            if (p.getMontantRegle() != null) {
                b.encaisse += p.getMontantRegle();
            }
            if (p.getAppeleLe() != null) {
                totalAttente += p.minutesDAttente(p.getAppeleLe());
                appeles++;
            }
        }
        for (Rendez_Vous r : rendezVous.duJour(aujourdhui())) {
            if (r.getEtat() == Rendez_Vous.ETAT_ABSENT) {
                b.absents++;
            }
        }
        b.attenteMoyenne = appeles == 0 ? 0 : Math.round((double) totalAttente / appeles);
        return b;
    }

    // ----- médecin -----

    /**
     * Le médecin appelle le premier patient de sa file (null : médecin du
     * poste), s'il n'a personne en consultation. Renvoie le passage appelé.
     */
    public Passage appelerSuivant(Integer idMedecin) {
        acces.exiger(Role.MEDECIN);
        int medecin = rendezVous.medecinPour(idMedecin);
        List<Passage> passages = passagesDuJour();
        refuserSiConsultationEnCours(passages, medecin, null);
        for (Passage p : file(passages, medecin, false)) {
            if (p.getEtat() == Passage.EN_ATTENTE) {
                appeler(p);
                return p;
            }
        }
        throw new MetierException("Aucun patient n'attend dans votre file.");
    }

    /**
     * Le médecin appelle ce patient : il passe en consultation. Refusé tant
     * qu'un autre patient est en consultation chez ce médecin.
     */
    public void appeler(final Passage p) {
        acces.exiger(Role.MEDECIN);
        if (p.getEtat() != Passage.EN_ATTENTE) {
            throw new MetierException("Ce patient n'est plus en attente.");
        }
        refuserSiConsultationEnCours(passagesDuJour(), p.getIdMedecin(), p);
        p.setEtat(Passage.EN_CONSULTATION);
        p.setAppeleLe(maintenant());
        try {
            daos.enTransaction(new UniteDeTravail<Void>() {
                @Override
                public Void executer() {
                    enregistrer(p, Passage.EN_ATTENTE, EVENEMENT_APPEL);
                    if (p.getIdRendezVous() != null) {
                        rendezVousDao.changerEtat(p.getIdRendezVous(), Rendez_Vous.ETAT_ARRIVE,
                                Rendez_Vous.ETAT_EN_CONSULTATION);
                    }
                    return null;
                }
            });
        } catch (RuntimeException e) {
            p.setEtat(Passage.EN_ATTENTE);
            p.setAppeleLe(null);
            throw e;
        }
    }

    private static void refuserSiConsultationEnCours(List<Passage> passages, int idMedecin, Passage appele) {
        Passage enCours = enConsultation(passages, idMedecin);
        if (enCours != null && (appele == null || !enCours.getIdPassage().equals(appele.getIdPassage()))) {
            throw new MetierException("La consultation de " + enCours.getPatient().getNom() + " "
                    + enCours.getPatient().getPrenom() + " est en cours : terminez-la avant d'appeler "
                    + "un autre patient.");
        }
    }

    /**
     * Le médecin termine la consultation sans en enregistrer une dans le
     * dossier : le patient retourne en salle d'attente « à régler », avec les
     * frais fixés (null : non précisés), jusqu'à ce que la secrétaire le
     * libère ({@link #liberer}). Son rendez-vous est terminé.
     */
    public void terminerConsultation(final Passage p, Integer frais) {
        acces.exiger(Role.MEDECIN);
        if (p.getEtat() != Passage.EN_CONSULTATION) {
            throw new MetierException("Ce patient n'est pas en consultation.");
        }
        verifierMontant(frais);
        p.setEtat(Passage.A_REGLER);
        p.setFrais(frais);
        try {
            daos.enTransaction(new UniteDeTravail<Void>() {
                @Override
                public Void executer() {
                    enregistrer(p, Passage.EN_CONSULTATION, EVENEMENT_SALLE);
                    if (p.getIdRendezVous() != null) {
                        rendezVousDao.changerEtat(p.getIdRendezVous(), Rendez_Vous.ETAT_EN_CONSULTATION,
                                Rendez_Vous.ETAT_TERMINE);
                    }
                    return null;
                }
            });
        } catch (RuntimeException e) {
            p.setEtat(Passage.EN_CONSULTATION);
            p.setFrais(null);
            throw e;
        }
    }

    private void enregistrer(Passage p, int etatAttendu, String evenement) {
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                if (!salleDao.mettreAJour(p, etatAttendu)) {
                    throw new MetierException(MODIFIE_AILLEURS);
                }
                daos.getEvenementDao().publier(evenement, p.getIdPassage());
                return null;
            }
        });
    }

    /** Vrai si ce patient est aujourd'hui dans la file d'un médecin (ses frais seront à régler). */
    public static boolean estDansLaFile(DAOFactory daos, int idPatient, LocalDate jour) {
        return passageDansLaFile(daos, idPatient, jour) != null;
    }

    private static Passage passageDansLaFile(DAOFactory daos, int idPatient, LocalDate jour) {
        for (Passage p : daos.getSalleAttenteDao().findDuJour(jour)) {
            if (p.getPatient().getIdentPat() == idPatient && p.isDansLaFile()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Fin de consultation, appelé dans la transaction qui enregistre la
     * consultation : le passage du jour de ce patient (appelé ou non) passe
     * « à régler » avec les frais fixés par le médecin (null : non précisés),
     * et son rendez-vous est terminé. La secrétaire le libère ensuite
     * ({@link #liberer}). Sans passage, rien n'est fait.
     *
     * @return true si un passage a été clos
     */
    public static boolean cloreConsultation(DAOFactory daos, int idPatient, LocalDateTime maintenant,
            Integer frais) {
        verifierMontant(frais);
        Passage p = passageDansLaFile(daos, idPatient, maintenant.toLocalDate());
        if (p == null) {
            return false;
        }
        int avant = p.getEtat();
        if (p.getAppeleLe() == null) {
            p.setAppeleLe(maintenant);
        }
        p.setFrais(frais);
        p.setEtat(Passage.A_REGLER);
        if (daos.getSalleAttenteDao().mettreAJour(p, avant) && p.getIdRendezVous() != null) {
            int etatRdv = avant == Passage.EN_CONSULTATION
                    ? Rendez_Vous.ETAT_EN_CONSULTATION : Rendez_Vous.ETAT_ARRIVE;
            daos.getRendezVousDao().changerEtat(p.getIdRendezVous(), etatRdv, Rendez_Vous.ETAT_TERMINE);
        }
        daos.getEvenementDao().publier(EVENEMENT_SALLE, p.getIdPassage());
        return true;
    }
}
