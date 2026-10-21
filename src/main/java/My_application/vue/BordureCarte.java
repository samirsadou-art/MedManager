package My_application.vue;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.border.AbstractBorder;

/**
 * Bordure d'une « carte » : contour arrondi discret et titre en gras à
 * l'intérieur, en haut à gauche. Elle remplace les {@code TitledBorder} des
 * formulaires (voir {@link Theme#harmoniser}) pour donner à toute
 * l'application l'aspect des panneaux de l'agenda.
 */
public class BordureCarte extends AbstractBorder {

    private static final long serialVersionUID = 1L;

    /** Arrondi des cartes, repris par le fond du panneau (style FlatLaf « arc »). */
    public static final int ARC = 14;
    private static final int MARGE = 10;

    private final String titre;

    public BordureCarte(String titre) {
        this.titre = titre == null || titre.trim().isEmpty() ? null : titre.trim();
    }

    public String getTitre() {
        return titre;
    }

    private static Font police() {
        return Theme.police(Font.BOLD, Theme.TAILLE_BASE + 1);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int haut = MARGE;
        if (titre != null) {
            haut += c.getFontMetrics(police()).getHeight() + 6;
        }
        insets.set(haut, MARGE, MARGE, MARGE);
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int largeur, int hauteur) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Theme.BORDURE);
        g2.drawRoundRect(x, y, largeur - 1, hauteur - 1, ARC, ARC);
        if (titre != null) {
            g2.setFont(police());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Theme.TEXTE);
            g2.drawString(titre, x + MARGE + 2, y + MARGE + fm.getAscent());
        }
        g2.dispose();
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
