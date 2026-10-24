package My_application.controller;

import javax.swing.SwingWorker;

/**
 * Lance un travail hors du fil Swing (EDT), puis sa suite sur l'EDT.
 * <p>
 * Les lectures longues (activité du jour...) ne figent plus la fenêtre.
 * Les tests utilisent {@link #IMMEDIAT}, qui enchaîne les deux sur place.
 */
public interface Executeur {

    void executer(Runnable travail, Runnable suite);

    /** Travail dans un {@link SwingWorker}, suite dans {@code done()} (sur l'EDT). */
    Executeur SWING = (travail, suite) -> new SwingWorker<Void, Void>() {
        @Override
        protected Void doInBackground() {
            travail.run();
            return null;
        }

        @Override
        protected void done() {
            suite.run();
        }
    }.execute();

    /** Tout de suite, dans le fil appelant (tests). */
    Executeur IMMEDIAT = (travail, suite) -> {
        travail.run();
        suite.run();
    };
}
