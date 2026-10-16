package My_application.dao;

import My_application.entities.medicament;
import java.util.List;

public interface MedicamentDao {

    List<medicament> findByNomCommencantPar(String prefixe);

    medicament findByNom(String nom);

    /** Formes galéniques déjà connues (comprimé, sirop...), sans doublon. */
    List<String> findFormes();

    medicament insert(String nom, String forme);
}
