package My_application.dao;

import java.time.LocalDateTime;

/**
 * Changements signalés aux autres postes (table evenement) : chaque poste
 * relit régulièrement le dernier identifiant et ne recharge ses écrans que
 * s'il a changé.
 */
public interface EvenementDao {

    void publier(String type, Integer idObjet);

    /** Identifiant du dernier événement, 0 s'il n'y en a aucun. */
    long dernier();

    /** Supprime les événements antérieurs à cette date ; renvoie leur nombre. */
    int purgerAvant(LocalDateTime limite);
}
