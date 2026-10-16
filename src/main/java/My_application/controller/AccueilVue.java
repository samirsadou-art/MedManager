package My_application.controller;

import My_application.entities.Rendez_Vous;
import My_application.entities.consultation;
import java.util.List;

/** Fenêtre principale : activité du jour. */
public interface AccueilVue extends Vue {

    void afficherRendezVousDuJour(List<Rendez_Vous> rendezVous);

    void afficherConsultationsDuJour(List<consultation> consultations);
}
