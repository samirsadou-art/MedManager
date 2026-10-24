package My_application.dao;

/**
 * La base a refusé une écriture qui aurait créé un doublon (clé primaire ou
 * index unique déjà pris, code MySQL 1062).
 * <p>
 * Distinguer ce cas permet au service de le traduire en message métier
 * (« ce créneau vient d'être attribué ») au lieu d'afficher une erreur SQL.
 */
public class DoublonException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    public DoublonException(String message, Throwable cause) {
        super(message, cause);
    }
}
