package My_application.admin;

import My_application.controller.Executeur;
import My_application.controller.StatistiquesController;
import My_application.controller.StatistiquesVue;
import My_application.entities.Rendez_Vous;
import My_application.entities.Statistiques;
import My_application.entities.medecin;
import My_application.report.RapportService;
import My_application.service.StatistiquesService;
import My_application.service.StatistiquesService.Periode;
import My_application.vue.Dialogues;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;

/**
 * Tableau de bord de l'administrateur : activité du cabinet sur une période,
 * pour tous les médecins ou un seul. Filtres sur une ligne en haut, chiffres
 * clés, consultations dans le temps, rendez-vous par issue et consultations
 * par médecin. Uniquement des nombres : aucun nom de patient.
 */
public class PageStatistiques extends JPanel implements StatistiquesVue {

    private static final long serialVersionUID = 1L;
    private static final String PERSONNALISEE = "Dates choisies";
    private static final String TOUS = "Tous les médecins";

    private final transient StatistiquesController controleur;
    private final JComboBox<Object> periode = new JComboBox<>();
    private final JSpinner du = dateSpinner();
    private final JSpinner au = dateSpinner();
    private final JComboBox<Object> medecin = new JComboBox<>();
    private final Tuile consultations = new Tuile("Consultations");
    private final Tuile patients = new Tuile("Patients vus");
    private final Tuile absences = new Tuile("Taux d'absence");
    private final Tuile attente = new Tuile("Attente moyenne");
    private final GraphiqueBarres graphique = new GraphiqueBarres();
    private final Repartition rendezVous = new Repartition();
    private final DefaultTableModel parMedecin = new DefaultTableModel(new Object[] {"Médecin", "Consultations"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int ligne, int colonne) {
            return false;
        }
    };
    private final JLabel titreGraphique = new JLabel();
    /** Évite de recalculer pendant qu'on remplit les listes. */
    private boolean enCours;

    public PageStatistiques(StatistiquesService service, RapportService rapports) {
        this(service, rapports, Executeur.SWING);
    }

    PageStatistiques(StatistiquesService service, RapportService rapports, Executeur executeur) {
        super(new BorderLayout(0, 12));
        setBackground(Theme.FOND);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        controleur = new StatistiquesController(this, service, rapports, executeur);

        enCours = true;
        for (Periode p : Periode.values()) {
            periode.addItem(p);
        }
        periode.addItem(PERSONNALISEE);
        periode.setSelectedItem(Periode.MOIS);
        medecin.addItem(TOUS);
        appliquerPeriode();
        enCours = false;

        periode.addActionListener(e -> {
            if (periode.getSelectedItem() instanceof Periode) {
                enCours = true;
                appliquerPeriode();
                enCours = false;
                calculer();
            }
        });
        du.addChangeListener(e -> datesChangees());
        au.addChangeListener(e -> datesChangees());
        medecin.addActionListener(e -> calculer());

        JPanel filtres = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtres.setOpaque(false);
        filtres.add(libelle("Période"));
        filtres.add(periode);
        filtres.add(libelle("du"));
        filtres.add(du);
        filtres.add(libelle("au"));
        filtres.add(au);
        filtres.add(Box.createHorizontalStrut(16));
        filtres.add(libelle("Médecin"));
        filtres.add(medecin);
        JButton pdf = new JButton("Exporter en PDF", Icones.svg("printer", 16, Theme.PRIMAIRE));
        pdf.addActionListener(e -> controleur.exporterPdf());
        JButton csv = new JButton("Exporter pour Excel (CSV)", Icones.svg("file-text", 16, Theme.PRIMAIRE));
        csv.addActionListener(e -> exporterCsv());
        filtres.add(Box.createHorizontalStrut(16));
        filtres.add(pdf);
        filtres.add(csv);

        JPanel tuiles = new JPanel(new GridLayout(1, 4, 12, 0));
        tuiles.setOpaque(false);
        tuiles.add(consultations);
        tuiles.add(patients);
        tuiles.add(absences);
        tuiles.add(attente);

        JPanel haut = new JPanel(new BorderLayout(0, 12));
        haut.setOpaque(false);
        haut.add(filtres, BorderLayout.NORTH);
        haut.add(tuiles, BorderLayout.CENTER);
        add(haut, BorderLayout.NORTH);

        JPanel carteGraphique = new JPanel(new BorderLayout(0, 6));
        titreGraphique.setForeground(Theme.TEXTE_SECONDAIRE);
        carteGraphique.add(titreGraphique, BorderLayout.NORTH);
        carteGraphique.add(graphique, BorderLayout.CENTER);
        Theme.carte(carteGraphique, "Consultations dans le temps");
        add(carteGraphique, BorderLayout.CENTER);

        JPanel carteRdv = new JPanel(new BorderLayout());
        carteRdv.add(rendezVous, BorderLayout.CENTER);
        Theme.carte(carteRdv, "Rendez-vous de la période");
        JTable table = new JTable(parMedecin);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        JPanel carteMedecins = new JPanel(new BorderLayout());
        carteMedecins.add(new JScrollPane(table), BorderLayout.CENTER);
        Theme.carte(carteMedecins, "Consultations par médecin");
        JPanel bas = new JPanel(new GridLayout(1, 2, 12, 0));
        bas.setOpaque(false);
        bas.setPreferredSize(new Dimension(100, 210));
        bas.add(carteRdv);
        bas.add(carteMedecins);
        add(bas, BorderLayout.SOUTH);

        Theme.harmoniser(this);
        // Après harmoniser, qui remet la police et la couleur des libellés de carte.
        for (Tuile t : new Tuile[] {consultations, patients, absences, attente}) {
            t.styler();
        }
    }

    /** À chaque affichage de la page : relit les médecins et recalcule. */
    public void rafraichir() {
        controleur.chargerMedecins();
        calculer();
    }

    private void calculer() {
        if (enCours) {
            return;
        }
        Object m = medecin.getSelectedItem();
        controleur.calculer(date(du), date(au), m instanceof medecin ? ((medecin) m).getIdmedecin() : null);
    }

    private void datesChangees() {
        if (!enCours) {
            enCours = true;
            periode.setSelectedItem(PERSONNALISEE);
            enCours = false;
            calculer();
        }
    }

    private void appliquerPeriode() {
        LocalDate[] b = ((Periode) periode.getSelectedItem()).bornes(LocalDate.now());
        du.setValue(Date.from(b[0].atStartOfDay(ZoneId.systemDefault()).toInstant()));
        au.setValue(Date.from(b[1].atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private void exporterCsv() {
        if (controleur.getDernieres() == null) {
            return;
        }
        JFileChooser choix = new JFileChooser();
        choix.setSelectedFile(new File(controleur.nomCsv()));
        if (choix.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = choix.getSelectedFile();
            if (!f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
                f = new File(f.getParentFile(), f.getName() + ".csv");
            }
            controleur.exporterCsv(f);
        }
    }

    @Override
    public void afficherMedecins(List<medecin> medecins) {
        Object avant = medecin.getSelectedItem();
        enCours = true;
        medecin.removeAllItems();
        medecin.addItem(TOUS);
        for (medecin m : medecins) {
            medecin.addItem(m);
            if (avant instanceof medecin && ((medecin) avant).getIdmedecin() == m.getIdmedecin()) {
                medecin.setSelectedItem(m);
            }
        }
        enCours = false;
    }

    @Override
    public void afficherStatistiques(Statistiques s, List<StatistiquesService.Barre> serie,
            Map<Integer, medecin> medecins) {
        consultations.setValeur(String.valueOf(s.getConsultations()), StatistiquesService.libellePeriode(s));
        patients.setValeur(String.valueOf(s.getPatientsDistincts()), s.getNouveauxPatients() + " nouveau"
                + (s.getNouveauxPatients() > 1 ? "x" : "") + " patient" + (s.getNouveauxPatients() > 1 ? "s" : ""));
        int absents = s.rendezVous(Rendez_Vous.ETAT_ABSENT);
        absences.setValeur(StatistiquesService.pourcentage(s.getTauxAbsence()),
                absents + " absence" + (absents > 1 ? "s" : "") + " sur " + (absents + s.getRendezVousHonores())
                + " rendez-vous passés");
        Statistiques.SalleAttente salle = s.getSalle();
        attente.setValeur(StatistiquesService.minutes(salle.getAttenteMoyenne()), salle.getPassages()
                + " passage" + (salle.getPassages() > 1 ? "s" : "") + ", " + salle.getSansRendezVous() + " sans RDV, "
                + salle.getUrgences() + " urgence" + (salle.getUrgences() > 1 ? "s" : ""));

        titreGraphique.setText("Nombre de consultations par " + StatistiquesService.grain(s) + " · survolez une barre pour le détail");
        graphique.setBarres(serie, "consultations");

        List<Object[]> lignes = new ArrayList<>();
        lignes.add(new Object[] {"Honorés", s.getRendezVousHonores()});
        lignes.add(new Object[] {"Absents", absents});
        lignes.add(new Object[] {"Annulés", s.rendezVous(Rendez_Vous.ETAT_ANNULE)});
        lignes.add(new Object[] {"Encore prévus", s.rendezVous(Rendez_Vous.ETAT_PREVU)});
        rendezVous.setLignes(lignes);

        parMedecin.setRowCount(0);
        for (Map.Entry<Integer, Integer> e : s.getConsultationsParMedecin().entrySet()) {
            medecin m = medecins.get(e.getKey());
            String nom = m == null ? "Médecin n° " + e.getKey() : m + (m.isActif() ? "" : " (parti)");
            parMedecin.addRow(new Object[] {nom, e.getValue()});
        }
    }

    @Override
    public void afficherErreur(String message) {
        Dialogues.erreur(this, message);
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }

    // ----- composants -----

    private static JLabel libelle(String texte) {
        JLabel l = new JLabel(texte);
        l.setForeground(Theme.TEXTE_SECONDAIRE);
        return l;
    }

    private static JSpinner dateSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        return s;
    }

    private static LocalDate date(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Chiffre clé : intitulé, grand nombre, ligne d'explication. */
    static final class Tuile extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel titre = new JLabel();
        private final JLabel valeur = new JLabel("–");
        private final JLabel detail = new JLabel(" ");

        Tuile(String titre) {
            super(new BorderLayout(0, 2));
            add(titre(titre), BorderLayout.NORTH);
            add(valeur, BorderLayout.CENTER);
            add(detail, BorderLayout.SOUTH);
            Theme.carte(this, null);
        }

        private JLabel titre(String texte) {
            titre.setText(texte);
            return titre;
        }

        void styler() {
            titre.setForeground(Theme.TEXTE_SECONDAIRE);
            titre.setFont(Theme.police(Font.PLAIN, 12f));
            valeur.setFont(Theme.police(Font.BOLD, 28f));
            valeur.setForeground(Theme.TEXTE);
            detail.setForeground(Theme.TEXTE_SECONDAIRE);
            detail.setFont(Theme.police(Font.PLAIN, 12f));
        }

        void setValeur(String v, String d) {
            valeur.setText(v);
            detail.setText(d);
        }

        String getValeur() {
            return valeur.getText();
        }
    }

    /** Barres horizontales d'une seule série : libellé, barre, nombre. */
    static final class Repartition extends JComponent {
        private static final long serialVersionUID = 1L;
        private List<Object[]> lignes = new ArrayList<>();

        void setLignes(List<Object[]> lignes) {
            this.lignes = lignes;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(Theme.police());
            FontMetrics fm = g.getFontMetrics();
            int max = 1;
            int etiquette = 0;
            for (Object[] l : lignes) {
                max = Math.max(max, (Integer) l[1]);
                etiquette = Math.max(etiquette, fm.stringWidth((String) l[0]));
            }
            int x0 = etiquette + 16;
            int largeur = Math.max(10, getWidth() - x0 - 50);
            int hauteurLigne = Math.max(22, Math.min(34, getHeight() / Math.max(1, lignes.size())));
            int y = 6;
            for (Object[] l : lignes) {
                int n = (Integer) l[1];
                g.setColor(Theme.TEXTE);
                g.drawString((String) l[0], 4, y + hauteurLigne / 2 + fm.getAscent() / 2 - 2);
                int w = n == 0 ? 0 : Math.max(3, (int) Math.round((double) n / max * largeur));
                g.setColor(Theme.PRIMAIRE);
                g.fillRoundRect(x0, y + hauteurLigne / 2 - 6, w, 12, 4, 4);
                g.setColor(Theme.TEXTE);
                g.drawString(String.valueOf(n), x0 + w + 8, y + hauteurLigne / 2 + fm.getAscent() / 2 - 2);
                y += hauteurLigne;
            }
            g.dispose();
        }
    }
}
