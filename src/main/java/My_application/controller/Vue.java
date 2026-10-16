package My_application.controller;

/**
 * Ce que tout contrôleur attend de sa vue (patron MVC) : afficher un message.
 * Les fenêtres Swing implémentent cette interface ; les tests utilisent une
 * fausse vue qui enregistre les messages.
 */
public interface Vue {

    void afficherErreur(String message);

    void afficherInformation(String message);
}
