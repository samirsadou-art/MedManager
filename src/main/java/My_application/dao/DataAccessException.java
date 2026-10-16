package My_application.dao;

/**
 * Exception unique de la couche DAO.
 * <p>
 * Elle enveloppe les {@link java.sql.SQLException} (exceptions techniques
 * vérifiées) afin que les couches supérieures (services, contrôleurs) ne
 * dépendent pas de JDBC. Elle est non vérifiée : le service décide s'il la
 * traite, le contrôleur la transforme en message pour l'utilisateur.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
