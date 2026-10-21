package My_application.dao;

import My_application.entities.consultation;
import My_application.entities.pathologie;
import My_application.entities.symptome;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Accès aux consultations et à leurs tables d'association
 * (consultsymptome, consultpathologie).
 * <p>
 * Les méthodes {@code find*} renvoient les consultations « en-tête » : le
 * patient et le médecin n'y portent que leur identifiant ; c'est le service
 * qui complète l'agrégat (symptômes, ordonnance, analyses...).
 */
public interface ConsultationDao {

    /** Insère l'en-tête de la consultation et lui affecte l'identifiant généré. */
    consultation insert(consultation c);

    List<consultation> findByPatient(int idPatient);

    List<consultation> findByDate(LocalDate date);

    void ajouterSymptomes(int idConsultation, List<symptome> symptomes);

    void ajouterPathologies(int idConsultation, List<pathologie> pathologies);

    List<symptome> findSymptomes(int idConsultation);

    List<pathologie> findPathologies(int idConsultation);

    /** Symptômes de toutes les consultations du patient, par identifiant de consultation. */
    Map<Integer, List<symptome>> findSymptomesParPatient(int idPatient);

    /** Pathologies de toutes les consultations du patient, par identifiant de consultation. */
    Map<Integer, List<pathologie>> findPathologiesParPatient(int idPatient);
}
