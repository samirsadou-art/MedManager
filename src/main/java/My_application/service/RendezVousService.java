package My_application.service;

import My_application.dao.DAOFactory;
import My_application.dao.DoublonException;
import My_application.dao.RendezVousDao;
import My_application.dao.UniteDeTravail;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.patient;
import java.sql.Date;
import java.sql.Time;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Agenda des rendez-vous et ses règles métier.
 * <p>
 * Ces règles étaient dupliquées dans deux gestionnaires de clic de
 * {@code calandar} (≈ 150 lignes chacun). Elles sont maintenant ici, une seule
 * fois, et couvertes par des tests :
 * <ul>
 * <li>le cabinet reçoit de {@code heureOuverture} h à {@code heureOuverture + nbHeures} h ;</li>
 * <li>au plus {@code maxParHeure} patients par heure, en créneaux réguliers
 * (4 par heure → :00, :15, :30, :45) ; on attribue le premier créneau libre ;</li>
 * <li>un patient ne peut avoir qu'un rendez-vous dans une même heure ;</li>
 * <li>on ne prend pas de rendez-vous dans le passé ;</li>
 * <li>seul un rendez-vous à venir peut être annulé.</li>
 * </ul>
 */
public class RendezVousService {

    public static final int HEURE_OUVERTURE = 8;
    public static final int NOMBRE_HEURES = 9;
    public static final int MAX_PAR_HEURE = 4;

    static final String CRENEAU_PRIS = "Ce créneau vient d'être attribué à un autre patient. "
            + "Veuillez en choisir un autre.";

    private final DAOFactory daos;
    private final RendezVousDao rendezVousDao;
    private final Clock horloge;
    private final int heureOuverture;
    private final int nombreHeures;
    private final int maxParHeure;

    public RendezVousService(DAOFactory daos) {
        this(daos, Clock.systemDefaultZone(), HEURE_OUVERTURE, NOMBRE_HEURES, MAX_PAR_HEURE);
    }

    public RendezVousService(DAOFactory daos, Clock horloge, int heureOuverture, int nombreHeures, int maxParHeure) {
        if (maxParHeure <= 0 || 60 % maxParHeure != 0) {
            throw new IllegalArgumentException("maxParHeure doit diviser 60 : " + maxParHeure);
        }
        this.daos = daos;
        this.rendezVousDao = daos.getRendezVousDao();
        this.horloge = horloge;
        this.heureOuverture = heureOuverture;
        this.nombreHeures = nombreHeures;
        this.maxParHeure = maxParHeure;
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

    public LocalDate aujourdhui() {
        return LocalDate.now(horloge);
    }

    /** Dimanche de la semaine contenant ce jour (l'agenda va du dimanche au samedi). */
    public static LocalDate dimancheDeLaSemaine(LocalDate jour) {
        return jour.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    // ----- consultation de l'agenda -----

    public List<Rendez_Vous> duJour(LocalDate jour) {
        return rendezVousDao.findEntre(Date.valueOf(jour), Date.valueOf(jour));
    }

    public List<Rendez_Vous> deLaSemaine(LocalDate dimanche) {
        return rendezVousDao.findEntre(Date.valueOf(dimanche), Date.valueOf(dimanche.plusDays(6)));
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

    /** Variante sans accès base : {@code existants} = rendez-vous déjà pris ce jour-là. */
    public Rendez_Vous proposer(patient malade, User utilisateur, LocalDate jour, int heure,
            List<Rendez_Vous> existants) {
        if (malade == null) {
            throw new MetierException("Aucun patient sélectionné.");
        }
        if (heure < heureOuverture || heure >= heureOuverture + nombreHeures) {
            throw new MetierException("Le cabinet est fermé à " + heure + " h.");
        }
        List<Rendez_Vous> dansLHeure = dansLHeure(existants, jour, heure);
        for (Rendez_Vous r : dansLHeure) {
            if (r.getPatient().getIdentPat() == malade.getIdentPat()) {
                throw new MetierException("Vous ne pouvez pas prendre plus d'un rendez-vous par heure !");
            }
        }
        if (dansLHeure.size() >= maxParHeure) {
            throw new MetierException("Ce créneau horaire est complet.");
        }
        int pas = 60 / maxParHeure;
        boolean[] occupe = new boolean[maxParHeure];
        for (Rendez_Vous r : dansLHeure) {
            int indice = r.getHeure().toLocalTime().getMinute() / pas;
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
                return nouveauRendezVous(malade, utilisateur, debut, maintenant);
            }
        }
        throw new MetierException("Impossible de prendre un rendez-vous dans le passé.");
    }

    private Rendez_Vous nouveauRendezVous(patient malade, User utilisateur, LocalDateTime debut,
            LocalDateTime maintenant) {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(malade);
        r.setUtilsateur(utilisateur);
        r.setDate(Date.valueOf(debut.toLocalDate()));
        r.setHeure(Time.valueOf(debut.toLocalTime()));
        r.setDateFix(Date.valueOf(maintenant.toLocalDate()));
        r.setHeureFix(Time.valueOf(maintenant.toLocalTime()));
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
                LocalDate jour = rdv.getDate().toLocalDate();
                LocalTime h = rdv.getHeure().toLocalTime();
                List<Rendez_Vous> dansLHeure = dansLHeure(duJour(jour), jour, h.getHour());
                for (Rendez_Vous r : dansLHeure) {
                    if (r.getPatient().getIdentPat() == rdv.getPatient().getIdentPat()) {
                        throw new MetierException("Vous ne pouvez pas prendre plus d'un rendez-vous par heure !");
                    }
                    if (r.getHeure().toLocalTime().equals(h)) {
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
                    // l'insertion : la clé primaire (date, heure) l'a refusé.
                    throw new MetierException(CRENEAU_PRIS);
                }
                return null;
            }
        });
    }

    /** Annule un rendez-vous à venir du patient {@code demandeur}. */
    public void annuler(Rendez_Vous rdv, patient demandeur) {
        if (!peutEtreAnnule(rdv, demandeur)) {
            throw new MetierException("Seul un rendez-vous à venir du patient courant peut être annulé.");
        }
        if (!rendezVousDao.delete(rdv)) {
            throw new MetierException("Ce rendez-vous n'existe plus.");
        }
    }

    public boolean peutEtreAnnule(Rendez_Vous rdv, patient demandeur) {
        return rdv != null && demandeur != null
                && rdv.getPatient().getIdentPat() == demandeur.getIdentPat()
                && !rdv.isPassed(LocalDateTime.now(horloge));
    }

    private static List<Rendez_Vous> dansLHeure(List<Rendez_Vous> rdvs, LocalDate jour, int heure) {
        List<Rendez_Vous> r = new ArrayList<>();
        for (Rendez_Vous x : rdvs) {
            if (x.getDate().toLocalDate().equals(jour) && x.getHeure().toLocalTime().getHour() == heure) {
                r.add(x);
            }
        }
        return r;
    }
}
