package My_application.modelTableau;

import My_application.entities.analyse;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * Modèle de tableau (vue) d'une liste d'analyses.
 * <p>
 * Correction : le nombre de colonnes était un champ {@code static}, partagé
 * par toutes les instances. Ouvrir le formulaire de demande (2 colonnes)
 * réduisait aussi le tableau des résultats (4 colonnes) et provoquait une
 * {@code ArrayIndexOutOfBoundsException}.
 */
public class my_modelAnalyse extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final int nbcolonnes;
    private List<analyse> ListAnalyse = new ArrayList<>();
    private final String[] colonnes = {"Analyse", "Observation", "Resultat", "Note"};

    public my_modelAnalyse() {
        this(2);
    }

    public my_modelAnalyse(int nbcol) {
        this.nbcolonnes = nbcol;
    }

    public List<analyse> getListAnalyse() {
        return ListAnalyse;
    }

    /** Le modèle affiche (et modifie) directement cette liste. */
    public void setListAnalyse(List<analyse> liste) {
        this.ListAnalyse = liste;
        fireTableDataChanged();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 2) {
            ListAnalyse.get(rowIndex).setResultat((String) aValue);
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }

    @Override
    public int getRowCount() {
        return ListAnalyse.size();
    }

    @Override
    public int getColumnCount() {
        return nbcolonnes;
    }

    @Override
    public String getColumnName(int column) {
        return colonnes[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        analyse ana = ListAnalyse.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return ana.getDesign();
            case 1:
                return ana.getObserv();
            case 2:
                return ana.getResultat();
            case 3:
                return ana.getNote();
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 2;
    }

    /** Ajoute l'analyse si elle n'est pas déjà demandée ; renvoie true si ajoutée. */
    public boolean ajout(analyse ana) {
        for (analyse a : ListAnalyse) {
            if (ana.egal(a)) {
                return false;
            }
        }
        ListAnalyse.add(ana);
        fireTableDataChanged();
        return true;
    }

    public void suppInd(int i) {
        ListAnalyse.remove(i);
        fireTableDataChanged();
    }

    public analyse getAnal(int i) {
        return ListAnalyse.get(i);
    }
}
