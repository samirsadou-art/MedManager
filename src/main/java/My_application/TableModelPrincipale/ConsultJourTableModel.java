package My_application.TableModelPrincipale;

import My_application.entities.consultation;
import My_application.modelTableau.ListeTableModel;

/** Consultations du jour (fenêtre principale). Données fournies par {@code AccueilController}. */
public class ConsultJourTableModel extends ListeTableModel<consultation> {

    private static final long serialVersionUID = 1L;

    public ConsultJourTableModel() {
        super("Genre", "Nom", "Prénom", "Age", "Heure consultation");
    }
}
