package My_application.controller;

import My_application.entities.DossierPatient;
import My_application.entities.consultation;
import java.time.LocalDate;
import java.util.List;

/** Page « Consultations » de la fenêtre principale (médecin) : consultations d'une date choisie. */
public interface ConsultationsVue extends Vue {

    void afficherConsultations(LocalDate jour, List<consultation> consultations);

    /** Le médecin a choisi une consultation : le dossier de son patient s'ouvre. */
    void ouvrirDossier(DossierPatient dossier);
}
