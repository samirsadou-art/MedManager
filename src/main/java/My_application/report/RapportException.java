package My_application.report;

/**
 * Échec technique de production d'un document (disque, visionneuse PDF,
 * génération). Contrairement à une {@code MetierException}, la cause est
 * conservée pour le journal ; le message, lui, est destiné à l'utilisateur
 * (il indique au besoin où se trouve le fichier) et ne contient pas de détail
 * technique.
 */
public class RapportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RapportException(String message, Throwable cause) {
        super(message, cause);
    }
}
