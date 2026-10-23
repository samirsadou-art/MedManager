package My_application.vue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * {@link FlowLayout} qui passe à la ligne quand la largeur manque et agrandit
 * la hauteur du conteneur en conséquence (le FlowLayout de Swing garde une
 * seule ligne et cache les composants qui débordent). Sert aux rangées de
 * boutons des panneaux dont la largeur se règle à la souris.
 */
public class RetourALaLigne extends FlowLayout {

    private static final long serialVersionUID = 1L;

    public RetourALaLigne(int alignement, int ecartH, int ecartV) {
        super(alignement, ecartH, ecartV);
    }

    @Override
    public Dimension preferredLayoutSize(Container cible) {
        return taille(cible, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container cible) {
        Dimension d = taille(cible, false);
        d.width -= getHgap() + 1;
        return d;
    }

    private Dimension taille(Container cible, boolean preferee) {
        synchronized (cible.getTreeLock()) {
            Container mesure = cible;
            while (mesure.getSize().width == 0 && mesure.getParent() != null) {
                mesure = mesure.getParent();
            }
            int largeurCible = mesure.getSize().width;
            if (largeurCible == 0) {
                largeurCible = Integer.MAX_VALUE;
            }
            Insets bords = cible.getInsets();
            int largeurMax = largeurCible - (bords.left + bords.right + getHgap() * 2);

            Dimension total = new Dimension(0, 0);
            int largeurLigne = 0;
            int hauteurLigne = 0;
            for (Component c : cible.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                Dimension d = preferee ? c.getPreferredSize() : c.getMinimumSize();
                if (largeurLigne > 0 && largeurLigne + getHgap() + d.width > largeurMax) {
                    ajouterLigne(total, largeurLigne, hauteurLigne);
                    largeurLigne = 0;
                    hauteurLigne = 0;
                }
                if (largeurLigne > 0) {
                    largeurLigne += getHgap();
                }
                largeurLigne += d.width;
                hauteurLigne = Math.max(hauteurLigne, d.height);
            }
            ajouterLigne(total, largeurLigne, hauteurLigne);
            total.width += bords.left + bords.right + getHgap() * 2;
            total.height += bords.top + bords.bottom + getVgap() * 2;
            return total;
        }
    }

    private void ajouterLigne(Dimension total, int largeur, int hauteur) {
        total.width = Math.max(total.width, largeur);
        if (total.height > 0) {
            total.height += getVgap();
        }
        total.height += hauteur;
    }
}
