package My_application.TableModelPrincipale;

import My_application.entities.patient;
import My_application.modelTableau.ListeTableModel;

/** Patients en salle d'attente (fenêtre principale). */
public class SalleAttenTableModel extends ListeTableModel<patient> {

    private static final long serialVersionUID = 1L;

    public SalleAttenTableModel() {
        super("Genre", "Nom", "Prénom", "Age");
    }
}
