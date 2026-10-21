package My_application.vue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Bandeau supérieur de la fenêtre principale : logo et nom de
 * l'application à gauche, informations (utilisateur, date) à droite.
 */
public class Bandeau extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));

    public Bandeau(String nom, String sousTitre) {
        super(new BorderLayout());
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
        JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        gauche.setOpaque(false);
        gauche.add(logo);
        gauche.add(textes);
        add(gauche, BorderLayout.WEST);
        droite.setOpaque(false);
        add(droite, BorderLayout.EAST);
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
