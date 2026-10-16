package My_application.dao;

import My_application.entities.patient;
import java.util.List;

public interface PatientDao extends GenericDao<patient, Integer> {

    /** Patients dont le nom commence par le préfixe, triés par nom. */
    List<patient> findByNomCommencantPar(String prefixe);
}
