package My_application.service;

/**
 * Violation d'une règle métier (champ obligatoire, créneau complet...).
 * Le message est destiné à être affiché tel quel à l'utilisateur.
 */
public class MetierException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MetierException(String message) {
        super(message);
    }
}
