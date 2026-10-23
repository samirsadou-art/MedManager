package My_application.controller;

import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.patient;
import My_application.service.DossierService;
import My_application.service.MetierException;
import My_application.service.PatientService;
import My_application.service.RendezVousService;
import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * Consultations d'une date choisie (menu « Consultations » du médecin) : la
 * lecture se fait en arrière-plan, puis la page est remplie. Un double-clic
 * ouvre le dossier complet du patient.
 */
public class ConsultationsController extends ControleurBase {

    private final ConsultationsVue vue;
    private final DossierService dossierService;
    private final PatientService patientService;
    private final RendezVousService rendezVousService;
    private final Supplier<medecin> medecinTraitant;

    /** Date affichée (null avant la première lecture). */
    private LocalDate jour;

    public ConsultationsController(ConsultationsVue vue, DossierService dossierService,
            PatientService patientService, RendezVousService rendezVousService, Supplier<medecin> medecinTraitant,
            Executeur executeur) {
        super(vue, executeur);
        this.vue = vue;
        this.dossierService = dossierService;
        this.patientService = patientService;
        this.rendezVousService = rendezVousService;
        this.medecinTraitant = medecinTraitant;
    }

    /** Affiche les consultations de ce jour. */
    public void afficher(LocalDate date) {
        if (date == null) {
            return;
        }
        jour = date;
        enArrierePlan(() -> dossierService.consultationsDuJour(date), consultations -> {
            // Une lecture plus ancienne qui arrive après le choix d'une autre date est ignorée.
            if (date.equals(jour)) {
                vue.afficherConsultations(date, consultations);
            }
        });
    }

    /** Relit la date affichée (aujourd'hui avant tout choix). */
    public void rafraichir() {
        afficher(jour == null ? rendezVousService.aujourdhui() : jour);
    }

    public LocalDate getJour() {
        return jour;
    }

    /** Ouvre le dossier du patient de cette consultation (fiche relue en entier). */
    public void ouvrirDossier(consultation c) {
        if (c == null || c.getPatient() == null) {
            return;
        }
        tenter(() -> {
            patient p = patientService.trouver(c.getPatient().getIdentPat());
            if (p == null) {
                throw new MetierException("Le patient de cette consultation est introuvable.");
            }
            vue.ouvrirDossier(dossierService.ouvrirDossier(p, medecinTraitant.get()));
        });
    }
}
