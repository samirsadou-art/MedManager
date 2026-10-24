package My_application.vue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.swing.JLabel;
import org.junit.Test;

public class BandeauTest {

    @Test
    public void heureEtDateEnFrancais() {
        assertEquals("09:05:07", Bandeau.texteHeure(LocalTime.of(9, 5, 7)));
        assertEquals("Jeudi 22 octobre 2026", Bandeau.texteDate(LocalDate.of(2026, 10, 22)));
    }

    @Test
    public void horlogeCentreeSurLaLargeur() {
        Bandeau b = new Bandeau(Theme.NOM_LOGICIEL, "Cabinet médical");
        b.ajouterInfo("user", "Dr Amrani Karim · Médecin");
        b.setSize(1400, b.getPreferredSize().height);
        b.doLayout();
        Rectangle centre = b.getComponent(1).getBounds();
        assertEquals(700, centre.x + centre.width / 2, 1);
        Rectangle gauche = b.getComponent(0).getBounds();
        Rectangle droite = b.getComponent(2).getBounds();
        assertTrue(gauche.x + gauche.width < centre.x);
        assertTrue(centre.x + centre.width < droite.x);
        assertTrue(((JLabel) ((javax.swing.JPanel) b.getComponent(1)).getComponent(0)).getText().matches("\\d\\d:\\d\\d:\\d\\d"));
    }
}
