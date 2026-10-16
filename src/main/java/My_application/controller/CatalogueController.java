package My_application.controller;

import My_application.entities.CategAnalyse;
import My_application.entities.analyse;
import My_application.entities.medicament;
import My_application.service.CatalogueService;
import java.util.ArrayList;
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

    public List<medicament> rechercherMedicaments(final String prefixe) {
        final List<medicament> r = new ArrayList<>();
        tenter(new Runnable() {
            @Override
            public void run() {
                r.addAll(catalogue.medicaments(prefixe));
            }
        });
        return r;
    }

    public List<String> formesMedicament() {
        final List<String> r = new ArrayList<>();
        tenter(new Runnable() {
            @Override
            public void run() {
                r.addAll(catalogue.formesMedicament());
            }
        });
        return r;
    }

    public medicament creerMedicament(final String nom, final String forme, final String dosage) {
        final medicament[] m = new medicament[1];
        if (tenter(new Runnable() {
            @Override
            public void run() {
                m[0] = catalogue.creerMedicament(nom, forme, dosage);
            }
        })) {
            vue.afficherInformation("Médicament « " + m[0].getDesignmed() + " » ajouté.");
        }
        return m[0];
    }

    public List<analyse> rechercherAnalyses(final String prefixe) {
        final List<analyse> r = new ArrayList<>();
        tenter(new Runnable() {
            @Override
            public void run() {
                r.addAll(catalogue.analyses(prefixe));
            }
        });
        return r;
    }

    public List<CategAnalyse> categoriesAnalyse() {
        final List<CategAnalyse> r = new ArrayList<>();
        tenter(new Runnable() {
            @Override
            public void run() {
                r.addAll(catalogue.categoriesAnalyse());
            }
        });
        return r;
    }

    public List<analyse> analysesDeCategorie(final CategAnalyse categorie) {
        final List<analyse> r = new ArrayList<>();
        tenter(new Runnable() {
            @Override
            public void run() {
                r.addAll(catalogue.analysesDeCategorie(categorie));
            }
        });
        return r;
    }

    public analyse creerAnalyse(final String designation, final CategAnalyse categorie) {
        final analyse[] a = new analyse[1];
        if (tenter(new Runnable() {
            @Override
            public void run() {
                a[0] = catalogue.creerAnalyse(designation, categorie);
            }
        })) {
            vue.afficherInformation("Analyse « " + a[0].getDesign() + " » ajoutée.");
        }
        return a[0];
    }
}
