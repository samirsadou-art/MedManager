package My_application.Patient;

import My_application.controller.ListePatientController;
import My_application.controller.ListePatientVue;
import My_application.entities.DossierPatient;
import My_application.entities.medecin;
import My_application.entities.patient;
import My_application.modelTableau.ListeTableModel;
import My_application.service.PatientService;
import My_application.vue.Dialogues;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Page « Patients » dans le panneau central de la fenêtre principale (elle
 * remplace l'ancienne fenêtre séparée {@code ListePatient}) : recherche par
 * nom, téléphone ou numéro de dossier, inscription, détails du patient (d'où
 * l'on modifie sa fiche), rendez-vous et arrivée sans rendez-vous. Le dossier
 * médical ne s'ouvre que pour un médecin ; un double-clic ouvre les détails
 * pour la secrétaire.
 */
public class PagePatients extends JPanel implements ListePatientVue {

    private static final long serialVersionUID = 1L;
    /** Attente après la dernière frappe avant de relire la liste. */
    static final int DELAI_RECHERCHE_MS = 250;

    private final transient PatientService patients;
    private final transient ListePatientController controleur;
    private final transient Consumer<DossierPatient> ouvrir;
    private final transient Consumer<patient> rendezVous;
    private final boolean medecin;
    private final ModelePatients modele = new ModelePatients();
    private final JTable table = new JTable(modele);
    private final JTextField recherche = new JTextField(28);
    private final JLabel resume = new JLabel(" ");
    private final JButton ouvrirBtn = new JButton("Ouvrir le dossier", Icones.svg("folder-open", 16, Theme.PRIMAIRE));
    private final JButton detailsBtn = new JButton("Détails", Icones.svg("eye", 16, Theme.PRIMAIRE));
    private final JButton rendezVousBtn = new JButton("Rendez-vous", Icones.svg("calendar-event", 16, Theme.PRIMAIRE));
    private final JButton salleBtn = new JButton("Salle d'attente", Icones.svg("clock", 16, Theme.PRIMAIRE));
    private final Timer delai;

    /**
     * @param creerControleur fabrique le contrôleur avec cette page comme vue
     * @param ouvrir     affiche le dossier médical chargé (médecin)
     * @param rendezVous ouvre l'agenda pour le patient choisi
     */
    public PagePatients(PatientService patients, Function<ListePatientVue, ListePatientController> creerControleur,
            boolean medecin, Consumer<DossierPatient> ouvrir, Consumer<patient> rendezVous) {
        super(new BorderLayout(0, 12));
        this.patients = patients;
        this.medecin = medecin;
        this.ouvrir = ouvrir;
        this.rendezVous = rendezVous;
        this.controleur = creerControleur.apply(this);
        setBackground(Theme.FOND);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        delai = new Timer(DELAI_RECHERCHE_MS, e -> rechercher());
        delai.setRepeats(false);
        recherche.putClientProperty("JTextField.placeholderText", "Nom, téléphone ou n° de dossier");
        recherche.putClientProperty("JTextField.leadingIcon", Icones.svg("search", 16, Theme.TEXTE_SECONDAIRE));
        recherche.putClientProperty("JTextField.showClearButton", Boolean.TRUE);
        recherche.setPreferredSize(new Dimension(320, recherche.getPreferredSize().height));
        recherche.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                delai.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                delai.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                delai.restart();
            }
        });
        recherche.addActionListener(e -> {
            delai.stop();
            rechercher();
        });

        JButton nouveau = new JButton("Nouveau patient", Icones.svg("user-plus", 16, Theme.PRIMAIRE));
        nouveau.addActionListener(e -> inscrire());
        ouvrirBtn.addActionListener(e -> controleur.ouvrirDossier(selection()));
        detailsBtn.setToolTipText("État civil et coordonnées du patient ; la fiche se modifie depuis cette fenêtre");
        detailsBtn.addActionListener(e -> afficherDetails(selection()));
        rendezVousBtn.setToolTipText("Prendre un rendez-vous pour ce patient");
        rendezVousBtn.addActionListener(e -> prendreRendezVous(selection()));
        salleBtn.setToolTipText("Patient arrivé sans rendez-vous : le mettre en salle d'attente");
        salleBtn.addActionListener(e -> mettreEnSalle(selection()));
        ouvrirBtn.setVisible(medecin);
        // La salle d'attente est tenue par la secrétaire : le médecin n'y met pas de patient d'ici.
        salleBtn.setVisible(!medecin);

        JPanel barre = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barre.setOpaque(false);
        JLabel libelle = new JLabel("Rechercher");
        libelle.setForeground(Theme.TEXTE_SECONDAIRE);
        barre.add(libelle);
        barre.add(recherche);
        barre.add(Box.createHorizontalStrut(16));
        barre.add(nouveau);
        add(barre, BorderLayout.NORTH);

        // Actions sur le patient choisi : en tête de la liste, à droite du résumé.
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(ouvrirBtn);
        actions.add(detailsBtn);
        actions.add(rendezVousBtn);
        actions.add(salleBtn);

        table.setRowHeight(40);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new Rendu());
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getSelectionModel().addListSelectionListener(e -> activerBoutons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
                    if (PagePatients.this.medecin) {
                        controleur.ouvrirDossier(selection());
                    } else {
                        afficherDetails(selection());
                    }
                }
            }
        });
        activerBoutons();

        JPanel carte = new JPanel(new BorderLayout(0, 6));
        resume.setForeground(Theme.TEXTE_SECONDAIRE);
        JPanel tete = new JPanel(new BorderLayout(12, 0));
        tete.setOpaque(false);
        tete.add(resume, BorderLayout.CENTER);
        tete.add(actions, BorderLayout.EAST);
        carte.add(tete, BorderLayout.NORTH);
        carte.add(new JScrollPane(table), BorderLayout.CENTER);
        Theme.carte(carte, "Patients");
        add(carte, BorderLayout.CENTER);

        Theme.harmoniser(this);
    }

    /** À chaque affichage de la page : relit la liste pour la recherche en cours. */
    public void rafraichir() {
        rechercher();
    }

    /** Met le curseur dans le champ de recherche (à l'ouverture de la page). */
    public void saisirRecherche() {
        SwingUtilities.invokeLater(recherche::requestFocusInWindow);
    }

    /** Pour les tests : patients affichés. */
    ListeTableModel<patient> modele() {
        return modele;
    }

    /** Pour les tests : texte de la recherche. */
    JTextField champRecherche() {
        return recherche;
    }

    JButton boutonDetails() {
        return detailsBtn;
    }

    JButton boutonSalleAttente() {
        return salleBtn;
    }

    void rechercher() {
        controleur.rechercher(recherche.getText().trim());
    }

    private void activerBoutons() {
        boolean une = selection() != null;
        ouvrirBtn.setEnabled(une);
        detailsBtn.setEnabled(une);
        rendezVousBtn.setEnabled(une);
        salleBtn.setEnabled(une);
    }

    private Window fenetre() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void inscrire() {
        FichePatient fiche = new FichePatient(fenetre(), patients);
        if (medecin) {
            fiche.sansSalleAttente();
        }
        fiche.setVisible(true);
        patient p = fiche.getPatient();
        if (p == null) {
            return;
        }
        afficherSeul(p);
        if (fiche.getSuite() == FichePatient.Suite.RENDEZ_VOUS) {
            prendreRendezVous(p);
        } else if (fiche.getSuite() == FichePatient.Suite.SALLE_ATTENTE) {
            mettreEnSalle(p);
        }
    }

    /** Fenêtre des détails ; si la fiche y a été modifiée, la liste montre le patient à jour. */
    private void afficherDetails(patient p) {
        if (p == null) {
            return;
        }
        DetailsPatient details = new DetailsPatient(fenetre(), patients, p);
        details.setVisible(true);
        if (details.getPatientModifie() != null) {
            afficherSeul(details.getPatientModifie());
        }
    }

    private void prendreRendezVous(patient p) {
        if (p != null) {
            rendezVous.accept(p);
        }
    }

    /** Patient arrivé sans rendez-vous : on choisit son médecin s'il y en a plusieurs. */
    private void mettreEnSalle(patient p) {
        if (p == null) {
            return;
        }
        Integer idMedecin = null;
        List<medecin> medecins = controleur.medecinsAChoisir();
        if (!medecins.isEmpty()) {
            Object choix = JOptionPane.showInputDialog(this, "Médecin qui recevra " + p + " :",
                    "Salle d'attente", JOptionPane.QUESTION_MESSAGE, null, medecins.toArray(), medecins.get(0));
            if (choix == null) {
                return;
            }
            idMedecin = ((medecin) choix).getIdmedecin();
        }
        controleur.mettreEnSalle(p, idMedecin);
    }

    /** Filtre la liste sur le patient (inscrit ou modifié) et le sélectionne. */
    private void afficherSeul(patient p) {
        recherche.setText(p.getNom());
        delai.stop();
        controleur.rechercher(p.getNom());
        selectionner(p.getIdentPat());
    }

    private void selectionner(int idPatient) {
        for (int i = 0; i < modele.getRowCount(); i++) {
            if (modele.getElement(i).getIdentPat() == idPatient) {
                int ligne = table.convertRowIndexToView(i);
                table.setRowSelectionInterval(ligne, ligne);
                table.scrollRectToVisible(table.getCellRect(ligne, 0, true));
                return;
            }
        }
    }

    private patient selection() {
        int ligne = table.getSelectedRow();
        return ligne < 0 ? null : modele.getElement(table.convertRowIndexToModel(ligne));
    }

    /** « 12 patients · double-cliquez pour ouvrir le dossier ». */
    static String resume(int nombre, boolean recherche, boolean medecin) {
        if (nombre == 0) {
            return recherche ? "Aucun patient ne correspond à la recherche." : "Aucun patient inscrit.";
        }
        return nombre + " patient" + (nombre > 1 ? "s" : "")
                + (recherche ? " trouvé" + (nombre > 1 ? "s" : "") : "")
                + " · double-cliquez pour ouvrir " + (medecin ? "le dossier médical" : "les détails");
    }

    // ----- ListePatientVue -----

    @Override
    public void afficherPatients(List<patient> liste) {
        patient avant = selection();
        modele.setDonnees(liste);
        resume.setText(resume(liste.size(), !recherche.getText().trim().isEmpty(), medecin));
        if (avant != null) {
            selectionner(avant.getIdentPat());
        }
        activerBoutons();
    }

    @Override
    public void afficherDossier(DossierPatient dossier) {
        ouvrir.accept(dossier);
    }

    @Override
    public void afficherErreur(String message) {
        Dialogues.erreur(this, message);
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }

    /** Colonnes de la page : avatar, numéro de dossier, nom, prénom, âge (le téléphone est dans les détails). */
    static final class ModelePatients extends ListeTableModel<patient> {

        private static final long serialVersionUID = 1L;

        ModelePatients() {
            super("Genre", "N° dossier", "Nom", "Prénom", "Âge");
        }
    }

    /** Une ligne = un patient ; la colonne choisit ce qui est affiché. */
    private static final class Rendu extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valeur, boolean choisie, boolean focus,
                int ligne, int colonne) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, "", choisie, focus, ligne, colonne);
            Theme.cellule(l, t, choisie, ligne);
            patient p = (patient) valeur;
            l.setIcon(null);
            l.setHorizontalAlignment(colonne == 0 || colonne == 4 ? CENTER : LEFT);
            switch (colonne) {
                case 0:
                    l.setIcon(Icones.patient(p, 32));
                    break;
                case 1:
                    l.setText(p.getNumeroDossier() == null ? "" : p.getNumeroDossier());
                    break;
                case 2:
                    l.setText(p.getNom().toUpperCase(Locale.FRENCH));
                    l.setFont(Theme.policeGras());
                    break;
                case 3:
                    l.setText(p.getPrenom());
                    break;
                default:
                    l.setText(p.getStringAge());
            }
            return l;
        }
    }
}
