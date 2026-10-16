package My_application.dao;

import My_application.entities.Rendez_Vous;
import java.sql.Date;
import java.util.List;

public interface RendezVousDao {

    void insert(Rendez_Vous rdv);

    /**
     * Supprime le rendez-vous de CE patient à ce créneau. L'ancienne requête
     * « delete ... where date=? and heure=? » supprimait aussi les rendez-vous
     * des autres patients du même créneau.
     *
     * @return true si une ligne a été supprimée
     */
    boolean delete(Rendez_Vous rdv);

    /** Rendez-vous entre deux dates incluses, triés par date puis heure. */
    List<Rendez_Vous> findEntre(Date debut, Date fin);
}
