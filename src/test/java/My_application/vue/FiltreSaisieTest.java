package My_application.vue;

import static org.junit.Assert.assertEquals;

import javax.swing.JTextField;
import org.junit.Test;

public class FiltreSaisieTest {

    private JTextField champ(FiltreSaisie filtre) {
        JTextField c = new JTextField();
        FiltreSaisie.installer(c, filtre);
        return c;
    }

    @Test
    public void unCollageInvalideEstRefuseEnEntier() {
        JTextField temperature = champ(FiltreSaisie.decimal(5));
        temperature.setText("38,5");
        assertEquals("38,5", temperature.getText());
        temperature.replaceSelection("abc"); // ce que fait Ctrl+V
        assertEquals("38,5", temperature.getText());
    }

    @Test
    public void laLongueurMaximaleCompteLeTexteFinal() {
        JTextField pulsation = champ(FiltreSaisie.entier(3));
        pulsation.setText("120");
        pulsation.setCaretPosition(3);
        pulsation.replaceSelection("0");
        assertEquals("120", pulsation.getText());
        pulsation.selectAll();
        pulsation.replaceSelection("95"); // remplacer une sélection reste possible
        assertEquals("95", pulsation.getText());
    }

    @Test
    public void lEffacementEstToujoursPermis() {
        JTextField pression = champ(new FiltreSaisie("0-9/", 5));
        pression.setText("12/8");
        pression.setText("");
        assertEquals("", pression.getText());
    }

    @Test
    public void uneValeurLueEnBaseEstAfficheeTelleQuelle() {
        JTextField pression = champ(new FiltreSaisie("0-9/", 7));
        FiltreSaisie.afficher(pression, "12 / 8"); // saisie ancienne, hors format
        assertEquals("12 / 8", pression.getText());
        pression.setCaretPosition(0);
        pression.replaceSelection("x"); // le filtre est toujours en place
        assertEquals("12 / 8", pression.getText());
    }
}
