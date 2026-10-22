package My_application.Patient;

import My_application.controller.FichePatientController;
import My_application.controller.FichePatientVue;
import My_application.entities.patient;
import My_application.service.PatientService;
import My_application.vue.Dialogues;
import My_application.vue.EnTetePage;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Fiche administrative d'un patient (Vue) : inscription par la secrétaire ou
 * modification de l'état civil et des coordonnées. Le dossier médical n'est
 * jamais montré ici.
 * <p>
 * Remplace l'ancien formulaire {@code NewPatient} (généré par NetBeans, sans
 * téléphone ni adresse). Après une inscription, propose d'enchaîner sur la
 * prise de rendez-vous ({@link #getSuite()}).
 */
public class FichePatient extends JDialog implements FichePatientVue {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter NAISSANCE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    /** Ce que l'utilisateur veut faire après l'inscription. */
    public enum Suite {
        AUCUNE,
        RENDEZ_VOUS
    }

    /** Wilayas d'Algérie, dans l'ordre de leur code. */
    static final String[] WILAYAS = {"", "Adrar", "Chlef", "Laghouat", "Oum El Bouaghi", "Batna", "Béjaïa",
        "Biskra", "Béchar", "Blida", "Bouira", "Tamanrasset", "Tébessa", "Tlemcen", "Tiaret", "Tizi Ouzou",
        "Alger", "Djelfa", "Jijel", "Sétif", "Saïda", "Skikda", "Sidi Bel Abbès", "Annaba", "Guelma",
        "Constantine", "Médéa", "Mostaganem", "M'Sila", "Mascara", "Ouargla", "Oran", "El Bayadh", "Illizi",
        "Bordj Bou Arréridj", "Boumerdès", "El Tarf", "Tindouf", "Tissemsilt", "El Oued", "Khenchela",
        "Souk Ahras", "Tipaza", "Mila", "Aïn Defla", "Naâma", "Aïn Témouchent", "Ghardaïa", "Relizane",
        "Timimoun", "Bordj Badji Mokhtar", "Ouled Djellal", "Béni Abbès", "In Salah", "In Guezzam",
        "Touggourt", "Djanet", "El M'Ghair", "El Meniaa"};

    private final transient FichePatientController controleur;
    /** Patient modifié, ou null pour une inscription. */
    private final transient patient original;

    private final JTextField numero = new JTextField(8);
    private final JTextField nom = new JTextField(18);
    private final JTextField prenom = new JTextField(18);
    private final JTextField naissance = new JTextField(10);
    private final JComboBox<String> sexe = new JComboBox<>(new String[]{"Masculin", "Féminin"});
    private final JComboBox<String> situation = new JComboBox<>(
            new String[]{"Célibataire", "Marié(e)", "Divorcé(e)", "Veuf (veuve)"});
    private final JTextField securiteSociale = new JTextField(14);
    private final JTextField telephone = new JTextField(14);
    private final JTextField email = new JTextField(18);
    private final JTextField adresse = new JTextField(28);
    private final JTextField ville = new JTextField(14);
    private final JComboBox<String> wilaya = new JComboBox<>(WILAYAS);
    private final JTextField pays = new JTextField("Algérie", 12);

    private transient patient resultat;
    private boolean existant;
    private Suite suite = Suite.AUCUNE;

    /** Inscription d'un nouveau patient. */
    public FichePatient(Window parent, PatientService patientService) {
        this(parent, patientService, null);
    }

    /** Modification de la fiche de {@code p} (null : inscription). */
    public FichePatient(Window parent, PatientService patientService, patient p) {
        super(parent, p == null ? "Nouveau patient" : "Fiche patient", ModalityType.APPLICATION_MODAL);
        this.original = p;
        controleur = new FichePatientController(this, patientService);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construire();
        Theme.harmoniser(this);
        EnTetePage.installer(this, p == null ? "user-plus" : "pencil",
                p == null ? "Nouveau patient" : "Fiche de " + p,
                p == null ? "Inscrivez le patient : état civil et coordonnées (* obligatoire)"
                        : "État civil et coordonnées ; le dossier médical n'est pas modifié ici");
        if (p != null) {
            remplir(FichePatientController.Saisie.depuis(p));
            numero.setEditable(false);
        }
        pack();
        setLocationRelativeTo(parent);
    }

    private void construire() {
        wilaya.setEditable(true);
        naissance.setToolTipText("jj/mm/aaaa");
        telephone.setToolTipText("Mobile 05, 06 ou 07 (10 chiffres) ou fixe (9 chiffres)");
        numero.setToolTipText("Laisser vide : numéro attribué automatiquement");

        JPanel etatCivil = grille();
        ligne(etatCivil, 0, "Nom *", nom, "Prénom *", prenom);
        ligne(etatCivil, 1, "Date de naissance *", naissance, "Sexe *", sexe);
        ligne(etatCivil, 2, "Situation familiale", situation, "N° de sécurité sociale", securiteSociale);
        ligne(etatCivil, 3, "N° de dossier", numero, null, null);
        Theme.carte(etatCivil, "État civil");

        JPanel coordonnees = grille();
        ligne(coordonnees, 0, "Téléphone *", telephone, "E-mail", email);
        GridBagConstraints c = contraintes(1, 1);
        c.gridwidth = 3;
        coordonnees.add(etiquette("Adresse"), contraintes(0, 1));
        coordonnees.add(adresse, c);
        ligne(coordonnees, 2, "Ville / commune", ville, "Wilaya", wilaya);
        ligne(coordonnees, 3, "Pays", pays, null, null);
        Theme.carte(coordonnees, "Coordonnées");

        JPanel cartes = new JPanel();
        cartes.setLayout(new javax.swing.BoxLayout(cartes, javax.swing.BoxLayout.Y_AXIS));
        cartes.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        cartes.setBackground(Theme.FOND);
        cartes.add(etatCivil);
        cartes.add(javax.swing.Box.createVerticalStrut(12));
        cartes.add(coordonnees);

        JButton annuler = new JButton("Annuler");
        annuler.addActionListener(e -> dispose());
        JButton enregistrer = new JButton("Enregistrer");
        enregistrer.addActionListener(e -> enregistrer());
        getRootPane().setDefaultButton(enregistrer);
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        boutons.setBackground(Theme.FOND);
        boutons.add(annuler);
        boutons.add(enregistrer);

        JPanel contenu = new JPanel(new BorderLayout());
        contenu.setBackground(Theme.FOND);
        contenu.add(cartes, BorderLayout.CENTER);
        contenu.add(boutons, BorderLayout.SOUTH);
        setContentPane(contenu);
    }

    private static JPanel grille() {
        JPanel p = new JPanel(new GridBagLayout());
        return p;
    }

    private static GridBagConstraints contraintes(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(6, x % 2 == 0 ? 4 : 8, 6, x % 2 == 0 ? 4 : 20);
        c.anchor = GridBagConstraints.WEST;
        c.fill = x % 2 == 0 ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        c.weightx = x % 2 == 0 ? 0 : 1;
        return c;
    }

    private static JLabel etiquette(String texte) {
        JLabel l = new JLabel(texte);
        l.setFont(Theme.police());
        return l;
    }

    private static void ligne(JPanel grille, int y, String l1, JComponent c1, String l2, JComponent c2) {
        grille.add(etiquette(l1), contraintes(0, y));
        grille.add(c1, contraintes(1, y));
        if (l2 != null) {
            grille.add(etiquette(l2), contraintes(2, y));
            grille.add(c2, contraintes(3, y));
        }
    }

    private void remplir(FichePatientController.Saisie s) {
        numero.setText(s.numero);
        nom.setText(s.nom);
        prenom.setText(s.prenom);
        naissance.setText(s.dateNaissance);
        sexe.setSelectedIndex(s.masculin ? 0 : 1);
        if (!s.situationMaritale.isEmpty()) {
            situation.setSelectedItem(s.situationMaritale);
        }
        securiteSociale.setText(s.numeroSecuriteSociale);
        telephone.setText(s.telephone);
        email.setText(s.email);
        adresse.setText(s.adresse);
        ville.setText(s.ville);
        wilaya.setSelectedItem(s.wilaya);
        pays.setText(s.pays);
    }

    private FichePatientController.Saisie lire() {
        FichePatientController.Saisie s = new FichePatientController.Saisie();
        s.numero = numero.getText();
        s.nom = nom.getText();
        s.prenom = prenom.getText();
        s.dateNaissance = naissance.getText();
        s.masculin = sexe.getSelectedIndex() == 0;
        s.situationMaritale = String.valueOf(situation.getSelectedItem());
        s.numeroSecuriteSociale = securiteSociale.getText();
        s.telephone = telephone.getText();
        s.email = email.getText();
        s.adresse = adresse.getText();
        s.ville = ville.getText();
        Object w = wilaya.getEditor().getItem();
        s.wilaya = w == null ? "" : w.toString();
        s.pays = pays.getText();
        return s;
    }

    private void enregistrer() {
        if (original != null) {
            resultat = controleur.modifier(original, lire());
            if (resultat != null) {
                dispose();
            }
            return;
        }
        FichePatientController.Resultat r = controleur.inscrire(lire());
        if (r == null) {
            return;
        }
        resultat = r.patient;
        existant = r.existant;
        if (!existant) {
            suite = demanderSuite(r.patient);
        }
        dispose();
    }

    private Suite demanderSuite(patient p) {
        Object[] choix = {"Prendre un rendez-vous", "Terminer"};
        int reponse = JOptionPane.showOptionDialog(this,
                "Patient " + p + " inscrit (dossier n° " + p.getIdentPat() + ").\nQue voulez-vous faire ?",
                "Patient inscrit", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, choix, choix[0]);
        return reponse == 0 ? Suite.RENDEZ_VOUS : Suite.AUCUNE;
    }

    /** Patient inscrit ou modifié, fiche existante choisie, ou null si rien n'a été enregistré. */
    public patient getPatient() {
        return resultat;
    }

    /** Vrai si, à l'inscription, une fiche existante a été choisie au lieu d'en créer une. */
    public boolean isFicheExistante() {
        return existant;
    }

    public Suite getSuite() {
        return suite;
    }

    // ----- FichePatientVue -----

    @Override
    public int proposerDoublons(List<patient> doublons) {
        DefaultListModel<String> lignes = new DefaultListModel<>();
        for (patient p : doublons) {
            lignes.addElement("N° " + p.getIdentPat() + "   " + p + "   né(e) le "
                    + (p.getDat_nais() == null ? "?" : NAISSANCE.format(p.getDat_nais()))
                    + (p.getTelephone() == null ? "" : "   tél. " + p.getTelephone()));
        }
        JList<String> liste = new JList<>(lignes);
        liste.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        liste.setSelectedIndex(0);
        liste.setVisibleRowCount(Math.min(6, doublons.size()));
        JPanel message = new JPanel(new BorderLayout(0, 8));
        message.add(new JLabel("Ce patient a peut-être déjà une fiche :"), BorderLayout.NORTH);
        message.add(new JScrollPane(liste), BorderLayout.CENTER);
        Object[] choix = {"Utiliser la fiche sélectionnée", "Créer une nouvelle fiche", "Annuler"};
        int reponse = JOptionPane.showOptionDialog(this, message, "Fiche existante ?",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, choix, choix[0]);
        if (reponse == 0) {
            return Math.max(0, liste.getSelectedIndex());
        }
        return reponse == 1 ? CREER_QUAND_MEME : ANNULER;
    }

    @Override
    public void afficherErreur(String message) {
        Dialogues.erreur(this, message);
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }
}
