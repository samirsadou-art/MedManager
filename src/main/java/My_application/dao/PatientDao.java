package My_application.dao;

import My_application.entities.patient;
import java.time.LocalDate;
import java.util.List;

public interface PatientDao extends GenericDao<patient, Integer> {

    /** Patients dont le nom commence par le préfixe, triés par nom. */
    List<patient> findByNomCommencantPar(String prefixe);

    /** Patients dont le téléphone (chiffres seuls) commence par le préfixe. */
    List<patient> findByTelephoneCommencantPar(String prefixe);

    /** Même nom et même date de naissance, ou même téléphone (s'il n'est pas null). */
    List<patient> findDoublons(String nom, LocalDate naissance, String telephone);
}
