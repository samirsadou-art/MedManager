package My_application.controller;

import My_application.service.DossierService;
import My_application.service.RendezVousService;
import java.time.LocalDate;

public class AccueilController extends ControleurBase {

    private final AccueilVue vue;
    private final DossierService dossierService;
    private final RendezVousService rendezVousService;

    public AccueilController(AccueilVue vue, DossierService dossierService, RendezVousService rendezVousService) {
        super(vue);
        this.vue = vue;
        this.dossierService = dossierService;
        this.rendezVousService = rendezVousService;
    }

    public void rafraichir() {
        tenter(new Runnable() {
            @Override
            public void run() {
                LocalDate jour = rendezVousService.aujourdhui();
                vue.afficherRendezVousDuJour(rendezVousService.duJour(jour));
                vue.afficherConsultationsDuJour(dossierService.consultationsDuJour(jour));
            }
        });
    }
}
