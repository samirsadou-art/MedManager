package My_application.controller;

import My_application.entities.medecin;
import My_application.entities.patient;
import My_application.service.DossierService;
import My_application.service.PatientService;
import My_application.service.RendezVousService;
import My_application.service.SalleAttenteService;
import My_application.service.Session;
import java.util.Collections;
import java.util.List;

public class ListePatientController extends ControleurBase {

    private final ListePatientVue vue;
    private final PatientService patientService;
    private final DossierService dossierService;
    private final Session session;
    private final SalleAttenteService salle;
    private final RendezVousService rendezVous;

    public ListePatientController(ListePatientVue vue, PatientService patientService,
            DossierService dossierService, Session session) {
        this(vue, patientService, dossierService, session, null, null);
    }

    public ListePatientController(ListePatientVue vue, PatientService patientService,
            DossierService dossierService, Session session, SalleAttenteService salle,
            RendezVousService rendezVous) {
        super(vue);
        this.vue = vue;
        this.patientService = patientService;
        this.dossierService = dossierService;
        this.session = session;
        this.salle = salle;
        this.rendezVous = rendezVous;
    }

    public void rechercher(String debutDuNom) {
        tenter(() -> vue.afficherPatients(patientService.rechercher(debutDuNom)));
    }

    public boolean ouvrirDossier(patient p) {
        return tenter(() -> vue.afficherDossier(dossierService.ouvrirDossier(p, session.getMedecinTraitant())));
    }

    /**
     * Médecins entre lesquels choisir pour un patient sans rendez-vous : vide
     * si le choix est inutile (poste d'un médecin, ou un seul médecin au cabinet).
     */
    public List<medecin> medecinsAChoisir() {
        if (rendezVous == null || session.getIdMedecinDuPoste() != null) {
            return Collections.emptyList();
        }
        List<medecin> medecins = tenter(rendezVous::medecins, Collections.<medecin>emptyList());
        return medecins.size() > 1 ? medecins : Collections.<medecin>emptyList();
    }

    /** Patient arrivé sans rendez-vous : il entre dans la file du médecin choisi (null : choix automatique). */
    public boolean mettreEnSalle(patient p, Integer idMedecin) {
        if (tenter(() -> {
            salle.arriveeSansRendezVous(p, idMedecin);
        })) {
            vue.afficherInformation(p.getNom() + " " + p.getPrenom() + " est en salle d'attente.");
            return true;
        }
        return false;
    }
}
