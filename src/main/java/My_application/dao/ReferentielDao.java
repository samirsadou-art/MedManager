package My_application.dao;

import java.util.List;

/**
 * Référentiel « id + désignation » (symptômes, pathologies) avec recherche
 * par préfixe pour l'auto-complétion des listes déroulantes.
 */
public interface ReferentielDao<T> {

    List<T> findByDesignationCommencantPar(String prefixe);

    /** Recherche exacte (insensible à la casse selon la collation MySQL). */
    T findByDesignation(String designation);

    T insert(String designation);
}
