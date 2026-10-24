package My_application.dao;

import My_application.entities.medecin;
import java.util.List;

public interface MedecinDao {

    medecin findById(int id);

    /** Tous les médecins du cabinet, actifs ou non, triés par nom puis prénom. */
    List<medecin> findAll();

    /** Crée la fiche ; renvoie le médecin avec son identifiant. */
    medecin insert(medecin m);

    /** Coordonnées de la fiche (pas l'état actif). */
    void modifier(medecin m);

    void definirActif(int idMedecin, boolean actif);
}
