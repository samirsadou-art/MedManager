package My_application.dao;

import My_application.entities.EntreeJournal;
import java.util.List;

/** Journal des accès : on ajoute et on lit, on ne modifie ni ne supprime jamais. */
public interface JournalDao {

    void ajouter(EntreeJournal entree);

    /** Accès au dossier du patient, du plus récent au plus ancien. */
    List<EntreeJournal> findByPatient(int idPatient);
}
