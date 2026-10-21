package My_application.controller;

import My_application.dao.DataAccessException;
import My_application.service.MetierException;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
    private final Executeur executeur;

    ControleurBase(Vue vue) {
        this(vue, Executeur.SWING);
    }

    ControleurBase(Vue vue, Executeur executeur) {
        this.vue = vue;
        this.executeur = executeur;
    }

    /** Exécute l'action ; renvoie false (et prévient l'utilisateur) en cas d'échec. */
    protected boolean tenter(Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuntimeException e) {
            signaler(e);
            return false;
        }
    }

    /** Renvoie le résultat de l'action, ou {@code siEchec} (utilisateur prévenu) en cas d'échec. */
    protected <T> T tenter(Supplier<T> action, T siEchec) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            signaler(e);
            return siEchec;
        }
    }

    /**
     * Lit en arrière-plan (la lecture ne doit pas toucher à l'interface), puis
     * affiche le résultat sur l'EDT ; une erreur de lecture est signalée comme
     * avec {@link #tenter(Runnable)}.
     */
    protected <T> void enArrierePlan(Supplier<T> lecture, Consumer<T> affichage) {
        Object[] resultat = new Object[1];
        Throwable[] erreur = new Throwable[1];
        executeur.executer(() -> {
            try {
                resultat[0] = lecture.get();
            } catch (RuntimeException | Error e) {
                erreur[0] = e;
            }
        }, () -> {
            if (erreur[0] instanceof RuntimeException) {
                signaler((RuntimeException) erreur[0]);
            } else if (erreur[0] != null) {
                LOG.log(Level.SEVERE, "Erreur inattendue", erreur[0]);
                vue.afficherErreur(ERREUR_INATTENDUE);
            } else {
                @SuppressWarnings("unchecked")
                T lu = (T) resultat[0];
                tenter(() -> affichage.accept(lu));
            }
        });
    }

    private void signaler(RuntimeException e) {
        if (e instanceof MetierException) {
            vue.afficherErreur(e.getMessage());
        } else if (e instanceof DataAccessException) {
            LOG.log(Level.SEVERE, "Erreur d'accès aux données", e);
            vue.afficherErreur(ERREUR_BASE);
        } else {
            LOG.log(Level.SEVERE, "Erreur inattendue", e);
            vue.afficherErreur(ERREUR_INATTENDUE);
        }
    }

    static final String ERREUR_BASE = "Erreur d'accès à la base de données.\n"
            + "Vérifiez que le serveur MySQL est démarré, puis réessayez.\n"
            + "Le détail technique est enregistré dans le journal (dossier GestiMed).";

    static final String ERREUR_INATTENDUE = "Une erreur inattendue s'est produite.\n"
            + "Le détail technique est enregistré dans le journal (dossier GestiMed).";
}
