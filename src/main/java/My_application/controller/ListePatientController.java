package My_application.controller;

import My_application.entities.DossierPatient;
import My_application.entities.patient;
import My_application.service.DossierService;
import My_application.service.PatientService;
import My_application.service.Session;

public class ListePatientController extends ControleurBase {

    private final ListePatientVue vue;
    private final PatientService patientService;
    private final DossierService dossierService;
    private final Session session;

    public ListePatientController(ListePatientVue vue, PatientService patientService,
            DossierService dossierService, Session session) {
        super(vue);
        this.vue = vue;
        this.patientService = patientService;
        this.dossierService = dossierService;
        this.session = session;
    }

    public void rechercher(final String debutDuNom) {
        tenter(new Runnable() {
            @Override
            public void run() {
                vue.afficherPatients(patientService.rechercher(debutDuNom));
            }
        });
    }

    public boolean ouvrirDossier(final patient p) {
        return tenter(new Runnable() {
            @Override
            public void run() {
                DossierPatient dossier = dossierService.ouvrirDossier(p, session.getMedecinTraitant());
                vue.afficherDossier(dossier);
            }
        });
    }
}
