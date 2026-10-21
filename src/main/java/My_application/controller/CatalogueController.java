package My_application.controller;

import My_application.entities.CategAnalyse;
import My_application.entities.analyse;
import My_application.entities.medicament;
import My_application.service.CatalogueService;
import java.util.Collections;
import java.util.List;

/**
 * Contrôleur des formulaires Ordonnance, Analyses, Nouveau médicament et
 * Nouvelle analyse : recherche dans les référentiels et ajout d'entrées.
 */
public class CatalogueController extends ControleurBase {

    private final Vue vue;
    private final CatalogueService catalogue;

    public CatalogueController(Vue vue, CatalogueService catalogue) {
        super(vue);
        this.vue = vue;
        this.catalogue = catalogue;
    }

    public List<medicament> rechercherMedicaments(String prefixe) {
        return tenter(() -> catalogue.medicaments(prefixe), Collections.<medicament>emptyList());
    }

    public List<String> formesMedicament() {
        return tenter(catalogue::formesMedicament, Collections.<String>emptyList());
    }

    public medicament creerMedicament(String nom, String forme, String dosage) {
        medicament m = tenter(() -> catalogue.creerMedicament(nom, forme, dosage), null);
        if (m != null) {
            vue.afficherInformation("Médicament « " + m.getDesignmed() + " » ajouté.");
        }
        return m;
    }

    public List<analyse> rechercherAnalyses(String prefixe) {
        return tenter(() -> catalogue.analyses(prefixe), Collections.<analyse>emptyList());
    }

    public List<CategAnalyse> categoriesAnalyse() {
        return tenter(catalogue::categoriesAnalyse, Collections.<CategAnalyse>emptyList());
    }

    public List<analyse> analysesDeCategorie(CategAnalyse categorie) {
        return tenter(() -> catalogue.analysesDeCategorie(categorie), Collections.<analyse>emptyList());
    }

    public analyse creerAnalyse(String designation, CategAnalyse categorie) {
        analyse a = tenter(() -> catalogue.creerAnalyse(designation, categorie), null);
        if (a != null) {
            vue.afficherInformation("Analyse « " + a.getDesign() + " » ajoutée.");
        }
        return a;
    }
}
