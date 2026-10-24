package My_application.dao;

import My_application.entities.Statistiques;
import java.time.LocalDate;
import java.util.Map;

/**
 * Comptages pour le tableau de bord de l'administrateur, entre deux dates
 * incluses. {@code idMedecin} null : tout le cabinet. Rien n'est lu
 * patient par patient.
 */
public interface StatistiquesDao {

    Map<LocalDate, Integer> consultationsParJour(LocalDate debut, LocalDate fin, Integer idMedecin);

    Map<Integer, Integer> consultationsParMedecin(LocalDate debut, LocalDate fin);

    int patientsDistincts(LocalDate debut, LocalDate fin, Integer idMedecin);

    /** Patients dont la première consultation (tous médecins) est dans la période, chez ce médecin. */
    int nouveauxPatients(LocalDate debut, LocalDate fin, Integer idMedecin);

    Map<Integer, Integer> rendezVousParEtat(LocalDate debut, LocalDate fin, Integer idMedecin);

    Statistiques.SalleAttente salleAttente(LocalDate debut, LocalDate fin, Integer idMedecin);
}
