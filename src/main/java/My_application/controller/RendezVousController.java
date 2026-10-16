package My_application.controller;

import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.patient;
import My_application.service.MetierException;
import My_application.service.RendezVousService;
import java.time.LocalDate;
import java.util.ArrayList;
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

    public List<Rendez_Vous> semaine(final LocalDate dimanche) {
        final List<Rendez_Vous> r = new ArrayList<>();
        tenter(new Runnable() {
            @Override
            public void run() {
                r.addAll(service.deLaSemaine(dimanche));
            }
        });
        return r;
    }

    /**
     * Calcule le créneau qui serait attribué, sans rien afficher en cas de
     * refus (simple clic : aperçu) ; renvoie null si aucun créneau possible.
     */
    public Rendez_Vous apercu(LocalDate jour, int heure) {
        try {
            return service.proposer(malade, utilisateur, jour, heure);
        } catch (MetierException e) {
            return null;
        } catch (RuntimeException e) {
            vue.afficherErreur("Erreur d'accès à la base de données.\n" + e.getMessage());
            return null;
        }
    }

    /** Double clic : calcule le créneau ; la règle refusée est affichée. */
    public Rendez_Vous proposer(final LocalDate jour, final int heure) {
        final Rendez_Vous[] r = new Rendez_Vous[1];
        tenter(new Runnable() {
            @Override
            public void run() {
                r[0] = service.proposer(malade, utilisateur, jour, heure);
            }
        });
        return r[0];
    }

    /** Enregistre le rendez-vous ; true si réussi. */
    public boolean confirmer(final Rendez_Vous rdv) {
        boolean ok = tenter(new Runnable() {
            @Override
            public void run() {
                service.confirmer(rdv);
            }
        });
        if (ok) {
            vue.afficherInformation("Prise du rendez-vous réussie !");
        }
        return ok;
    }

    public boolean peutAnnuler(Rendez_Vous rdv) {
        return service.peutEtreAnnule(rdv, malade);
    }

    public boolean annuler(final Rendez_Vous rdv) {
        return tenter(new Runnable() {
            @Override
            public void run() {
                service.annuler(rdv, malade);
            }
        });
    }
}
