package My_application.controller;

import My_application.entities.DossierPatient;
import My_application.entities.consultation;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Page « Consultations » de la fenêtre principale (médecin) : consultations d'une date choisie. */
public interface ConsultationsVue extends Vue {

    /**
     * @param medecins nom de chaque médecin, par identifiant
     */
    void afficherConsultations(LocalDate jour, List<consultation> consultations, Map<Integer, String> medecins);

    /** Le médecin a choisi une consultation : le dossier de son patient s'ouvre. */
    void ouvrirDossier(DossierPatient dossier);
}
