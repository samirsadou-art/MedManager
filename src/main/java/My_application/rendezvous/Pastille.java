package My_application.rendezvous;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/** Petite pastille ronde de couleur (statuts de l'agenda et légende). */
public class Pastille implements Icon {

    private final Color couleur;
    private final int taille;

    public Pastille(Color couleur, int taille) {
        this.couleur = couleur;
        this.taille = taille;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(couleur);
        g2.fillOval(x, y, taille, taille);
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
