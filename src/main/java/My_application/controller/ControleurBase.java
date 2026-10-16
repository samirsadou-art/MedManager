package My_application.controller;

import My_application.dao.DataAccessException;
import My_application.service.MetierException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Traitement commun des erreurs : une règle métier non respectée est affichée
 * telle quelle ; une erreur technique est journalisée et un message clair est
 * affiché, au lieu d'être avalée dans un {@code Logger} invisible pour
 * l'utilisateur comme dans la version d'origine.
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
            vue.afficherErreur("Erreur d'accès à la base de données.\n" + e.getMessage());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Erreur inattendue", e);
            vue.afficherErreur("Erreur inattendue : " + e);
        }
        return false;
    }
}
