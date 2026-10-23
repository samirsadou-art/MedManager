package My_application.Patient;

import My_application.entities.patient;
import My_application.service.PatientService;
import My_application.vue.EnTetePage;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Détails d'un patient de la page « Patients », en lecture seule : état civil
 * et coordonnées. « Modifier la fiche » ouvre {@link FichePatient} et la
 * fenêtre se met à jour après l'enregistrement.
 */
public class DetailsPatient extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final transient PatientService patients;
    private final JPanel cartes = new JPanel();
    private final JButton modifierBtn = new JButton("Modifier la fiche", Icones.svg("pencil", 16, Theme.PRIMAIRE));
    private transient patient affiche;
    private transient patient modifie;

    public DetailsPatient(Window parent, PatientService patients, patient p) {
        super(parent, "Détails du patient", ModalityType.APPLICATION_MODAL);
        this.patients = patients;
        this.affiche = p;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        cartes.setLayout(new BoxLayout(cartes, BoxLayout.Y_AXIS));
        cartes.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        cartes.setBackground(Theme.FOND);
        remplir();

        modifierBtn.addActionListener(e -> modifier());
        JButton fermer = new JButton("Fermer");
        fermer.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(fermer);
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        boutons.setBackground(Theme.FOND);
        boutons.add(modifierBtn);
        boutons.add(fermer);

        JPanel contenu = new JPanel(new BorderLayout());
        contenu.setBackground(Theme.FOND);
        contenu.add(cartes, BorderLayout.CENTER);
        contenu.add(boutons, BorderLayout.SOUTH);
        setContentPane(contenu);

        Theme.harmoniser(this);
        EnTetePage.installer(this, "user", "Détails du patient", "État civil et coordonnées");
        pack();
        setLocationRelativeTo(parent);
    }

    /** Patient enregistré par « Modifier la fiche », null s'il n'a pas été modifié. */
    public patient getPatientModifie() {
        return modifie;
    }

    /** Pour les tests. */
    JButton boutonModifier() {
        return modifierBtn;
    }

    /** Pour les tests : patient affiché. */
    patient patientAffiche() {
        return affiche;
    }

    private void modifier() {
        FichePatient fiche = new FichePatient(this, patients, affiche);
        fiche.setVisible(true);
        if (fiche.getPatient() != null) {
            affiche = fiche.getPatient();
            modifie = affiche;
            remplir();
            Theme.harmoniser(cartes);
            pack();
        }
    }

    private void remplir() {
        patient p = affiche;
        cartes.removeAll();

        JLabel nom = new JLabel(p.getNom().toUpperCase(Locale.FRENCH) + " " + p.getPrenom(),
                Icones.patient(p, 56), JLabel.LEFT);
        nom.setIconTextGap(14);
        nom.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE + 4));
        nom.setForeground(Theme.TEXTE);
        nom.setBorder(BorderFactory.createEmptyBorder(0, 4, 12, 0));
        nom.setAlignmentX(LEFT_ALIGNMENT);
        cartes.add(nom);

        JPanel etatCivil = grille();
        ligne(etatCivil, 0, "N° de dossier", p.getNumeroDossier(), "Âge", p.getDat_nais() == null ? "" : p.getStringAge());
        ligne(etatCivil, 1, "Date de naissance", p.getDat_nais() == null ? "" : p.getDat_nais().format(DATE),
                "Sexe", p.getSexe() == null ? "" : p.isMasculin() ? "Masculin" : "Féminin");
        ligne(etatCivil, 2, "Situation familiale", p.getSituatMarit(), "N° de sécurité sociale", p.getNum_soc());
        Theme.carte(etatCivil, "État civil");
        etatCivil.setAlignmentX(LEFT_ALIGNMENT);
        cartes.add(etatCivil);
        cartes.add(Box.createVerticalStrut(12));

        JPanel coordonnees = grille();
        ligne(coordonnees, 0, "Téléphone", p.getTelephone(), "E-mail", p.getEmail());
        ligne(coordonnees, 1, "Adresse", adresse(p), null, null);
        ligne(coordonnees, 2, "Wilaya", p.getWilaya(), "Pays", p.getPays());
        Theme.carte(coordonnees, "Coordonnées");
        coordonnees.setAlignmentX(LEFT_ALIGNMENT);
        cartes.add(coordonnees);

        cartes.revalidate();
        cartes.repaint();
    }

    /** « Localité, Ville » sans les parties vides ni répétées (la wilaya a sa propre ligne). */
    static String adresse(patient p) {
        List<String> parties = new ArrayList<>();
        for (String s : new String[]{p.getAdresse(), p.getLocalite(), p.getVille()}) {
            if (s != null && !s.trim().isEmpty() && !parties.contains(s.trim())) {
                parties.add(s.trim());
            }
        }
        return String.join(", ", parties);
    }

    private static JPanel grille() {
        return new JPanel(new GridBagLayout());
    }

    private static GridBagConstraints contraintes(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(6, x % 2 == 0 ? 4 : 8, 6, x % 2 == 0 ? 4 : 24);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = x % 2 == 0 ? 0 : 1;
        return c;
    }

    private static void ligne(JPanel grille, int y, String l1, String v1, String l2, String v2) {
        GridBagConstraints c = contraintes(1, y);
        if (l2 == null) {
            c.gridwidth = 3;
        }
        grille.add(libelle(l1), contraintes(0, y));
        grille.add(valeur(v1), c);
        if (l2 != null) {
            grille.add(libelle(l2), contraintes(2, y));
            grille.add(valeur(v2), contraintes(3, y));
        }
    }

    private static JLabel libelle(String texte) {
        JLabel l = new JLabel(texte);
        l.setFont(Theme.police());
        l.setForeground(Theme.TEXTE_SECONDAIRE);
        return l;
    }

    private static JLabel valeur(String texte) {
        JLabel l = new JLabel(texte == null || texte.trim().isEmpty() ? "—" : texte.trim());
        l.setFont(Theme.policeGras());
        l.setForeground(Theme.TEXTE);
        return l;
    }
}
