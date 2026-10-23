package My_application.dossier;

import My_application.entities.Passage;
import My_application.entities.patient;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Bandeau d'identité en haut du dossier patient : photo, nom, âge, date de
 * naissance, sexe, téléphone et adresse, présence en salle d'attente, et à
 * droite le numéro de dossier, de sécurité sociale et la situation familiale.
 * Il remplace le panneau « Informations du patient » de gauche.
 */
public class BandeauIdentite extends JPanel {

    private static final long serialVersionUID = 1L;
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    private final JLabel photo = new JLabel();
    private final JLabel nom = new JLabel();
    private final JPanel presence = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel details = new JPanel();
    private final JPanel numeros = new JPanel(new GridLayout(2, 3, 28, 2));

    /** @param modifier ouvre la fiche du patient (bouton crayon) */
    public BandeauIdentite(Runnable modifier) {
        super(new BorderLayout(20, 0));
        Theme.carte(this, null);
        setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        nom.setFont(Theme.police(Font.BOLD, 20f));
        nom.setForeground(Theme.TEXTE);
        JPanel ligneNom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ligneNom.setOpaque(false);
        ligneNom.add(nom);
        ligneNom.add(Box.createHorizontalStrut(14));
        presence.setOpaque(false);
        ligneNom.add(presence);
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.X_AXIS));

        JPanel centre = new JPanel();
        centre.setOpaque(false);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.add(Box.createVerticalGlue());
        ligneNom.setAlignmentX(LEFT_ALIGNMENT);
        details.setAlignmentX(LEFT_ALIGNMENT);
        centre.add(ligneNom);
        centre.add(Box.createVerticalStrut(8));
        centre.add(details);
        centre.add(Box.createVerticalGlue());

        numeros.setBackground(Theme.LIGNE_ALTERNEE);
        numeros.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDURE), BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        JButton crayon = new JButton(Icones.svg("pencil", 18, Theme.PRIMAIRE));
        crayon.setToolTipText("Modifier la fiche du patient");
        crayon.addActionListener(e -> modifier.run());
        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        droite.setOpaque(false);
        droite.add(numeros);
        droite.add(crayon);
        JPanel droiteCentree = new JPanel(new java.awt.GridBagLayout());
        droiteCentree.setOpaque(false);
        droiteCentree.add(droite);

        add(photo, BorderLayout.WEST);
        add(centre, BorderLayout.CENTER);
        add(droiteCentree, BorderLayout.EAST);
    }

    /** @param enCours passage du jour si le patient est au cabinet, sinon null */
    public void afficher(patient p, Passage enCours) {
        photo.setIcon(Icones.patient(p, 72));
        nom.setText(p.getNom().toUpperCase(Locale.FRENCH) + " " + p.getPrenom());

        presence.removeAll();
        if (enCours != null) {
            presence.add(new Etiquette(textePresence(enCours), Theme.AVERTISSEMENT, Theme.AVERTISSEMENT_CLAIR));
        }

        details.removeAll();
        info("user", p.getStringAge());
        if (p.getDat_nais() != null) {
            info("calendar", p.getDat_nais().format(DATE));
        }
        info(p.isMasculin() ? "gender-male" : "gender-female", p.isMasculin() ? "Masculin" : "Féminin");
        info("phone", p.getTelephone());
        info("map-pin", adresse(p));

        numeros.removeAll();
        numero("N° de dossier");
        numero("N° sécurité sociale");
        numero("Situation familiale");
        valeur(p.getNumeroDossier());
        valeur(p.getNum_soc());
        valeur(p.getSituatMarit());
        revalidate();
        repaint();
    }

    static String textePresence(Passage p) {
        switch (p.getEtat()) {
            case Passage.EN_CONSULTATION:
                return "En consultation";
            case Passage.A_REGLER:
                return "Consultation terminée, à régler";
            default:
                return "En salle d'attente depuis " + p.getArriveeLe().format(HEURE);
        }
    }

    /** « Ville, Wilaya » sans les parties vides (la localité est dans la vue d'ensemble). */
    static String adresse(patient p) {
        List<String> parties = new ArrayList<>();
        for (String s : new String[]{p.getVille(), p.getWilaya()}) {
            if (s != null && !s.trim().isEmpty() && !parties.contains(s.trim())) {
                parties.add(s.trim());
            }
        }
        return String.join(", ", parties);
    }

    private void info(String icone, String texte) {
        if (texte == null || texte.trim().isEmpty()) {
            return;
        }
        JLabel l = new JLabel(texte.trim(), Icones.svg(icone, 16, Theme.PRIMAIRE), JLabel.LEFT);
        l.setIconTextGap(6);
        l.setFont(Theme.police());
        l.setForeground(Theme.TEXTE);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 26));
        details.add(l);
    }

    private void numero(String libelle) {
        JLabel l = new JLabel(libelle);
        l.setFont(Theme.police(Font.PLAIN, Theme.TAILLE_BASE - 1));
        l.setForeground(Theme.TEXTE_SECONDAIRE);
        numeros.add(l);
    }

    private void valeur(String texte) {
        JLabel l = new JLabel(texte == null || texte.trim().isEmpty() ? "—" : texte.trim());
        l.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE + 1));
        l.setForeground(Theme.TEXTE);
        numeros.add(l);
    }
}
