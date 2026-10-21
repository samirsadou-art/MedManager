package My_application.controller;

import My_application.entities.Rendez_Vous;
import My_application.entities.consultation;
import My_application.service.DossierService;
import My_application.service.RendezVousService;
import java.time.LocalDate;
import java.util.List;

public class AccueilController extends ControleurBase {

    private final AccueilVue vue;
    private final DossierService dossierService;
    private final RendezVousService rendezVousService;

    public AccueilController(AccueilVue vue, DossierService dossierService, RendezVousService rendezVousService) {
        this(vue, dossierService, rendezVousService, Executeur.SWING);
    }

    public AccueilController(AccueilVue vue, DossierService dossierService, RendezVousService rendezVousService,
            Executeur executeur) {
        super(vue, executeur);
        this.vue = vue;
        this.dossierService = dossierService;
        this.rendezVousService = rendezVousService;
    }

    /** Rendez-vous et consultations du jour, lus ensemble hors de l'EDT. */
    private static final class Activite {
        List<Rendez_Vous> rendezVous;
        List<consultation> consultations;
    }

    /** Relit l'activité du jour en arrière-plan : la fenêtre reste utilisable pendant la lecture. */
    public void rafraichir() {
        enArrierePlan(() -> {
            LocalDate jour = rendezVousService.aujourdhui();
            Activite a = new Activite();
            a.rendezVous = rendezVousService.duJour(jour);
            a.consultations = dossierService.consultationsDuJour(jour);
            return a;
        }, a -> {
            vue.afficherRendezVousDuJour(a.rendezVous);
            vue.afficherConsultationsDuJour(a.consultations);
        });
    }
}
