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
import java.util.List;
import java.util.function.IntSupplier;

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
 * <li>le rendez-vous est pris avec le médecin du poste ({@link Session#getIdMedecin()}).</li>
 * <li>on ne prend pas de rendez-vous dans le passé ;</li>
 * <li>seul un rendez-vous à venir peut être annulé.</li>
 * </ul>
 */
public class RendezVousService {

    public static final int HEURE_OUVERTURE = 8;
    public static final int NOMBRE_HEURES = 9;
    public static final int MAX_PAR_HEURE = 4;
    /** Médecin utilisé quand aucun n'est fourni (même valeur que le médecin par défaut de la session). */
    public static final int MEDECIN_PAR_DEFAUT = 1;

    static final String CRENEAU_PRIS = "Ce créneau vient d'être attribué à un autre patient. "
            + "Veuillez en choisir un autre.";

    private final DAOFactory daos;
    private final RendezVousDao rendezVousDao;
    private final Clock horloge;
    private final int heureOuverture;
    private final int nombreHeures;
    private final int maxParHeure;
    private final JournalAcces journal;
    private final IntSupplier medecinCourant;

    public RendezVousService(DAOFactory daos) {
        this(daos, JournalAcces.AUCUN);
    }

    public RendezVousService(DAOFactory daos, JournalAcces journal) {
        this(daos, Clock.systemDefaultZone(), HEURE_OUVERTURE, NOMBRE_HEURES, MAX_PAR_HEURE, journal);
    }

    /** Production : les rendez-vous sont pris avec le médecin de la session. */
    public RendezVousService(DAOFactory daos, JournalAcces journal, IntSupplier medecinCourant) {
        this(daos, Clock.systemDefaultZone(), HEURE_OUVERTURE, NOMBRE_HEURES, MAX_PAR_HEURE, journal,
                medecinCourant);
    }

    public RendezVousService(DAOFactory daos, Clock horloge, int heureOuverture, int nombreHeures, int maxParHeure) {
        this(daos, horloge, heureOuverture, nombreHeures, maxParHeure, JournalAcces.AUCUN);
    }

    public RendezVousService(DAOFactory daos, Clock horloge, int heureOuverture, int nombreHeures, int maxParHeure,
            JournalAcces journal) {
        this(daos, horloge, heureOuverture, nombreHeures, maxParHeure, journal, () -> MEDECIN_PAR_DEFAUT);
    }

    public RendezVousService(DAOFactory daos, Clock horloge, int heureOuverture, int nombreHeures, int maxParHeure,
            JournalAcces journal, IntSupplier medecinCourant) {
        if (maxParHeure <= 0 || 60 % maxParHeure != 0) {
            throw new IllegalArgumentException("maxParHeure doit diviser 60 : " + maxParHeure);
        }
        this.daos = daos;
        this.rendezVousDao = daos.getRendezVousDao();
        this.horloge = horloge;
        this.heureOuverture = heureOuverture;
        this.nombreHeures = nombreHeures;
        this.maxParHeure = maxParHeure;
        this.journal = journal;
        this.medecinCourant = medecinCourant;
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

    /** Médecin avec lequel les nouveaux rendez-vous sont pris (celui du poste). */
    public int getIdMedecinCourant() {
        return medecinCourant.getAsInt();
    }

    /** Médecins du cabinet, pour filtrer l'agenda (triés par nom). */
    public List<medecin> medecins() {
        return daos.getMedecinDao().findAll();
    }

    public LocalDate aujourdhui() {
        return LocalDate.now(horloge);
    }

    /** Dimanche de la semaine contenant ce jour (l'agenda va du dimanche au samedi). */
    public static LocalDate dimancheDeLaSemaine(LocalDate jour) {
        return jour.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    // ----- consultation de l'agenda -----

    public List<Rendez_Vous> duJour(LocalDate jour) {
        return rendezVousDao.findEntre(jour, jour);
    }

    public List<Rendez_Vous> deLaSemaine(LocalDate dimanche) {
        return rendezVousDao.findEntre(dimanche, dimanche.plusDays(6));
    }

    // ----- prise de rendez-vous -----

    /**
     * Calcule le rendez-vous qui serait attribué au patient dans l'heure
     * {@code heure} du jour {@code jour} (premier créneau libre), sans
     * l'enregistrer. Lit l'agenda du jour en base.
     *
     * @throws MetierException si la règle métier l'interdit (message affichable)
     */
    public Rendez_Vous proposer(patient malade, User utilisateur, LocalDate jour, int heure) {
        return proposer(malade, utilisateur, jour, heure, duJour(jour));
    }

    /**
     * Comme {@link #proposer(patient, User, LocalDate, int)}, mais avec le
     * médecin choisi dans l'agenda plutôt que celui du poste (cabinet à
     * plusieurs médecins).
     */
    public Rendez_Vous proposer(patient malade, User utilisateur, LocalDate jour, int heure, int idMedecin) {
        return proposer(malade, utilisateur, jour, heure, duJour(jour), idMedecin);
    }

    /** Variante sans accès base : {@code existants} = rendez-vous déjà pris ce jour-là. */
    public Rendez_Vous proposer(patient malade, User utilisateur, LocalDate jour, int heure,
            List<Rendez_Vous> existants) {
        return proposer(malade, utilisateur, jour, heure, existants, medecinCourant.getAsInt());
    }

    private Rendez_Vous proposer(patient malade, User utilisateur, LocalDate jour, int heure,
            List<Rendez_Vous> existants, int idMedecin) {
        if (malade == null) {
            throw new MetierException("Aucun patient sélectionné.");
        }
        if (heure < heureOuverture || heure >= heureOuverture + nombreHeures) {
            throw new MetierException("Le cabinet est fermé à " + heure + " h.");
        }
        for (Rendez_Vous r : dansLHeure(existants, jour, heure)) {
            if (r.getPatient().getIdentPat() == malade.getIdentPat()) {
                throw new MetierException("Vous ne pouvez pas prendre plus d'un rendez-vous par heure !");
            }
        }
        List<Rendez_Vous> dansLHeure = duMedecin(dansLHeure(existants, jour, heure), idMedecin);
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
        LocalDateTime maintenant = LocalDateTime.now(horloge).truncatedTo(ChronoUnit.MINUTES);
        for (int i = 0; i < maxParHeure; i++) {
            if (!occupe[i]) {
                LocalDateTime debut = jour.atTime(heure, i * pas);
                if (!debut.isAfter(maintenant)) {
                    continue; // créneau déjà passé
                }
                Rendez_Vous r = nouveauRendezVous(malade, utilisateur, debut, maintenant);
                r.setIdMedecin(idMedecin);
                return r;
            }
        }
        throw new MetierException("Impossible de prendre un rendez-vous dans le passé.");
    }

    private Rendez_Vous nouveauRendezVous(patient malade, User utilisateur, LocalDateTime debut,
            LocalDateTime maintenant) {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(malade);
        r.setUtilisateur(utilisateur);
        r.setDate(debut.toLocalDate());
        r.setHeure(debut.toLocalTime());
        r.setDateFix(maintenant.toLocalDate());
        r.setHeureFix(maintenant.toLocalTime());
        return r;
    }

    /**
     * Enregistre le rendez-vous après avoir revérifié que le créneau est
     * toujours libre (un autre poste du cabinet a pu le prendre entre-temps).
     */
    public void confirmer(final Rendez_Vous rdv) {
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                LocalDate jour = rdv.getDate();
                LocalTime h = rdv.getHeure();
                if (rdv.getIdMedecin() == null) {
                    rdv.setIdMedecin(medecinCourant.getAsInt());
                }
                List<Rendez_Vous> touteLHeure = dansLHeure(duJour(jour), jour, h.getHour());
                for (Rendez_Vous r : touteLHeure) {
                    if (r.getPatient().getIdentPat() == rdv.getPatient().getIdentPat()) {
                        throw new MetierException("Vous ne pouvez pas prendre plus d'un rendez-vous par heure !");
                    }
                }
                List<Rendez_Vous> dansLHeure = duMedecin(touteLHeure, rdv.getIdMedecin());
                for (Rendez_Vous r : dansLHeure) {
                    if (r.getHeure().equals(h)) {
                        throw new MetierException(CRENEAU_PRIS);
                    }
                }
                if (dansLHeure.size() >= maxParHeure) {
                    throw new MetierException("Ce créneau horaire est complet.");
                }
                try {
                    rendezVousDao.insert(rdv);
                } catch (DoublonException e) {
                    // Un autre poste a enregistré ce créneau entre la vérification et
                    // l'insertion : la contrainte unique (idMedecin, date, heure) l'a refusé.
                    throw new MetierException(CRENEAU_PRIS);
                }
                journal.noter(JournalAcces.Action.RENDEZ_VOUS_PRIS, rdv.getPatient().getIdentPat(),
                        rdv.getDate() + " " + rdv.getHeure());
                return null;
            }
        });
    }

    /** Annule un rendez-vous à venir du patient {@code demandeur}. */
    public void annuler(final Rendez_Vous rdv, patient demandeur) {
        if (!peutEtreAnnule(rdv, demandeur)) {
            throw new MetierException("Seul un rendez-vous à venir du patient courant peut être annulé.");
        }
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                if (!rendezVousDao.delete(rdv)) {
                    throw new MetierException("Ce rendez-vous n'existe plus.");
                }
                journal.noter(JournalAcces.Action.RENDEZ_VOUS_ANNULE, rdv.getPatient().getIdentPat(),
                        rdv.getDate() + " " + rdv.getHeure());
                return null;
            }
        });
    }

    public boolean peutEtreAnnule(Rendez_Vous rdv, patient demandeur) {
        return rdv != null && demandeur != null
                && rdv.getPatient().getIdentPat() == demandeur.getIdentPat()
                && !rdv.isPassed(LocalDateTime.now(horloge));
    }

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

    private static List<Rendez_Vous> dansLHeure(List<Rendez_Vous> rdvs, LocalDate jour, int heure) {
        List<Rendez_Vous> r = new ArrayList<>();
        for (Rendez_Vous x : rdvs) {
            if (x.getDate().equals(jour) && x.getHeure().getHour() == heure) {
                r.add(x);
            }
        }
        return r;
    }
}
