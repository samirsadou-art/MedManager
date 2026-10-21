package My_application.vue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.junit.Assert.assertTrue;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.Test;

public class ThemeTest {

    @Test
    public void lesFondsSombresDeviennentLaCouleurPrincipale() {
        assertEquals(Theme.PRIMAIRE, Theme.fond(Color.BLACK, false));
        assertEquals(Theme.PRIMAIRE, Theme.fond(new Color(0, 0, 102), false));   // bandeau bleu marine
    }

    @Test
    public void lesPastelsEtLesGrisDeviennentNeutres() {
        assertEquals(Theme.FOND, Theme.fond(new Color(255, 204, 204), false));   // panneau rose
        assertEquals(Theme.FOND, Theme.fond(new Color(204, 204, 204), false));
        assertEquals(Theme.SURFACE, Theme.fond(new Color(204, 255, 255), true)); // arbre cyan
        assertEquals(Theme.SURFACE, Theme.fond(Color.WHITE, false));
    }

    @Test
    public void lesCouleursDeTexte() {
        assertEquals(Color.WHITE, Theme.texte(Color.RED, true));                 // sur bandeau
        assertEquals(Theme.DANGER, Theme.texte(Color.RED, false));               // alerte
        assertEquals(Theme.PRIMAIRE_FONCE, Theme.texte(new Color(102, 0, 0), false));
        assertEquals(Theme.TEXTE_SECONDAIRE, Theme.texte(new Color(102, 102, 102), false));
        assertEquals(Theme.TEXTE, Theme.texte(Color.BLACK, false));
        assertEquals(Theme.TEXTE, Theme.texte(Color.WHITE, false));              // blanc sur fond clair
    }

    @Test
    public void lesAnciennesIconesOntUnEquivalentSvg() {
        Icon i = Icones.remplacement("file:/x/res/icons/consult48.png", 48, Theme.PRIMAIRE);
        assertNotNull(i);
        assertEquals(32, i.getIconWidth());
        assertNull(Icones.remplacement("/res/icons/h128.png", 128, Theme.PRIMAIRE)); // avatar conservé
        assertNull(Icones.remplacement(null, 16, Theme.PRIMAIRE));
    }

    @Test
    public void tailleDesPictogrammes() {
        assertEquals(64, Icones.taille(128));
        assertEquals(32, Icones.taille(48));
        assertEquals(24, Icones.taille(32));
        assertEquals(20, Icones.taille(24));
        assertEquals(16, Icones.taille(16));
    }

    @Test
    public void unGroupeTitreDevientUneCarteBlanche() {
        JPanel groupe = new JPanel();
        groupe.setBorder(BorderFactory.createTitledBorder("Paramètres Vitaux"));
        JPanel interieur = new JPanel();
        JLabel cellule = new JLabel("Num:");
        cellule.setOpaque(true);
        cellule.setBackground(new Color(204, 204, 204));
        interieur.add(cellule);
        groupe.add(interieur);
        JPanel fenetre = new JPanel();
        fenetre.add(groupe);

        Theme.harmoniser(fenetre);

        assertTrue(groupe.getBorder() instanceof BordureCarte);
        assertEquals("Paramètres Vitaux", ((BordureCarte) groupe.getBorder()).getTitre());
        assertEquals(Theme.SURFACE, groupe.getBackground());
        assertTrue(Theme.estCarte(groupe));
        assertEquals("le panneau intérieur prend le blanc de la carte", Theme.SURFACE, interieur.getBackground());
        assertEquals("les cellules grisées restent grises", Theme.FOND, cellule.getBackground());
        assertEquals("hors carte, le fond de fenêtre reste", false, Theme.estCarte(fenetre));
    }
}
