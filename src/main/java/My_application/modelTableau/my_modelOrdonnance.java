package My_application.modelTableau;

import My_application.entities.medicament;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * Modèle de tableau (vue) d'une ordonnance.
 * <p>
 * Il travaille sur une COPIE des médicaments : le formulaire peut être annulé
 * sans modifier l'ordonnance de la consultation (avant, « Annuler » gardait
 * les modifications puisque le TableModel était l'ordonnance elle-même).
 */
public class my_modelOrdonnance extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final List<medicament> ListMedicament = new ArrayList<>();
    private final String[] colonnes = {
        "Nom du médicament", "Nombre unités", "Posologie", "Moment", "Durée du traitement"};

    public my_modelOrdonnance() {
    }

    /** Modèle initialisé avec une copie des médicaments donnés. */
    public my_modelOrdonnance(List<medicament> medicaments) {
        for (medicament m : medicaments) {
            ListMedicament.add(new medicament(m));
        }
    }

    public List<medicament> getListMedicament() {
        return new ArrayList<>(ListMedicament);
    }

    @Override
    public int getRowCount() {
        return ListMedicament.size();
    }

    @Override
    public int getColumnCount() {
        return colonnes.length;
    }

    @Override
    public String getColumnName(int column) {
        return colonnes[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? String.class : Integer.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        medicament med = ListMedicament.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return med.getDesignmed();
            case 1:
                return med.getUnite();
            case 2:
                return med.getPosologie();
            case 3:
                return med.getMode();
            case 4:
                return med.getDuree();
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /** Ajoute le médicament s'il n'est pas déjà dans l'ordonnance ; renvoie true si ajouté. */
    public boolean ajout(medicament med) {
        for (medicament m : ListMedicament) {
            if (med.egal(m)) {
                return false;
            }
        }
        ListMedicament.add(med);
        fireTableDataChanged();
        return true;
    }

    public void suppInd(int i) {
        ListMedicament.remove(i);
        fireTableDataChanged();
    }

    public medicament getMedicament(int i) {
        return ListMedicament.get(i);
    }
}
