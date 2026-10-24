package My_application.dao;

import My_application.entities.medicament;
import java.util.List;
import java.util.Map;

/** Lignes d'ordonnance (table {@code prescrire}). */
public interface PrescriptionDao {

    void insertAll(int idConsultation, List<medicament> medicaments);

    List<medicament> findByConsultation(int idConsultation);

    /** Ordonnances de toutes les consultations du patient, par identifiant de consultation. */
    Map<Integer, List<medicament>> findByPatient(int idPatient);
}
