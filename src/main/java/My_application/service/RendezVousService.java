package My_application.service;

import My_application.dao.DAOFactory;
import My_application.dao.DoublonException;
import My_application.dao.RendezVousDao;
import My_application.dao.UniteDeTravail;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.medecin;
import My_application.entities.patient;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Agenda des rendez-vous et ses règles métier.
 * <p>
 * Ces règles étaient dupliquées dans deux gestionnaires de clic de
 * {@code calandar} (≈ 150 lignes chacun). Elles sont maintenant ici, une seule
 * fois, et couvertes par des tests :
 * <ul>
 * <li>le cabinet reçoit de {@code heureOuverture} h à {@code heureOuverture + nbHeures} h ;</li>
 * <li>au plus {@code maxParHeure} patients par heure et par médecin, en créneaux réguliers
 * (4 par heure → :00, :15, :30, :45) ; on attribue le premier créneau libre ;</li>
 * <li>un patient ne peut avoir qu'un rendez-vous dans une même heure (tous médecins confondus) ;</li>
 * <li>le rendez-vous est pris avec le médecin choisi dans l'agenda, sinon celui du poste
 * ({@link Session#getIdMedecinDuPoste()}) ; un poste sans médecin (secrétaire) doit le
 * choisir, sauf si le cabinet n'en a qu'un ;</li>
 * <li>on ne prend pas de rendez-vous dans le passé ;</li>
 * <li>seul un rendez-vous prévu et à venir peut être annulé ou déplacé ; seul un rendez-vous
 * prévu dont l'heure est passée peut être marqué absent.</li>
 * </ul>
 * Rien n'est supprimé : une annulation ou une absence change l'état du rendez-vous
 * ({@link Rendez_Vous#ETAT_ANNULE}, {@link Rendez_Vous#ETAT_ABSENT}), ce qui libère
 * le créneau et garde l'historique.
 */
public class RendezVousService {

    public static final int HEURE_OUVERTURE = 8;
    public static final int NOMBRE_HEURES = 9;
    public static final int MAX_PAR_HEURE = 4;
    /** Médecin utilisé quand aucun n'est fourni (même valeur que le médecin par défaut de la session). */
    public static final int MEDECIN_PAR_DEFAUT = 1;
    public static final int LONGUEUR_MOTIF = 80;
    /** Événement publié pour les autres postes à chaque changement de l'agenda. */
    public static final String EVENEMENT_RENDEZ_VOUS = "RENDEZ_VOUS";

    static final String CRENEAU_PRIS = "Ce créneau vient d'être attribué à un autre patient. "
            + "Veuillez en choisir un autre.";
    static final String MEDECIN_A_CHOISIR = "Choisissez d'abord le médecin du rendez-vous "
            + "dans la liste en haut de l'agenda.";
    static final String MODIFIE_AILLEURS = "Ce rendez-vous vient d'être modifié sur un autre poste. "
            + "Actualisez l'agenda.";
    /** Au-delà, la recherche demande un texte plus précis. */
    static final int PATIENTS_RECHERCHES = 50;

    private final DAOFactory daos;
    private final RendezVousDao rendezVousDao;
    private final PatientService patients;
    private final Clock horloge;
    private final int heureOuverture;
    private final int nombreHeures;
    private final int maxParHeure;
    private final JournalAcces journal;
    private final Supplier<Integer> medecinDuPoste;

    public RendezVousService(DAOFactory daos) {
        this(daos, JournalAcces.AUCUN);
    }

    public RendezVousService(DAOFactory daos, JournalAcces journal) {
        this(daos, Clock.systemDefaultZone(), HEURE_OUVERTURE, NOMBRE_HEURES, MAX_PAR_HEURE, journal);
    }

    /**
     * Production : les rendez-vous sont pris avec le médecin du poste
     * ({@code medecinDuPoste} renvoie null pour un poste sans médecin).
     */
    public RendezVousService(DAOFactory daos, JournalAcces journal, Supplier<Integer> medecinDuPoste) {
        this(daos, Clock.systemDefaultZone(), HEURE_OUVERTURE, NOMBRE_HEURES, MAX_PAR_HEURE, journal,
                medecinDuPoste);
    }

    public RendezVousService(DAOFactory daos, Clock horloge, int heureOuverture, int nombreHeures, int maxParHeure) {
        this(daos, horloge, heureOuverture, nombreHeures, maxParHeure, JournalAcces.AUCUN);
    }

    public RendezVousService(DAOFactory daos, Clock horloge, int heureOuverture, int nombreHeures, int maxParHeure,
            JournalAcces journal) {
        this(daos, horloge, heureOuverture, nombreHeures, maxParHeure, journal, () -> MEDECIN_PAR_DEFAUT);
    }

    public RendezVousService(DAOFactory daos, Clock horloge, int heureOuverture, int nombreHeures, int maxParHeure,
            JournalAcces journal, Supplier<Integer> medecinDuPoste) {
        if (maxParHeure <= 0 || 60 % maxParHeure != 0) {
            throw new IllegalArgumentException("maxParHeure doit diviser 60 : " + maxParHeure);
        }
        this.daos = daos;
        this.rendezVousDao = daos.getRendezVousDao();
        this.patients = new PatientService(daos);
        this.horloge = horloge;
        this.heureOuverture = heureOuverture;
        this.nombreHeures = nombreHeures;
        this.maxParHeure = maxParHeure;
        this.journal = journal;
        this.medecinDuPoste = medecinDuPoste;
    }

    public int getMaxParHeure() {
        return maxParHeure;
    }

    public int getHeureOuverture() {
        return heureOuverture;
    }

    public int getNombreHeures() {
        return nombreHeures;
    }

    /** Médecin du poste, avec lequel les rendez-vous sont pris par défaut ; null pour un poste sans médecin. */
    public Integer getIdMedecinCourant() {
        return medecinDuPoste.get();
    }

    /** Médecins du cabinet, pour filtrer l'agenda (triés par nom). */
    public List<medecin> medecins() {
        return daos.getMedecinDao().findAll();
    }

    public LocalDate aujourdhui() {
        return LocalDate.now(horloge);
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge).truncatedTo(ChronoUnit.MINUTES);
    }

    /** Dimanche de la semaine contenant ce jour (l'agenda va du dimanche au samedi). */
    public static LocalDate dimancheDeLaSemaine(LocalDate jour) {
        return jour.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    // ----- consultation de l'agenda -----

    /** Rendez-vous du jour, sans les annulés (les absents restent visibles). */
    public List<Rendez_Vous> duJour(LocalDate jour) {
        return sansAnnules(rendezVousDao.findEntre(jour, jour));
    }

    /** Rendez-vous de la semaine, sans les annulés. */
    public List<Rendez_Vous> deLaSemaine(LocalDate dimanche) {
        return sansAnnules(rendezVousDao.findEntre(dimanche, dimanche.plusDays(6)));
    }

    /**
     * Liste du jour à imprimer pour un médecin (ou tous si null) : les
     * rendez-vous qui occupent leur créneau, par médecin puis par heure.
     */
    public List<Rendez_Vous> listeDuJour(LocalDate jour, Integer idMedecin) {
        List<Rendez_Vous> l = new ArrayList<>();
        for (Rendez_Vous r : rendezVousDao.findEntre(jour, jour)) {
            if (r.isOccupant() && (idMedecin == null || idMedecin.equals(r.getIdMedecin()))) {
                l.add(r);
            }
        }
        l.sort((a, b) -> {
            int m = Integer.compare(a.getIdMedecin() == null ? 0 : a.getIdMedecin(),
                    b.getIdMedecin() == null ? 0 : b.getIdMedecin());
            return m != 0 ? m : a.getHeure().compareTo(b.getHeure());
        });
        return l;
    }

    /**
     * Rendez-vous (passés, à venir, annulés compris) des patients dont le nom
     * ou le téléphone commence par le texte, du plus récent au plus ancien.
     */
    public List<Rendez_Vous> rechercher(String texte) {
        if (texte == null || texte.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<patient> trouves = patients.rechercher(texte);
        if (trouves.size() > PATIENTS_RECHERCHES) {
            throw new MetierException("Trop de patients correspondent à « " + texte.trim()
                    + " ». Précisez le nom ou le numéro de téléphone.");
        }
        Set<Integer> ids = new LinkedHashSet<>();
        for (patient p : trouves) {
            ids.add(p.getIdentPat());
        }
        return rendezVousDao.findDesPatients(ids);
    }

    private static List<Rendez_Vous> sansAnnules(List<Rendez_Vous> rdvs) {
        List<Rendez_Vous> l = new ArrayList<>();
        for (Rendez_Vous r : rdvs) {
            if (r.getEtat() != Rendez_Vous.ETAT_ANNULE) {
                l.add(r);
            }
        }
        return l;
    }

    // ----- prise de rendez-vous -----

    /**
     * Médecin d'un nouveau rendez-vous : celui choisi dans l'agenda, sinon
     * celui du poste, sinon le seul médecin du cabinet.
     *
     * @throws MetierException si un poste sans médecin n'en a pas choisi
     */
    public int medecinPour(Integer choisi) {
        if (choisi != null) {
            return choisi;
        }
        Integer duPoste = medecinDuPoste.get();
        if (duPoste != null) {
            return duPoste;
        }
        List<medecin> cabinet = medecins();
        if (cabinet.size() == 1) {
            return cabinet.get(0).getIdmedecin();
        }
        throw new MetierException(MEDECIN_A_CHOISIR);
    }

    /**
     * Calcule le rendez-vous qui serait attribué au patient dans l'heure
     * {@code heure} du jour {@code jour} (premier créneau libre), sans
     * l'enregistrer. Lit l'agenda du jour en base.
     *
     * @throws MetierException si la règle métier l'interdit (message affichable)
     */
    public Rendez_Vous proposer(patient malade, User utilisateur, LocalDate jour, int heure) {
        return proposer(malade, utilisateur, jour, heure, (Integer) null);
    }

    /**
     * Comme {@link #proposer(patient, User, LocalDate, int)}, avec le médecin
     * choisi dans l'agenda (null : celui du poste).
     */
    public Rendez_Vous proposer(patient malade, User utilisateur, LocalDate jour, int heure, Integer idMedecin) {
        return proposer(malade, utilisateur, jour, heure, duJour(jour), idMedecin);
    }

    /** Variante sans accès base : {@code existants} = rendez-vous déjà pris ce jour-là. */
    public Rendez_Vous proposer(patient malade, User utilisateur, LocalDate jour, int heure,
            List<Rendez_Vous> existants) {
        return proposer(malade, utilisateur, jour, heure, existants, null);
    }

    private Rendez_Vous proposer(patient malade, User utilisateur, LocalDate jour, int heure,
            List<Rendez_Vous> existants, Integer choisi) {
        if (malade == null) {
            throw new MetierException("Aucun patient sélectionné.");
        }
        int idMedecin = medecinPour(choisi);
        LocalDateTime maintenant = maintenant();
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(malade);
        r.setUtilisateur(utilisateur);
        LocalDateTime debut = creneauLibre(malade, jour, heure, idMedecin, existants, null);
        r.setDate(debut.toLocalDate());
        r.setHeure(debut.toLocalTime());
        r.setDateFix(maintenant.toLocalDate());
        r.setHeureFix(maintenant.toLocalTime());
        r.setIdMedecin(idMedecin);
        return r;
    }

    /**
     * Premier créneau libre de l'heure chez ce médecin pour ce patient.
     * {@code ignore} : rendez-vous à ne pas compter (celui qu'on déplace).
     */
    private LocalDateTime creneauLibre(patient malade, LocalDate jour, int heure, int idMedecin,
            List<Rendez_Vous> existants, Integer ignore) {
        if (heure < heureOuverture || heure >= heureOuverture + nombreHeures) {
            throw new MetierException("Le cabinet est fermé à " + heure + " h.");
        }
        List<Rendez_Vous> touteLHeure = occupants(existants, jour, heure, ignore);
        verifierUnParHeure(touteLHeure, malade);
        List<Rendez_Vous> dansLHeure = duMedecin(touteLHeure, idMedecin);
        if (dansLHeure.size() >= maxParHeure) {
            throw new MetierException("Ce créneau horaire est complet.");
        }
        int pas = 60 / maxParHeure;
        boolean[] occupe = new boolean[maxParHeure];
        for (Rendez_Vous r : dansLHeure) {
            int indice = r.getHeure().getMinute() / pas;
            if (indice < maxParHeure) {
                occupe[indice] = true;
            }
        }
        LocalDateTime maintenant = maintenant();
        for (int i = 0; i < maxParHeure; i++) {
            if (!occupe[i]) {
                LocalDateTime debut = jour.atTime(heure, i * pas);
                if (debut.isAfter(maintenant)) {
                    return debut;
                }
            }
        }
        throw new MetierException("Impossible de prendre un rendez-vous dans le passé.");
    }

    private static void verifierUnParHeure(List<Rendez_Vous> touteLHeure, patient malade) {
        for (Rendez_Vous r : touteLHeure) {
            if (r.getPatient().getIdentPat() == malade.getIdentPat()) {
                throw new MetierException("Vous ne pouvez pas prendre plus d'un rendez-vous par heure !");
            }
        }
    }

    /**
     * Revérifie, dans la transaction, que le créneau exact est toujours libre
     * (un autre poste du cabinet a pu le prendre entre-temps).
     */
    private void verifierCreneauToujoursLibre(Rendez_Vous rdv, Integer ignore) {
        LocalTime h = rdv.getHeure();
        List<Rendez_Vous> touteLHeure = occupants(duJour(rdv.getDate()), rdv.getDate(), h.getHour(), ignore);
        verifierUnParHeure(touteLHeure, rdv.getPatient());
        List<Rendez_Vous> dansLHeure = duMedecin(touteLHeure, rdv.getIdMedecin());
        for (Rendez_Vous r : dansLHeure) {
            if (r.getHeure().equals(h)) {
                throw new MetierException(CRENEAU_PRIS);
            }
        }
        if (dansLHeure.size() >= maxParHeure) {
            throw new MetierException("Ce créneau horaire est complet.");
        }
    }

    /** Motif nettoyé : null s'il est vide. */
    static String motif(String motif) {
        String m = motif == null ? "" : motif.trim().replaceAll("\\s+", " ");
        if (m.length() > LONGUEUR_MOTIF) {
            throw new MetierException("Le motif ne doit pas dépasser " + LONGUEUR_MOTIF + " caractères.");
        }
        return m.isEmpty() ? null : m;
    }

    /** Enregistre le rendez-vous proposé (avec son motif éventuel). */
    public void confirmer(final Rendez_Vous rdv) {
        rdv.setMotif(motif(rdv.getMotif()));
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                if (rdv.getIdMedecin() == null) {
                    rdv.setIdMedecin(medecinPour(null));
                }
                verifierCreneauToujoursLibre(rdv, null);
                try {
                    rendezVousDao.insert(rdv);
                } catch (DoublonException e) {
                    // Un autre poste a enregistré ce créneau entre la vérification et
                    // l'insertion : la contrainte unique (idMedecin, date, heure) l'a refusé.
                    throw new MetierException(CRENEAU_PRIS);
                }
                journal.noter(JournalAcces.Action.RENDEZ_VOUS_PRIS, rdv.getPatient().getIdentPat(),
                        rdv.getDate() + " " + rdv.getHeure());
                daos.getEvenementDao().publier(EVENEMENT_RENDEZ_VOUS, rdv.getIdRendezVous());
                return null;
            }
        });
    }

    // ----- gestes du quotidien -----

    /** Un rendez-vous prévu et à venir peut être annulé ou déplacé. */
    public boolean peutEtreAnnule(Rendez_Vous rdv) {
        return rdv != null && rdv.getIdRendezVous() != null && rdv.getEtat() == Rendez_Vous.ETAT_PREVU
                && !rdv.isPassed(maintenant());
    }

    public boolean peutEtreDeplace(Rendez_Vous rdv) {
        return peutEtreAnnule(rdv);
    }

    /** Un rendez-vous prévu dont l'heure est passée peut être marqué absent. */
    public boolean peutEtreMarqueAbsent(Rendez_Vous rdv) {
        return rdv != null && rdv.getIdRendezVous() != null && rdv.getEtat() == Rendez_Vous.ETAT_PREVU
                && rdv.isPassed(maintenant());
    }

    /** Annule un rendez-vous à venir : il reste dans l'historique et libère son créneau. */
    public void annuler(Rendez_Vous rdv) {
        if (!peutEtreAnnule(rdv)) {
            throw new MetierException("Seul un rendez-vous prévu et à venir peut être annulé.");
        }
        changerEtat(rdv, Rendez_Vous.ETAT_ANNULE, JournalAcces.Action.RENDEZ_VOUS_ANNULE);
    }

    /** Le patient n'est pas venu : le rendez-vous passe à « absent ». */
    public void marquerAbsent(Rendez_Vous rdv) {
        if (!peutEtreMarqueAbsent(rdv)) {
            throw new MetierException("Seul un rendez-vous prévu dont l'heure est passée peut être marqué absent.");
        }
        changerEtat(rdv, Rendez_Vous.ETAT_ABSENT, JournalAcces.Action.RENDEZ_VOUS_ABSENT);
    }

    private void changerEtat(final Rendez_Vous rdv, final int etat, final JournalAcces.Action action) {
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                if (!rendezVousDao.changerEtat(rdv.getIdRendezVous(), Rendez_Vous.ETAT_PREVU, etat)) {
                    throw new MetierException(MODIFIE_AILLEURS);
                }
                journal.noter(action, rdv.getPatient().getIdentPat(), rdv.getDate() + " " + rdv.getHeure());
                daos.getEvenementDao().publier(EVENEMENT_RENDEZ_VOUS, rdv.getIdRendezVous());
                return null;
            }
        });
        rdv.setEtat(etat);
    }

    /**
     * Calcule le créneau où le rendez-vous serait déplacé : le premier libre
     * de cette heure, chez {@code idMedecin} (null : le même médecin). Le
     * rendez-vous d'origine ne compte pas comme occupé.
     */
    public Rendez_Vous proposerDeplacement(Rendez_Vous rdv, LocalDate jour, int heure, Integer idMedecin) {
        if (!peutEtreDeplace(rdv)) {
            throw new MetierException("Seul un rendez-vous prévu et à venir peut être déplacé.");
        }
        int medecin = idMedecin != null ? idMedecin : medecinPour(rdv.getIdMedecin());
        LocalDateTime debut = creneauLibre(rdv.getPatient(), jour, heure, medecin, duJour(jour),
                rdv.getIdRendezVous());
        Rendez_Vous n = new Rendez_Vous();
        n.setIdRendezVous(rdv.getIdRendezVous());
        n.setPatient(rdv.getPatient());
        n.setUtilisateur(rdv.getUtilisateur());
        n.setMotif(rdv.getMotif());
        n.setDateFix(rdv.getDateFix());
        n.setHeureFix(rdv.getHeureFix());
        n.setEtat(rdv.getEtat());
        n.setIdMedecin(medecin);
        n.setDate(debut.toLocalDate());
        n.setHeure(debut.toLocalTime());
        return n;
    }

    /** Enregistre le déplacement calculé par {@link #proposerDeplacement}. */
    public void deplacer(final Rendez_Vous ancien, final Rendez_Vous nouveau) {
        if (!peutEtreDeplace(ancien) || !ancien.getIdRendezVous().equals(nouveau.getIdRendezVous())) {
            throw new MetierException("Seul un rendez-vous prévu et à venir peut être déplacé.");
        }
        if (!nouveau.getDebut().isAfter(maintenant())) {
            throw new MetierException("Impossible de déplacer un rendez-vous dans le passé.");
        }
        final String detail = ancien.getDate() + " " + ancien.getHeure() + " -> " + nouveau.getDate() + " "
                + nouveau.getHeure();
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                verifierCreneauToujoursLibre(nouveau, ancien.getIdRendezVous());
                boolean fait;
                try {
                    fait = rendezVousDao.deplacer(nouveau.getIdRendezVous(), nouveau.getDate(), nouveau.getHeure(),
                            nouveau.getIdMedecin());
                } catch (DoublonException e) {
                    throw new MetierException(CRENEAU_PRIS);
                }
                if (!fait) {
                    throw new MetierException(MODIFIE_AILLEURS);
                }
                journal.noter(JournalAcces.Action.RENDEZ_VOUS_DEPLACE, ancien.getPatient().getIdentPat(), detail);
                daos.getEvenementDao().publier(EVENEMENT_RENDEZ_VOUS, nouveau.getIdRendezVous());
                return null;
            }
        });
    }

    // ----- outils -----

    /**
     * Rendez-vous de ce médecin. Un rendez-vous sans médecin (pris avant
     * la migration V9) compte pour tous : on ne risque pas de double réservation.
     */
    private static List<Rendez_Vous> duMedecin(List<Rendez_Vous> rdvs, int idMedecin) {
        List<Rendez_Vous> r = new ArrayList<>();
        for (Rendez_Vous x : rdvs) {
            if (x.getIdMedecin() == null || x.getIdMedecin() == idMedecin) {
                r.add(x);
            }
        }
        return r;
    }

    /** Rendez-vous de cette heure qui occupent leur créneau, sauf {@code ignore}. */
    private static List<Rendez_Vous> occupants(List<Rendez_Vous> rdvs, LocalDate jour, int heure, Integer ignore) {
        List<Rendez_Vous> r = new ArrayList<>();
        for (Rendez_Vous x : rdvs) {
            if (x.isOccupant() && x.getDate().equals(jour) && x.getHeure().getHour() == heure
                    && (ignore == null || !ignore.equals(x.getIdRendezVous()))) {
                r.add(x);
            }
        }
        return r;
    }
}
