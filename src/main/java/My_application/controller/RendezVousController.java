package My_application.controller;

import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.medecin;
import My_application.entities.patient;
import My_application.report.RapportService;
import My_application.service.MetierException;
import My_application.service.RendezVousService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Contrôleur de l'agenda ({@code calandar}, {@code DetailRendez}). */
public class RendezVousController extends ControleurBase {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
    static final String IMPRESSION_INDISPONIBLE = "L'impression directe n'est pas disponible sur ce poste : "
            + "le document PDF a été ouvert, imprimez-le depuis la visionneuse (Ctrl+P).";

    private final Vue vue;
    private final RendezVousService service;
    private final RapportService rapports;
    private final patient malade;
    private final User utilisateur;
    /** Médecin choisi dans l'agenda ; null = médecin du poste. */
    private Integer medecin;

    public RendezVousController(Vue vue, RendezVousService service, patient malade, User utilisateur) {
        this(vue, service, null, malade, utilisateur);
    }

    public RendezVousController(Vue vue, RendezVousService service, RapportService rapports, patient malade,
            User utilisateur) {
        super(vue);
        this.vue = vue;
        this.service = service;
        this.rapports = rapports;
        this.malade = malade;
        this.utilisateur = utilisateur;
    }

    public RendezVousService getService() {
        return service;
    }

    public patient getPatient() {
        return malade;
    }

    /**
     * Médecin avec lequel les prochains rendez-vous seront proposés ; null
     * pour revenir au médecin du poste.
     */
    public void setMedecin(Integer idMedecin) {
        this.medecin = idMedecin;
    }

    /**
     * Médecin des prochains rendez-vous : celui choisi, sinon celui du poste ;
     * null pour un poste sans médecin tant qu'aucun n'est choisi.
     */
    public Integer getIdMedecinReservation() {
        return medecin != null ? medecin : service.getIdMedecinCourant();
    }

    /** Médecins du cabinet ; liste vide si la base est indisponible. */
    public List<medecin> medecins() {
        return tenter(service::medecins, Collections.<medecin>emptyList());
    }

    public List<Rendez_Vous> semaine(LocalDate dimanche) {
        return tenter(() -> service.deLaSemaine(dimanche), Collections.<Rendez_Vous>emptyList());
    }

    /**
     * Calcule le créneau qui serait attribué, sans rien afficher en cas de
     * refus (simple clic : aperçu) ; renvoie null si aucun créneau possible.
     */
    public Rendez_Vous apercu(LocalDate jour, int heure) {
        // Une panne de base passe par tenter (journal + message clair) ;
        // seul le refus métier reste silencieux.
        return tenter(() -> {
            try {
                return service.proposer(malade, utilisateur, jour, heure, medecin);
            } catch (MetierException e) {
                return null;
            }
        }, null);
    }

    /** Double clic : calcule le créneau ; la règle refusée est affichée. */
    public Rendez_Vous proposer(LocalDate jour, int heure) {
        return tenter(() -> service.proposer(malade, utilisateur, jour, heure, medecin), null);
    }

    /** Enregistre le rendez-vous ; true si réussi. */
    public boolean confirmer(Rendez_Vous rdv) {
        boolean ok = tenter(() -> service.confirmer(rdv));
        if (ok) {
            vue.afficherInformation("Prise du rendez-vous réussie !");
        }
        return ok;
    }

    public boolean peutAnnuler(Rendez_Vous rdv) {
        return service.peutEtreAnnule(rdv);
    }

    public boolean annuler(Rendez_Vous rdv) {
        return tenter(() -> service.annuler(rdv));
    }

    public boolean peutMarquerAbsent(Rendez_Vous rdv) {
        return service.peutEtreMarqueAbsent(rdv);
    }

    public boolean marquerAbsent(Rendez_Vous rdv) {
        return tenter(() -> service.marquerAbsent(rdv));
    }

    public boolean peutDeplacer(Rendez_Vous rdv) {
        return service.peutEtreDeplace(rdv);
    }

    /**
     * Nouveau créneau du rendez-vous dans cette heure, avec le médecin choisi
     * dans l'agenda (sinon le même) ; null si c'est impossible (raison affichée).
     */
    public Rendez_Vous proposerDeplacement(Rendez_Vous rdv, LocalDate jour, int heure) {
        return tenter(() -> service.proposerDeplacement(rdv, jour, heure, medecin), null);
    }

    public boolean deplacer(Rendez_Vous ancien, Rendez_Vous nouveau) {
        boolean ok = tenter(() -> service.deplacer(ancien, nouveau));
        if (ok) {
            vue.afficherInformation("Rendez-vous déplacé au " + JOUR.format(nouveau.getDate()) + " à "
                    + nouveau.getHeureString() + ".");
        }
        return ok;
    }

    /** Rendez-vous des patients dont le nom ou le téléphone commence par le texte. */
    public List<Rendez_Vous> rechercher(String texte) {
        return tenter(() -> service.rechercher(texte), Collections.<Rendez_Vous>emptyList());
    }

    /** Ouvre ou imprime la liste du jour (médecin choisi dans l'agenda, ou tous). */
    public void listeDuJour(LocalDate jour, boolean imprimer) {
        if (rapports == null) {
            return;
        }
        Boolean fait = tenter(() -> {
            Map<Integer, medecin> parId = new LinkedHashMap<>();
            for (medecin m : service.medecins()) {
                parId.put(m.getIdmedecin(), m);
            }
            return rapports.listeRendezVous(jour, service.listeDuJour(jour, medecin), parId, imprimer);
        }, null);
        if (Boolean.FALSE.equals(fait)) {
            vue.afficherInformation(IMPRESSION_INDISPONIBLE);
        }
    }
}
