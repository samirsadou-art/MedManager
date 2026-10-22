package My_application.dao;

import My_application.entities.Passage;
import java.time.LocalDate;
import java.util.List;

/** Passages dans la salle d'attente (table salle_attente). */
public interface SalleAttenteDao {

    /**
     * Enregistre l'arrivée et donne son identifiant au passage.
     *
     * @throws DoublonException si le patient est déjà dans la salle ce jour-là,
     *                          ou si ce rendez-vous a déjà son passage
     */
    void insert(Passage p);

    /** Passages du jour (tous états), avec le patient et l'heure du rendez-vous. */
    List<Passage> findDuJour(LocalDate jour);

    /**
     * Enregistre l'état, les heures d'appel et de sortie, la priorité, les
     * frais et le paiement du passage, seulement s'il est encore dans l'état
     * {@code etatAttendu}.
     *
     * @return true si le passage a été modifié
     */
    boolean mettreAJour(Passage p, int etatAttendu);

    /** Derniers frais fixés par ce médecin (pour les proposer à nouveau) ; null s'il n'en a jamais fixé. */
    Integer dernierFrais(int idMedecin);
}
