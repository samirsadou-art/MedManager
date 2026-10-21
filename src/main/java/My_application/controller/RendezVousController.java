package My_application.controller;

import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.patient;
import My_application.service.MetierException;
import My_application.service.RendezVousService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Contrôleur de l'agenda ({@code calandar}, {@code DetailRendez}). */
public class RendezVousController extends ControleurBase {

    private final Vue vue;
    private final RendezVousService service;
    private final patient malade;
    private final User utilisateur;

    public RendezVousController(Vue vue, RendezVousService service, patient malade, User utilisateur) {
        super(vue);
        this.vue = vue;
        this.service = service;
        this.malade = malade;
        this.utilisateur = utilisateur;
    }

    public RendezVousService getService() {
        return service;
    }

    public patient getPatient() {
        return malade;
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
                return service.proposer(malade, utilisateur, jour, heure);
            } catch (MetierException e) {
                return null;
            }
        }, null);
    }

    /** Double clic : calcule le créneau ; la règle refusée est affichée. */
    public Rendez_Vous proposer(LocalDate jour, int heure) {
        return tenter(() -> service.proposer(malade, utilisateur, jour, heure), null);
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
        return service.peutEtreAnnule(rdv, malade);
    }

    public boolean annuler(Rendez_Vous rdv) {
        return tenter(() -> service.annuler(rdv, malade));
    }
}
