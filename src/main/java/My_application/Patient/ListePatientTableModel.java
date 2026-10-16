package My_application.Patient;

import My_application.entities.patient;
import My_application.modelTableau.ListeTableModel;

/** Liste des patients. Données fournies par {@code ListePatientController}. */
public class ListePatientTableModel extends ListeTableModel<patient> {

    private static final long serialVersionUID = 1L;

    public ListePatientTableModel() {
        super("Genre", "Nom", "Prénom", "Age");
    }
}
