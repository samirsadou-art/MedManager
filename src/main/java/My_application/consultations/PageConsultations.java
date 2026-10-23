package My_application.consultations;

import My_application.controller.ConsultationsController;
import My_application.controller.ConsultationsVue;
import My_application.controller.Executeur;
import My_application.entities.DossierPatient;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.modelTableau.ListeTableModel;
import My_application.service.DossierService;
import My_application.service.PatientService;
import My_application.service.RendezVousService;
import My_application.vue.Dialogues;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Page « Consultations » du médecin, dans le panneau central de la fenêtre
 * principale : consultations d'une date choisie (aujourd'hui au départ),
 * jour précédent ou suivant en un clic. Double-clic ou « Ouvrir le dossier »
 * ouvre le dossier médical du patient.
 */
public class PageConsultations extends JPanel implements ConsultationsVue {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private final transient ConsultationsController controleur;
    private final transient Consumer<DossierPatient> ouvrir;
    private final JSpinner date = new JSpinner(new SpinnerDateModel());
    private final ListeTableModel<consultation> modele = new ListeTableModel<>(
            "Genre", "N° dossier", "Nom", "Prénom", "Âge", "Heure");
    private final JTable table = new JTable(modele);
    private final JLabel resume = new JLabel(" ");
    private final JButton ouvrirBtn = new JButton("Ouvrir le dossier", Icones.svg("folder-open", 16, Theme.PRIMAIRE));
    /** Évite de relire pendant qu'on remet la date affichée dans le champ. */
    private boolean enCours;

    public PageConsultations(DossierService dossiers, PatientService patients, RendezVousService rendezVous,
            Supplier<medecin> medecinTraitant, Consumer<DossierPatient> ouvrir) {
        this(dossiers, patients, rendezVous, medecinTraitant, ouvrir, Executeur.SWING);
    }

    PageConsultations(DossierService dossiers, PatientService patients, RendezVousService rendezVous,
            Supplier<medecin> medecinTraitant, Consumer<DossierPatient> ouvrir, Executeur executeur) {
        super(new BorderLayout(0, 12));
        this.ouvrir = ouvrir;
        setBackground(Theme.FOND);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        controleur = new ConsultationsController(this, dossiers, patients, rendezVous, medecinTraitant, executeur);

        date.setEditor(new JSpinner.DateEditor(date, "dd/MM/yyyy"));
        date.addChangeListener(e -> {
            if (!enCours) {
                controleur.afficher(jourChoisi());
            }
        });
        JButton precedent = new JButton(Icones.svg("chevron-left", 16, Theme.PRIMAIRE));
        precedent.setToolTipText("Jour précédent");
        precedent.addActionListener(e -> decaler(-1));
        JButton suivant = new JButton(Icones.svg("chevron-right", 16, Theme.PRIMAIRE));
        suivant.setToolTipText("Jour suivant");
        suivant.addActionListener(e -> decaler(1));
        JButton aujourdhui = new JButton("Aujourd'hui", Icones.svg("calendar", 16, Theme.PRIMAIRE));
        aujourdhui.addActionListener(e -> controleur.afficher(LocalDate.now()));
        ouvrirBtn.addActionListener(e -> controleur.ouvrirDossier(selection()));

        JPanel filtres = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtres.setOpaque(false);
        JLabel libelle = new JLabel("Date des consultations");
        libelle.setForeground(Theme.TEXTE_SECONDAIRE);
        filtres.add(libelle);
        filtres.add(precedent);
        filtres.add(date);
        filtres.add(suivant);
        filtres.add(aujourdhui);
        filtres.add(Box.createHorizontalStrut(16));
        filtres.add(ouvrirBtn);
        add(filtres, BorderLayout.NORTH);

        table.setRowHeight(40);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new Rendu());
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getSelectionModel().addListSelectionListener(e -> ouvrirBtn.setEnabled(selection() != null));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
                    controleur.ouvrirDossier(selection());
                }
            }
        });
        ouvrirBtn.setEnabled(false);

        JPanel carte = new JPanel(new BorderLayout(0, 6));
        resume.setForeground(Theme.TEXTE_SECONDAIRE);
        carte.add(resume, BorderLayout.NORTH);
        carte.add(new JScrollPane(table), BorderLayout.CENTER);
        Theme.carte(carte, "Consultations");
        add(carte, BorderLayout.CENTER);

        Theme.harmoniser(this);
    }

    /** À chaque affichage de la page : relit la date affichée (aujourd'hui la première fois). */
    public void rafraichir() {
        controleur.rafraichir();
    }

    private void decaler(int jours) {
        controleur.afficher(jourChoisi().plusDays(jours));
    }

    private LocalDate jourChoisi() {
        return ((Date) date.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private consultation selection() {
        int ligne = table.getSelectedRow();
        return ligne < 0 ? null : modele.getElement(table.convertRowIndexToModel(ligne));
    }

    /** « 3 consultations le jeudi 23 octobre 2026 ». */
    static String resume(LocalDate jour, int nombre) {
        String quand = (jour.equals(LocalDate.now()) ? "aujourd'hui, " : "le ") + jour.format(JOUR);
        return nombre == 0 ? "Aucune consultation " + quand + "."
                : nombre + " consultation" + (nombre > 1 ? "s " : " ") + quand
                + " · double-cliquez pour ouvrir le dossier du patient";
    }

    // ----- ConsultationsVue -----

    @Override
    public void afficherConsultations(LocalDate jour, List<consultation> consultations) {
        enCours = true;
        date.setValue(Date.from(jour.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        enCours = false;
        modele.setDonnees(consultations);
        resume.setText(resume(jour, consultations.size()));
        ouvrirBtn.setEnabled(selection() != null);
    }

    @Override
    public void ouvrirDossier(DossierPatient dossier) {
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

    /** Une ligne = une consultation ; la colonne choisit ce qui est affiché. */
    private static final class Rendu extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valeur, boolean choisie, boolean focus,
                int ligne, int colonne) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, "", choisie, focus, ligne, colonne);
            Theme.cellule(l, t, choisie, ligne);
            consultation c = (consultation) valeur;
            l.setIcon(null);
            l.setHorizontalAlignment(colonne == 0 || colonne >= 4 ? CENTER : LEFT);
            switch (colonne) {
                case 0:
                    l.setIcon(Icones.patient(c.getPatient(), 32));
                    break;
                case 1:
                    l.setText(c.getPatient().getNumeroDossier() == null ? "" : c.getPatient().getNumeroDossier());
                    break;
                case 2:
                    l.setText(c.getPatient().getNom().toUpperCase(Locale.FRENCH));
                    l.setFont(Theme.policeGras());
                    break;
                case 3:
                    l.setText(c.getPatient().getPrenom());
                    break;
                case 4:
                    l.setText(c.getPatient().getStringAge());
                    break;
                default:
                    l.setText(c.getHeureString());
            }
            return l;
        }
    }
}
