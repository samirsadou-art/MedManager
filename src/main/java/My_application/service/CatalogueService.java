package My_application.service;

import java.util.Locale;
import My_application.dao.AnalyseDao;
import My_application.dao.DAOFactory;
import My_application.dao.MedicamentDao;
import My_application.dao.PathologieDao;
import My_application.dao.SymptomeDao;
import My_application.entities.CategAnalyse;
import My_application.entities.analyse;
import My_application.entities.medicament;
import My_application.entities.pathologie;
import My_application.entities.symptome;
import java.util.Collections;
import java.util.List;

/**
 * Référentiels médicaux : symptômes, pathologies, médicaments, analyses.
 * Sert l'auto-complétion des formulaires et la création de nouvelles entrées.
 */
public class CatalogueService {

    private final SymptomeDao symptomeDao;
    private final PathologieDao pathologieDao;
    private final MedicamentDao medicamentDao;
    private final AnalyseDao analyseDao;

    public CatalogueService(DAOFactory daos) {
        this.symptomeDao = daos.getSymptomeDao();
        this.pathologieDao = daos.getPathologieDao();
        this.medicamentDao = daos.getMedicamentDao();
        this.analyseDao = daos.getAnalyseDao();
    }

    // ----- symptômes / pathologies -----

    public List<symptome> symptomes(String prefixe) {
        if (estVide(prefixe)) {
            return Collections.emptyList();
        }
        return symptomeDao.findByDesignationCommencantPar(prefixe.trim());
    }

    /**
     * Renvoie le symptôme portant cette désignation, en le créant s'il
     * n'existe pas encore (l'ancien code créait un doublon à chaque saisie).
     */
    public symptome obtenirOuCreerSymptome(String designation) {
        String d = normaliser(designation, "symptôme");
        symptome existant = symptomeDao.findByDesignation(d);
        return existant != null ? existant : symptomeDao.insert(d);
    }

    public List<pathologie> pathologies(String prefixe) {
        if (estVide(prefixe)) {
            return Collections.emptyList();
        }
        return pathologieDao.findByDesignationCommencantPar(prefixe.trim());
    }

    public pathologie obtenirOuCreerPathologie(String designation) {
        String d = normaliser(designation, "pathologie");
        pathologie existante = pathologieDao.findByDesignation(d);
        return existante != null ? existante : pathologieDao.insert(d);
    }

    // ----- médicaments -----

    public List<medicament> medicaments(String prefixe) {
        if (estVide(prefixe)) {
            return Collections.emptyList();
        }
        return medicamentDao.findByNomCommencantPar(prefixe.trim());
    }

    public List<String> formesMedicament() {
        return medicamentDao.findFormes();
    }

    /**
     * Ajoute un médicament au catalogue. La désignation enregistrée est
     * « NOM FORME DOSAGE », comme dans la version d'origine.
     */
    public medicament creerMedicament(String nom, String forme, String dosage) {
        String n = normaliser(nom, "médicament");
        StringBuilder designation = new StringBuilder(n.toUpperCase(Locale.FRENCH));
        if (!estVide(forme)) {
            designation.append(' ').append(forme.trim());
        }
        if (!estVide(dosage)) {
            designation.append(' ').append(dosage.trim());
        }
        String d = designation.toString();
        if (medicamentDao.findByNom(d) != null) {
            throw new MetierException("Le médicament « " + d + " » existe déjà.");
        }
        return medicamentDao.insert(d, estVide(forme) ? null : forme.trim());
    }

    // ----- analyses -----

    public List<analyse> analyses(String prefixe) {
        if (estVide(prefixe)) {
            return Collections.emptyList();
        }
        return analyseDao.findByDesignationCommencantPar(prefixe.trim());
    }

    public List<analyse> analysesDeCategorie(CategAnalyse categorie) {
        if (categorie == null) {
            throw new MetierException("Choisissez une catégorie d'analyses.");
        }
        return analyseDao.findByCategorie(categorie.getId());
    }

    public List<CategAnalyse> categoriesAnalyse() {
        return analyseDao.findCategories();
    }

    public analyse creerAnalyse(String designation, CategAnalyse categorie) {
        String d = normaliser(designation, "analyse").toUpperCase(Locale.FRENCH);
        if (categorie == null) {
            throw new MetierException("Choisissez la catégorie de l'analyse.");
        }
        for (analyse a : analyseDao.findByDesignationCommencantPar(d)) {
            if (a.getDesign().equalsIgnoreCase(d)) {
                throw new MetierException("L'analyse « " + d + " » existe déjà.");
            }
        }
        return analyseDao.insert(d, categorie.getId());
    }

    // ----- utilitaires -----

    private static boolean estVide(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String normaliser(String s, String quoi) {
        if (estVide(s)) {
            throw new MetierException("Saisie vide : " + quoi + ".");
        }
        return s.trim().replaceAll("\\s+", " ");
    }
}
