package My_application.controller;

import My_application.entities.DossierPatient;
import My_application.entities.patient;
import java.util.List;

public interface ListePatientVue extends Vue {

    void afficherPatients(List<patient> patients);

    /** Demande à la vue d'afficher le dossier chargé. */
    void afficherDossier(DossierPatient dossier);
}
