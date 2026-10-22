package My_application.dao;

import My_application.entities.medecin;
import java.util.List;

public interface MedecinDao {

    medecin findById(int id);

    /** Tous les médecins du cabinet, triés par nom puis prénom. */
    List<medecin> findAll();
}
