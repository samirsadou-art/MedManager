package My_application.TableModelPrincipale;

import My_application.entities.Rendez_Vous;
import My_application.modelTableau.ListeTableModel;

/** Rendez-vous du jour (fenêtre principale). Données fournies par {@code AccueilController}. */
public class RendezJourTableModel extends ListeTableModel<Rendez_Vous> {

    private static final long serialVersionUID = 1L;

    public RendezJourTableModel() {
        super("Genre", "Nom", "Prénom", "Age", "Horaire", "Statut");
    }
}
