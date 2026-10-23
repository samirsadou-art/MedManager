package My_application.Ordonnance;

import My_application.controller.CatalogueController;
import My_application.controller.Vue;
import My_application.entities.consultation;
import My_application.entities.medicament;
import My_application.entities.ordonnance;
import My_application.service.CatalogueService;
import My_application.service.Posologie;
import My_application.vue.AutoCompletion;
import My_application.vue.Dialogues;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;

/**
 * Saisie de l'ordonnance de la consultation en cours, directement dans
 * l'onglet « Ordonnance » du dossier (elle remplace la fenêtre
 * {@link OrdonnanceForm} pour cette consultation).
 * <p>
 * En haut, la ligne à ajouter : médicament (recherche dans le référentiel),
 * unités par prise, fois par jour, durée et moment, avec la quantité totale
 * et une alerte quand une allergie du patient concerne le médicament. En
 * dessous, les lignes de l'ordonnance (double-clic : modifier, Suppr :
 * retirer) et « Reprendre l'ordonnance du … ». Les changements vont
 * directement dans l'ordonnance de la consultation, enregistrée avec elle.
 */
public class SaisieOrdonnance extends JPanel implements Vue {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    static final String AJOUTER = "Ajouter à l'ordonnance";
    static final String METTRE_A_JOUR = "Mettre à jour";

    private final ordonnance ordo;
    private final consultation precedente;
    private final Supplier<List<String>> allergies;
    private final Runnable changement;
    private final CatalogueController controleur;
    private final CatalogueService catalogue;

    private final DefaultComboBoxModel<Object> suggestions = new DefaultComboBoxModel<>();
    private final JComboBox<Object> choix = new JComboBox<>();
    private final JSpinner prise = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JSpinner fois = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    private final JSpinner duree = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
    private final JToggleButton matin = moment("Matin");
    private final JToggleButton soir = moment("Soir");
    private final JToggleButton indifferent = moment("Indifférent");
    private final JLabel besoin = new JLabel(" ");
    private final JLabel alerte = new JLabel();
    private final JButton ajouter = new JButton(AJOUTER);
    private final JButton annulerModif = new JButton("Annuler");
    private final Lignes modele = new Lignes();
    private final JTable table = new JTable(modele);
    private final JButton modifier = new JButton("Modifier");
    private final JButton retirer = new JButton("Retirer");
    private final JButton toutRetirer = new JButton("Tout retirer");
    private final JButton reprendre = new JButton();
    private final JLabel resume = new JLabel();

    /** Ligne en cours de modification, -1 pendant un ajout. */
    private int enEdition = -1;

    /** Crée un médicament absent du référentiel (fenêtre « Nouveau médicament ») ; null si abandon. */
    Function<String, medicament> creation;
    /** Pose une question oui / non au médecin. */
    Predicate<String> confirmation = q -> Dialogues.confirmer(this, q);

    /**
     * @param precedente consultation dont l'ordonnance peut être reprise, ou null
     * @param allergies  allergies notées dans le dossier (relues à chaque saisie)
     * @param changement appelé après chaque changement de l'ordonnance
     */
    public SaisieOrdonnance(ordonnance ordo, CatalogueService catalogue, consultation precedente,
            Supplier<List<String>> allergies, Runnable changement) {
        super(new BorderLayout(0, 12));
        this.ordo = ordo;
        this.catalogue = catalogue;
        this.precedente = precedente;
        this.allergies = allergies;
        this.changement = changement;
        this.controleur = new CatalogueController(this, catalogue);
        this.creation = this::creerDansLeReferentiel;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(panneauSaisie(), BorderLayout.NORTH);
        add(panneauLignes(), BorderLayout.CENTER);
        indifferent.setSelected(true);
        majBesoin();
        majLignes();
    }

    @Override
    public void afficherErreur(String message) {
        Dialogues.erreur(this, message);
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }

    /** Place le curseur dans le champ Médicament (bouton « Ordonnance » du dossier). */
    public void commencer() {
        choix.requestFocusInWindow();
    }

    // ----- la ligne à ajouter -----

    private JComponent panneauSaisie() {
        choix.setEditable(true);
        choix.putClientProperty("JTextField.placeholderText", "Rechercher un médicament (nom, dosage, forme)…");
        choix.putClientProperty("JTextField.leadingIcon", Icones.svg("search", 16, Theme.TEXTE_SECONDAIRE));
        AutoCompletion.installer(choix, suggestions, true, new AutoCompletion.Source<medicament>() {
            @Override
            public List<medicament> suggestions(String prefixe) {
                return controleur.rechercherMedicaments(prefixe);
            }
        });
        editeur().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                majAlerte();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                majAlerte();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                majAlerte();
            }
        });
        editeur().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !choix.isPopupVisible()) {
                    valider();
                }
            }
        });
        for (JSpinner s : new JSpinner[] {prise, fois, duree}) {
            s.addChangeListener(e -> majBesoin());
            ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(3);
        }
        ButtonGroup groupe = new ButtonGroup();
        groupe.add(matin);
        groupe.add(soir);
        groupe.add(indifferent);

        JPanel champs = new JPanel(new GridBagLayout());
        champs.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, 4, 12);
        g.weightx = 1;
        champs.add(libelle("Médicament"), g);
        g.weightx = 0;
        champs.add(libelle("Unités par prise"), g);
        champs.add(libelle("Fois par jour"), g);
        g.insets = new Insets(0, 0, 4, 0);
        champs.add(libelle("Durée (jours)"), g);
        g.gridy = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);
        g.weightx = 1;
        champs.add(choix, g);
        g.weightx = 0;
        champs.add(prise, g);
        champs.add(fois, g);
        g.insets = new Insets(0, 0, 0, 0);
        champs.add(duree, g);

        JPanel moments = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        moments.setOpaque(false);
        moments.add(libelle("Moment"));
        moments.add(matin);
        moments.add(soir);
        moments.add(indifferent);
        besoin.setFont(Theme.police());
        besoin.setForeground(Theme.TEXTE_SECONDAIRE);
        besoin.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        moments.add(besoin);

        principal(ajouter, "plus");
        ajouter.addActionListener(e -> valider());
        annulerModif.setToolTipText("Abandonner la modification de la ligne");
        annulerModif.addActionListener(e -> vider());
        annulerModif.setVisible(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(annulerModif);
        actions.add(ajouter);
        JPanel ligne2 = new JPanel(new BorderLayout());
        ligne2.setOpaque(false);
        ligne2.add(moments, BorderLayout.CENTER);
        ligne2.add(actions, BorderLayout.EAST);

        alerte.setIcon(Icones.svg("first-aid-kit", 16, Theme.DANGER));
        alerte.setIconTextGap(8);
        alerte.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE));
        alerte.setForeground(Theme.DANGER);
        alerte.setOpaque(true);
        alerte.setBackground(Theme.DANGER_CLAIR);
        alerte.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xF2B8B8)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        alerte.setVisible(false);

        JPanel bas = new JPanel(new BorderLayout(0, 10));
        bas.setOpaque(false);
        bas.add(ligne2, BorderLayout.NORTH);
        bas.add(alerte, BorderLayout.CENTER);

        JPanel panneau = new JPanel(new BorderLayout(0, 10));
        panneau.setBackground(new Color(0xF3F9FA));
        panneau.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCFE3E6)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        panneau.add(champs, BorderLayout.NORTH);
        panneau.add(bas, BorderLayout.CENTER);
        return panneau;
    }

    private static JLabel libelle(String texte) {
        JLabel l = new JLabel(texte);
        l.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE));
        l.setForeground(Theme.TEXTE);
        return l;
    }

    private static JToggleButton moment(String texte) {
        JToggleButton b = new JToggleButton(texte);
        b.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE));
        b.setFocusable(true);
        // Moment choisi plein, dans la couleur principale.
        b.putClientProperty("FlatLaf.style", "selectedBackground: #0E7C86; selectedForeground: #FFFFFF;");
        return b;
    }

    private static void principal(JButton b, String icone) {
        b.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE));
        b.setBackground(Theme.PRIMAIRE);
        b.setForeground(Color.WHITE);
        b.setIcon(Icones.svg(icone, 16, Color.WHITE));
        b.setIconTextGap(8);
    }

    private static void secondaire(JButton b, String icone, Color couleur) {
        b.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE));
        b.setIcon(Icones.svg(icone, 16, couleur));
        b.setIconTextGap(8);
        if (couleur == Theme.DANGER) {
            b.setForeground(Theme.DANGER);
        }
    }

    private JTextComponent editeur() {
        return (JTextComponent) choix.getEditor().getEditorComponent();
    }

    int mode() {
        return matin.isSelected() ? medicament.MODE_MATIN
                : soir.isSelected() ? medicament.MODE_SOIR : medicament.MODE_INDIFFERENT;
    }

    private void majBesoin() {
        String b = Posologie.besoin(valeur(prise), valeur(fois), valeur(duree));
        besoin.setText(b.isEmpty() ? " " : b);
    }

    private static int valeur(JSpinner s) {
        return ((Number) s.getValue()).intValue();
    }

    private void majAlerte() {
        List<String> concernees = Posologie.allergiesConcernees(AutoCompletion.saisie(choix), allergies());
        alerte.setVisible(!concernees.isEmpty());
        alerte.setText(concernees.isEmpty() ? "" : "Allergie notée dans le dossier : "
                + String.join(", ", concernees) + ". Vérifiez avant de prescrire ce médicament.");
    }

    private List<String> allergies() {
        List<String> a = allergies == null ? null : allergies.get();
        return a == null ? Collections.<String>emptyList() : a;
    }

    /** « Ajouter à l'ordonnance » ou « Mettre à jour » selon la ligne en cours. */
    void valider() {
        if (enEdition >= 0) {
            ecrire(ordo.getMedicament(enEdition));
            vider();
            majLignes();
            return;
        }
        String saisie = AutoCompletion.saisie(choix);
        if (saisie.isEmpty()) {
            choix.requestFocusInWindow();
            return;
        }
        medicament choisi = AutoCompletion.selection(choix, medicament.class);
        if (choisi == null) {
            if (!confirmation.test("« " + saisie + " » n'est pas dans la liste des médicaments.\n"
                    + "L'ajouter à la liste ?")) {
                return;
            }
            choisi = creation.apply(saisie);
            if (choisi == null) {
                return;
            }
        }
        ajouter(choisi);
    }

    /** Ajoute le médicament du référentiel avec la posologie saisie ; false s'il n'est pas ajouté. */
    boolean ajouter(medicament choisi) {
        List<String> concernees = Posologie.allergiesConcernees(choisi.getDesignmed(), allergies());
        if (!concernees.isEmpty() && !confirmation.test("Le patient est allergique : "
                + String.join(", ", concernees) + ".\nPrescrire quand même « " + choisi.getDesignmed() + " » ?")) {
            return false;
        }
        medicament m = new medicament();
        m.setIdmed(choisi.getIdmed());
        m.setDesignmed(choisi.getDesignmed());
        ecrire(m);
        if (!ordo.ajouter(m)) {
            afficherErreur("Ce médicament figure déjà sur l'ordonnance.");
            return false;
        }
        vider();
        majLignes();
        return true;
    }

    private void ecrire(medicament m) {
        m.setUnite(valeur(prise));
        m.setPosologie(valeur(fois));
        m.setDuree(valeur(duree));
        m.setMode(mode());
    }

    private medicament creerDansLeReferentiel(String nom) {
        AddMedicamentForm form = new AddMedicamentForm(catalogue);
        form.setNomMED(nom);
        form.setVisible(true);
        return form.getMedicamentCree();
    }

    /** Remet la saisie à zéro (après un ajout, ou pour abandonner une modification). */
    void vider() {
        enEdition = -1;
        choix.setEnabled(true);
        suggestions.removeAllElements();
        choix.setSelectedItem(null);
        editeur().setText("");
        prise.setValue(1);
        fois.setValue(1);
        duree.setValue(0);
        indifferent.setSelected(true);
        ajouter.setText(AJOUTER);
        ajouter.setIcon(Icones.svg("plus", 16, Color.WHITE));
        annulerModif.setVisible(false);
        alerte.setVisible(false);
        table.clearSelection();
        choix.requestFocusInWindow();
    }

    /** Recopie la ligne dans la saisie ; « Mettre à jour » l'enregistre. */
    void modifier(int index) {
        if (index < 0 || index >= ordo.getNombreMedicament()) {
            return;
        }
        medicament m = ordo.getMedicament(index);
        enEdition = index;
        editeur().setText(m.getDesignmed());
        choix.setEnabled(false);
        prise.setValue(Math.max(1, Math.min(20, m.getUnite())));
        fois.setValue(Math.max(1, Math.min(10, m.getPosologie())));
        duree.setValue(Math.max(0, Math.min(365, m.getDuree())));
        if (m.getMode() == medicament.MODE_MATIN) {
            matin.setSelected(true);
        } else if (m.getMode() == medicament.MODE_SOIR) {
            soir.setSelected(true);
        } else {
            indifferent.setSelected(true);
        }
        ajouter.setText(METTRE_A_JOUR);
        ajouter.setIcon(Icones.svg("check", 16, Color.WHITE));
        annulerModif.setVisible(true);
        majAlerte();
    }

    // ----- les lignes de l'ordonnance -----

    private JComponent panneauLignes() {
        table.setRowHeight(34);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setMaxWidth(46);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getSelectionModel().addListSelectionListener(e -> majBoutons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    modifier(table.getSelectedRow());
                }
            }
        });
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    retirer(table.getSelectedRow());
                }
            }
        });
        JScrollPane defile = new JScrollPane(table);
        defile.setBorder(BorderFactory.createLineBorder(Theme.BORDURE));
        defile.setPreferredSize(new java.awt.Dimension(400, 150));

        secondaire(modifier, "pencil", Theme.PRIMAIRE);
        modifier.setToolTipText("Modifier la ligne choisie (ou double-clic)");
        modifier.addActionListener(e -> modifier(table.getSelectedRow()));
        secondaire(retirer, "trash", Theme.DANGER);
        retirer.setToolTipText("Retirer la ligne choisie (ou touche Suppr)");
        retirer.addActionListener(e -> retirer(table.getSelectedRow()));
        secondaire(toutRetirer, "x", Theme.TEXTE_SECONDAIRE);
        toutRetirer.addActionListener(e -> {
            if (confirmation.test("Retirer tous les médicaments de l'ordonnance ?")) {
                toutRetirer();
            }
        });
        secondaire(reprendre, "refresh", Theme.PRIMAIRE);
        reprendre.setVisible(precedente != null);
        if (precedente != null) {
            reprendre.setText("Reprendre l'ordonnance du " + precedente.getDate_cons().format(JOUR));
            reprendre.setToolTipText("Ajouter les médicaments de cette ordonnance (déjà présents : ignorés)");
        }
        reprendre.addActionListener(e -> reprendre());
        resume.setFont(Theme.police());
        resume.setForeground(Theme.TEXTE_SECONDAIRE);

        JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        gauche.setOpaque(false);
        gauche.add(reprendre);
        gauche.add(toutRetirer);
        gauche.add(resume);
        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        droite.setOpaque(false);
        droite.add(modifier);
        droite.add(retirer);
        JPanel boutons = new JPanel(new BorderLayout());
        boutons.setOpaque(false);
        boutons.add(gauche, BorderLayout.CENTER);
        boutons.add(droite, BorderLayout.EAST);

        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.add(defile, BorderLayout.CENTER);
        p.add(boutons, BorderLayout.SOUTH);
        return p;
    }

    void retirer(int index) {
        if (index < 0 || index >= ordo.getNombreMedicament()) {
            return;
        }
        if (index == enEdition) {
            vider();
        } else if (enEdition > index) {
            enEdition--;
        }
        ordo.retirer(index);
        majLignes();
    }

    /** Vide l'ordonnance (bouton « Supprimer » du dossier ou « Tout retirer »). */
    public void toutRetirer() {
        vider();
        ordo.setMedicaments(Collections.<medicament>emptyList());
        majLignes();
    }

    /** Ajoute les médicaments de l'ordonnance précédente qui n'y sont pas encore ; renvoie leur nombre. */
    int reprendre() {
        if (precedente == null) {
            return 0;
        }
        int n = 0;
        for (medicament m : precedente.getOrdoPat().getMedicaments()) {
            if (ordo.ajouter(new medicament(m))) {
                n++;
            }
        }
        majLignes();
        if (n == 0) {
            afficherInformation("Ces médicaments figurent déjà sur l'ordonnance.");
        }
        return n;
    }

    private void majLignes() {
        modele.fireTableDataChanged();
        int n = ordo.getNombreMedicament();
        resume.setText(n == 0 ? "Aucun médicament" : n + (n == 1 ? " médicament" : " médicaments"));
        toutRetirer.setEnabled(n > 0);
        majBoutons();
        if (changement != null) {
            changement.run();
        }
    }

    private void majBoutons() {
        boolean choisie = table.getSelectedRow() >= 0;
        modifier.setEnabled(choisie);
        retirer.setEnabled(choisie);
    }

    // ----- accès pour les tests -----

    JComboBox<Object> champMedicament() {
        return choix;
    }

    AbstractTableModel modele() {
        return modele;
    }

    JSpinner champPrise() {
        return prise;
    }

    JSpinner champFois() {
        return fois;
    }

    JSpinner champDuree() {
        return duree;
    }

    JToggleButton boutonMatin() {
        return matin;
    }

    JButton boutonAjouter() {
        return ajouter;
    }

    JButton boutonReprendre() {
        return reprendre;
    }

    JLabel alerte() {
        return alerte;
    }

    JLabel besoin() {
        return besoin;
    }

    /** Les lignes : N°, médicament, posologie, durée. */
    private final class Lignes extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] colonnes = {"N°", "Médicament", "Posologie", "Durée"};

        @Override
        public int getRowCount() {
            return ordo.getNombreMedicament();
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
        public Object getValueAt(int ligne, int colonne) {
            medicament m = ordo.getMedicament(ligne);
            switch (colonne) {
                case 0:
                    return String.valueOf(ligne + 1);
                case 1:
                    return m.getDesignmed();
                case 2:
                    return Posologie.libelle(m);
                default:
                    String d = Posologie.duree(m.getDuree());
                    return d.isEmpty() ? "—" : d;
            }
        }
    }

    /** Copie des lignes (pour les tests). */
    List<medicament> lignes() {
        return new ArrayList<>(ordo.getMedicaments());
    }
}
