package My_application.controller;

import My_application.entities.MessageInterne;
import My_application.service.MessagerieService.Conversation;
import java.util.List;

/** Fenêtre de messagerie interne. */
public interface MessagerieVue extends Vue {

    /** Liste de gauche : « Tout le cabinet » puis les membres. */
    void afficherConversations(List<Conversation> conversations);

    /** Messages du fil ouvert (null : tout le cabinet). */
    void afficherMessages(Integer autre, List<MessageInterne> messages);

    /** Nombre total de messages non lus (badge du menu). */
    void afficherNonLus(int total);

    /** Un nouveau message est arrivé (son). */
    void signalerNouveauMessage();

    /** Vrai si la fenêtre est à l'écran : le fil ouvert est alors lu au fur et à mesure. */
    boolean estAffichee();
}
