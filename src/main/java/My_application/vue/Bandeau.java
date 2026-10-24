package My_application.vue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Bandeau supérieur de la fenêtre principale : logo et nom de
 * l'application à gauche, heure et date au milieu (mises à jour en
 * direct), informations (utilisateur) à droite.
 */
public class Bandeau extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.FRENCH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private final JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    private final JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
    private final JPanel centre = new JPanel();
    private final JLabel heure = new JLabel("", SwingConstants.CENTER);
    private final JLabel date = new JLabel("", SwingConstants.CENTER);
    /** Remet l'heure à jour chaque seconde, seulement tant que le bandeau est affiché. */
    private final Timer horloge = new Timer(1000, e -> actualiserHorloge());

    public Bandeau(String nom, String sousTitre) {
        setLayout(null);
        setBackground(Theme.PRIMAIRE_FONCE);
        setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 22));

        JLabel logo = new JLabel(new Logo(40));
        JLabel titre = new JLabel(nom);
        titre.setFont(Theme.police(Font.BOLD, 20));
        titre.setForeground(Color.WHITE);
        JLabel sous = new JLabel(sousTitre);
        sous.setFont(Theme.police(Font.PLAIN, 12));
        sous.setForeground(Theme.PRIMAIRE_CLAIR);
        JPanel textes = new JPanel();
        textes.setOpaque(false);
        textes.setLayout(new BoxLayout(textes, BoxLayout.Y_AXIS));
        textes.add(titre);
        textes.add(sous);
        gauche.setOpaque(false);
        gauche.add(logo);
        gauche.add(textes);
        add(gauche);

        heure.setFont(Theme.police(Font.BOLD, 22));
        heure.setForeground(Color.WHITE);
        heure.setAlignmentX(CENTER_ALIGNMENT);
        date.setFont(Theme.police(Font.PLAIN, 13));
        date.setForeground(Theme.PRIMAIRE_CLAIR);
        date.setAlignmentX(CENTER_ALIGNMENT);
        centre.setOpaque(false);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.add(heure);
        centre.add(date);
        add(centre);
        actualiserHorloge();

        droite.setOpaque(false);
        add(droite);
    }

    /** « 14:05:09 » */
    static String texteHeure(LocalTime t) {
        return HEURE.format(t);
    }

    /** « Jeudi 22 octobre 2026 » */
    static String texteDate(LocalDate d) {
        String s = DATE.format(d);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void actualiserHorloge() {
        heure.setText(texteHeure(LocalTime.now()));
        date.setText(texteDate(LocalDate.now()));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        actualiserHorloge();
        horloge.start();
    }

    @Override
    public void removeNotify() {
        horloge.stop();
        super.removeNotify();
    }

    /**
     * Logo à gauche, informations à droite, et l'heure centrée sur la largeur
     * du bandeau (et non entre les deux blocs), décalée seulement si elle
     * devait les chevaucher.
     */
    @Override
    public void doLayout() {
        Insets in = getInsets();
        int h = getHeight() - in.top - in.bottom;
        Dimension g = gauche.getPreferredSize();
        Dimension d = droite.getPreferredSize();
        Dimension c = centre.getPreferredSize();
        gauche.setBounds(in.left, in.top + (h - g.height) / 2, g.width, g.height);
        droite.setBounds(getWidth() - in.right - d.width, in.top + (h - d.height) / 2, d.width, d.height);
        int x = (getWidth() - c.width) / 2;
        x = Math.max(x, in.left + g.width + 12);
        x = Math.min(x, getWidth() - in.right - d.width - 12 - c.width);
        centre.setBounds(x, in.top + (h - c.height) / 2, c.width, c.height);
    }

    @Override
    public Dimension getPreferredSize() {
        Insets in = getInsets();
        Dimension g = gauche.getPreferredSize();
        Dimension d = droite.getPreferredSize();
        Dimension c = centre.getPreferredSize();
        int hauteur = Math.max(c.height, Math.max(g.height, d.height));
        return new Dimension(in.left + g.width + c.width + d.width + 24 + in.right, in.top + hauteur + in.bottom);
    }

    /** Information affichée à droite du bandeau, en blanc, avec son pictogramme. */
    public JLabel ajouterInfo(String icone, String texte) {
        JLabel l = new JLabel(texte, icone == null ? null : Icones.svg(icone, 20, Color.WHITE), JLabel.LEFT);
        l.setFont(Theme.police(Font.BOLD, 13));
        l.setForeground(Color.WHITE);
        l.setIconTextGap(8);
        droite.add(l);
        return l;
    }

    public void ajouter(JComponent c) {
        droite.add(c);
    }

    /** Croix médicale blanche dans un disque, comme l'icône de l'application. */
    private static final class Logo implements Icon {

        private final int taille;

        Logo(int taille) {
            this.taille = taille;
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.PRIMAIRE);
            g2.fillOval(x, y, taille, taille);
            int marge = taille / 5;
            Icones.svg("medical-cross", taille - 2 * marge, Color.WHITE).paintIcon(c, g2, x + marge, y + marge);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return taille;
        }

        @Override
        public int getIconHeight() {
            return taille;
        }
    }
}
