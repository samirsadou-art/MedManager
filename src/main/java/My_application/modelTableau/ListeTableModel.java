package My_application.modelTableau;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * Modèle de tableau générique : une ligne = un objet métier, rendu par un
 * {@code TableCellRenderer} qui choisit quoi afficher selon la colonne.
 * <p>
 * Factorise trois classes quasi identiques (liste des patients, consultations
 * et rendez-vous du jour). Le modèle ne fait plus aucun accès base : le
 * contrôleur lui fournit les données via {@link #setDonnees(List)}.
 */
public class ListeTableModel<T> extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final String[] colonnes;
    private final List<T> lignes = new ArrayList<>();

    public ListeTableModel(String... colonnes) {
        this.colonnes = colonnes;
    }

    public void setDonnees(List<T> donnees) {
        lignes.clear();
        lignes.addAll(donnees);
        fireTableDataChanged();
    }

    public void addElement(T element) {
        lignes.add(element);
        fireTableDataChanged();
    }

    public void clear() {
        lignes.clear();
        fireTableDataChanged();
    }

    public T getElement(int index) {
        return lignes.get(index);
    }

    public int getSize() {
        return lignes.size();
    }

    @Override
    public int getColumnCount() {
        return colonnes.length;
    }

    @Override
    public int getRowCount() {
        return lignes.size();
    }

    @Override
    public String getColumnName(int col) {
        return colonnes[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        return lignes.get(row);
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }
}
