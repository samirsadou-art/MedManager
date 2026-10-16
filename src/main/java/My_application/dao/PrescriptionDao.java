package My_application.dao;

import My_application.entities.medicament;
import java.util.List;

/** Lignes d'ordonnance (table {@code prescrire}). */
public interface PrescriptionDao {

    void insertAll(int idConsultation, List<medicament> medicaments);

    List<medicament> findByConsultation(int idConsultation);
}
