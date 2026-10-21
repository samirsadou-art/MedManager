package My_application.controller;

import My_application.entities.DossierPatient;
import My_application.entities.consultation;
import My_application.entities.pathologie;
import My_application.entities.symptome;
import My_application.report.RapportService;
import My_application.service.CatalogueService;
import My_application.service.ConstantesVitales;
import My_application.service.DossierService;
import java.util.Collections;
import java.util.List;

/**
 * Contrôleur du dossier patient : fait le lien entre la fenêtre
 * {@code DossierPatientForm} et les services (catalogue, dossier, rapports).
 * La fenêtre ne contient plus aucune requête SQL.
 */
public class DossierPatientController extends ControleurBase {

    private final Vue vue;
    private final DossierService dossierService;
    private final CatalogueService catalogueService;
    private final RapportService rapportService;
    private final DossierPatient dossier;

    public DossierPatientController(Vue vue, DossierPatient dossier, DossierService dossierService,
            CatalogueService catalogueService, RapportService rapportService) {
        super(vue);
        this.vue = vue;
        this.dossier = dossier;
        this.dossierService = dossierService;
        this.catalogueService = catalogueService;
        this.rapportService = rapportService;
    }

    public DossierPatient getDossier() {
        return dossier;
    }

    /** Démarre une nouvelle consultation ; null si impossible. */
    public consultation nouvelleConsultation() {
        return tenter(() -> dossierService.nouvelleConsultation(dossier), null);
    }

    // ----- auto-complétion -----

    public List<symptome> suggestionsSymptomes(String prefixe) {
        return tenter(() -> catalogueService.symptomes(prefixe), Collections.<symptome>emptyList());
    }

    public List<pathologie> suggestionsPathologies(String prefixe) {
        return tenter(() -> catalogueService.pathologies(prefixe), Collections.<pathologie>emptyList());
    }

    // ----- symptômes / pathologies de la consultation -----

    /**
     * Ajoute à la consultation le symptôme choisi dans la liste, ou, si
     * l'utilisateur a tapé un libellé libre, le symptôme correspondant
     * (créé dans le référentiel s'il n'existe pas).
     */
    public boolean ajouterSymptome(consultation c, symptome choisi, String saisie) {
        return tenter(() -> c.addSymptome(choisi != null ? choisi : catalogueService.obtenirOuCreerSymptome(saisie)));
    }

    public boolean ajouterPathologie(consultation c, pathologie choisie, String saisie) {
        return tenter(() -> c.addPathologie(
                choisie != null ? choisie : catalogueService.obtenirOuCreerPathologie(saisie)));
    }

    // ----- constantes vitales -----

    /** Champs de constantes vitales saisis dans le dossier. */
    public enum Constante {
        TEMPERATURE, PULSATION, PRESSION, RESPIRATION, POIDS
    }

    /**
     * Valide la saisie d'une constante et la recopie dans la consultation.
     * Renvoie false (et affiche la règle non respectée) si la valeur est invalide.
     */
    public boolean saisirConstante(consultation c, Constante champ, String saisie) {
        return tenter(() -> {
            switch (champ) {
                case TEMPERATURE:
                    c.setTemperature(ConstantesVitales.temperature(saisie));
                    break;
                case PULSATION:
                    c.setPul_Cardiaque(ConstantesVitales.pulsation(saisie));
                    break;
                case PRESSION:
                    c.setPressionA(ConstantesVitales.pression(saisie));
                    break;
                case RESPIRATION:
                    c.setTauxRespir(ConstantesVitales.respiration(saisie));
                    break;
                case POIDS:
                    c.setPoids(ConstantesVitales.poids(saisie));
                    break;
                default:
                    throw new IllegalArgumentException(String.valueOf(champ));
            }
        });
    }

    // ----- enregistrement -----

    public boolean aDesModifications() {
        return dossierService.aDesModifications(dossier);
    }

    /** Enregistre le dossier ; renvoie true si tout a été écrit. */
    public boolean enregistrer() {
        return tenter(() -> dossierService.enregistrer(dossier));
    }

    // ----- documents -----

    /** Aperçu (imprimer = false) ou impression de l'ordonnance au format PDF. */
    public void ordonnance(consultation c, boolean imprimer) {
        signalerSiNonImprime(tenter(() -> rapportService.ordonnance(c, imprimer), null));
    }

    /** Aperçu (imprimer = false) ou impression de la demande d'analyses au format PDF. */
    public void analyses(consultation c, boolean imprimer) {
        signalerSiNonImprime(tenter(() -> rapportService.analyses(c, imprimer), null));
    }

    /** {@code fait} vaut null si la génération a échoué (erreur déjà affichée). */
    private void signalerSiNonImprime(Boolean fait) {
        if (Boolean.FALSE.equals(fait)) {
            vue.afficherInformation(IMPRESSION_INDISPONIBLE);
        }
    }

    static final String IMPRESSION_INDISPONIBLE = "L'impression directe n'est pas disponible sur ce poste : "
            + "le document PDF a été ouvert, imprimez-le depuis la visionneuse (Ctrl+P).";
}
