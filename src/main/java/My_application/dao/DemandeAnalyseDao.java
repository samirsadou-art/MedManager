package My_application.dao;

import My_application.entities.analyse;
import java.util.List;
import java.util.Map;

/** Analyses demandées lors d'une consultation (table {@code demandanalyse}). */
public interface DemandeAnalyseDao {

    void insertAll(int idConsultation, List<analyse> analyses);

    /** Enregistre les résultats et notes saisis après coup. */
    void updateResultats(int idConsultation, List<analyse> analyses);

    List<analyse> findByConsultation(int idConsultation);

    /** Analyses de toutes les consultations du patient, par identifiant de consultation. */
    Map<Integer, List<analyse>> findByPatient(int idPatient);
}
