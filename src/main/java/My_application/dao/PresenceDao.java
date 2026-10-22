package My_application.dao;

import java.util.Set;

/**
 * Présence des membres (table utilisateur_presence). Les heures sont celles
 * du serveur : les horloges des postes peuvent différer.
 */
public interface PresenceDao {

    /** Ce membre est connecté sur ce poste, maintenant. */
    void signaler(int idUtilisateur, String poste);

    /** Ce membre ferme GestiMed. */
    void quitter(int idUtilisateur);

    /** Membres qui ont donné signe de vie depuis moins de {@code secondes}. */
    Set<Integer> enLigne(int secondes);
}
