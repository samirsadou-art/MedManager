package My_application.admin;

import My_application.controller.MembresController;
import My_application.controller.MembresVue;
import My_application.entities.User;
import My_application.service.MembresService;
import My_application.vue.Dialogues;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * Page « Membres du cabinet » du compte administrateur : nom, rôle,
 * identifiant et état de chaque compte. L'ajout et la modification se font
 * pour l'instant avec l'outil {@code GestionComptes}.
 */
public class PageMembres extends JPanel implements MembresVue {

    private static final long serialVersionUID = 1L;

    private final transient MembresController controleur;
    private final ModeleMembres modele = new ModeleMembres();

    public PageMembres(MembresService membresService) {
        super(new BorderLayout());
        setBackground(Theme.FOND);
        JTable table = new JTable(modele);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        JScrollPane defilement = new JScrollPane(table);
        JPanel carte = new JPanel(new BorderLayout());
        carte.add(defilement, BorderLayout.CENTER);
        Theme.carte(carte, "Comptes de connexion");
        setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        add(carte, BorderLayout.CENTER);
        Theme.harmoniser(this);
        controleur = new MembresController(this, membresService);
    }

    public void rafraichir() {
        controleur.rafraichir();
    }

    @Override
    public void afficherMembres(List<User> membres) {
        modele.setDonnees(membres);
    }

    @Override
    public void afficherErreur(String message) {
        Dialogues.erreur(this, message);
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }

    /** Une ligne par membre ; colonnes en lecture seule. */
    static final class ModeleMembres extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private static final String[] COLONNES = {"Nom", "Rôle", "Identifiant", "Médecin n°", "État"};

        private final List<User> donnees = new ArrayList<>();

        void setDonnees(List<User> membres) {
            donnees.clear();
            donnees.addAll(membres);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return donnees.size();
        }

        @Override
        public int getColumnCount() {
            return COLONNES.length;
        }

        @Override
        public String getColumnName(int colonne) {
            return COLONNES[colonne];
        }

        @Override
        public Object getValueAt(int ligne, int colonne) {
            User u = donnees.get(ligne);
            switch (colonne) {
                case 0:
                    return u.toString();
                case 1:
                    return u.getRole().getLibelle();
                case 2:
                    return u.getLogin() == null ? "(aucun)" : u.getLogin();
                case 3:
                    return u.getIdMedecin() == null ? "" : String.valueOf(u.getIdMedecin());
                default:
                    return u.isActif() ? "Actif" : "Désactivé";
            }
        }
    }
}
