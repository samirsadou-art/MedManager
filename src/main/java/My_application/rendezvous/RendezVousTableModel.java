package My_application.rendezvous;

import My_application.entities.Rendez_Vous;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.DefaultListModel;
import javax.swing.table.AbstractTableModel;

/**
 * Grille hebdomadaire de l'agenda (vue) : une ligne par heure d'ouverture,
 * une colonne par jour (dimanche → samedi). Chaque cellule contient la liste
 * des rendez-vous de l'heure.
 * <p>
 * Le modèle ne lit plus la base (l'ancien {@code chargerFromBD} faisait
 * 1 + 2×N requêtes) : le contrôleur lui fournit les rendez-vous de la semaine
 * via {@link #afficherSemaine(LocalDate, List)}.
 */
public class RendezVousTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] JOURS = {"Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"};
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.FRENCH);

    private final int maxParHeure;
    private final int heureOuverture;
    private final Object[][] data;
    private final String[] columnNames = new String[8];
    private LocalDate dimanche;

    public RendezVousTableModel(int maxParHeure, int heureOuverture, int nombreHeures) {
        this.maxParHeure = maxParHeure;
        this.heureOuverture = heureOuverture;
        this.data = new Object[nombreHeures][8];
        columnNames[0] = "Horaire/Date";
        for (int i = 0; i < nombreHeures; i++) {
            int h = heureOuverture + i;
            data[i][0] = String.format("%02d:00 - %02d:00", h, h + 1);
        }
        for (int j = 1; j < 8; j++) {
            columnNames[j] = JOURS[j - 1];
        }
    }

    /** Affiche la semaine commençant au dimanche donné. */
    public void afficherSemaine(LocalDate dimancheDeLaSemaine, List<Rendez_Vous> rendezVous) {
        this.dimanche = dimancheDeLaSemaine;
        for (int j = 1; j < 8; j++) {
            columnNames[j] = JOURS[j - 1] + " [" + FORMAT.format(dimanche.plusDays(j - 1)) + "]";
            for (int i = 0; i < data.length; i++) {
                data[i][j] = null;
            }
        }
        for (Rendez_Vous r : rendezVous) {
            LocalDate jour = r.getDate().toLocalDate();
            int col = (int) java.time.temporal.ChronoUnit.DAYS.between(dimanche, jour) + 1;
            int row = r.getHeure().toLocalTime().getHour() - heureOuverture;
            if (col >= 1 && col <= 7 && row >= 0 && row < data.length) {
                ajouter(r, row, col);
            }
        }
        fireTableStructureChanged();
    }

    private void ajouter(Rendez_Vous r, int row, int col) {
        @SuppressWarnings("unchecked")
        DefaultListModel<Rendez_Vous> liste = (DefaultListModel<Rendez_Vous>) data[row][col];
        if (liste == null) {
            liste = new DefaultListModel<>();
            data[row][col] = liste;
        }
        liste.addElement(r);
    }

    /** Jour correspondant à une colonne (1 = dimanche). */
    public LocalDate jourDeColonne(int col) {
        return dimanche == null || col < 1 ? null : dimanche.plusDays(col - 1);
    }

    /** Heure de début correspondant à une ligne. */
    public int heureDeLigne(int row) {
        return heureOuverture + row;
    }

    public LocalDate getDimanche() {
        return dimanche;
    }

    public int getMaxParH() {
        return maxParHeure;
    }

    @SuppressWarnings("unchecked")
    public DefaultListModel<Rendez_Vous> getListModel(int row, int col) {
        return (DefaultListModel<Rendez_Vous>) data[row][col];
    }

    public int getNombrePatient(int row, int col) {
        DefaultListModel<?> l = (DefaultListModel<?>) data[row][col];
        return l == null ? 0 : l.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return data.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        return data[row][col];
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
