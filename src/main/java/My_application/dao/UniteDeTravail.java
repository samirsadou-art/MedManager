package My_application.dao;

/** Traitement à exécuter de façon atomique (voir {@link DAOFactory#enTransaction}). */
public interface UniteDeTravail<T> {

    T executer();
}
