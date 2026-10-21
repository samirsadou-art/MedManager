package My_application.controller;

import My_application.dao.DataAccessException;
import My_application.service.MetierException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Traitement commun des erreurs : une règle métier non respectée est affichée
 * telle quelle ; une erreur technique est journalisée (avec la requête SQL et
 * la trace, voir {@code Journal}) et seul un message clair est affiché :
 * l'écran ne montre jamais le SQL ni les noms de tables.
 */
abstract class ControleurBase {

    private static final Logger LOG = Logger.getLogger("GestiMed");

    private final Vue vue;

    ControleurBase(Vue vue) {
        this.vue = vue;
    }

    /** Exécute l'action ; renvoie false (et prévient l'utilisateur) en cas d'échec. */
    protected boolean tenter(Runnable action) {
        try {
            action.run();
            return true;
        } catch (MetierException e) {
            vue.afficherErreur(e.getMessage());
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Erreur d'accès aux données", e);
            vue.afficherErreur(ERREUR_BASE);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Erreur inattendue", e);
            vue.afficherErreur(ERREUR_INATTENDUE);
        }
        return false;
    }

    static final String ERREUR_BASE = "Erreur d'accès à la base de données.\n"
            + "Vérifiez que le serveur MySQL est démarré, puis réessayez.\n"
            + "Le détail technique est enregistré dans le journal (dossier GestiMed).";

    static final String ERREUR_INATTENDUE = "Une erreur inattendue s'est produite.\n"
            + "Le détail technique est enregistré dans le journal (dossier GestiMed).";
}
