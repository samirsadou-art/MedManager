package My_application.dao;

import My_application.entities.CategAnalyse;
import My_application.entities.analyse;
import java.util.List;

/** Catalogue des analyses et de leurs catégories. */
public interface AnalyseDao {

    List<analyse> findByDesignationCommencantPar(String prefixe);

    List<analyse> findByCategorie(int idCategorie);

    List<CategAnalyse> findCategories();

    analyse insert(String designation, int idCategorie);
}
