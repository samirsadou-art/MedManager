package My_application.dao;

import My_application.entities.Rendez_Vous;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface RendezVousDao {

    /** Enregistre le rendez-vous et lui donne son identifiant. */
    void insert(Rendez_Vous rdv);

    /**
     * Change l'état du rendez-vous seulement s'il est encore dans l'état
     * {@code etatAttendu} (un autre poste a pu le modifier entre-temps). On ne
     * supprime plus de rendez-vous : une annulation ou une absence est un état.
     *
     * @return true si le rendez-vous a été modifié
     */
    boolean changerEtat(int idRendezVous, int etatAttendu, int nouvelEtat);

    /**
     * Déplace un rendez-vous encore prévu vers un autre créneau (et
     * éventuellement un autre médecin).
     *
     * @return true si le rendez-vous a été modifié
     * @throws DoublonException si le créneau est déjà occupé chez ce médecin
     */
    boolean deplacer(int idRendezVous, LocalDate jour, LocalTime heure, int idMedecin);

    /** Rendez-vous entre deux dates incluses (tous états), triés par date puis heure. */
    List<Rendez_Vous> findEntre(LocalDate debut, LocalDate fin);

    /** Rendez-vous de ces patients (tous états), du plus récent au plus ancien. */
    List<Rendez_Vous> findDesPatients(Collection<Integer> idPatients);

    /** Nombre de rendez-vous encore prévus chez ce médecin à partir de {@code jour} inclus. */
    int compterPrevus(int idMedecin, LocalDate jour);
}
