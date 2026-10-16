package My_application.dao;

import java.util.List;

/**
 * Contrat CRUD commun à tous les DAO (patron DAO).
 *
 * @param <T>  type de l'entité
 * @param <ID> type de son identifiant
 */
public interface GenericDao<T, ID> {

    /** Insère l'entité et lui affecte l'identifiant généré ; renvoie l'entité. */
    T insert(T entite);

    void update(T entite);

    void delete(ID id);

    /** Renvoie l'entité ou {@code null} si elle n'existe pas. */
    T findById(ID id);

    List<T> findAll();
}
