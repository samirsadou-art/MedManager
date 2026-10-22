package My_application.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ConstantesVitalesTest {

    @Test
    public void champsVidesNonRenseignes() {
        assertEquals(-1, ConstantesVitales.temperature(" "), 0);
        assertEquals(-1, ConstantesVitales.pulsation(null));
        assertArrayEquals(new int[]{-1, -1}, ConstantesVitales.pression(""));
    }

    @Test
    public void valeursValides() {
        assertEquals(38.5, ConstantesVitales.temperature("38,5"), 0.001); // virgule acceptée
        assertEquals(72, ConstantesVitales.pulsation("72"));
        assertArrayEquals(new int[]{120, 80}, ConstantesVitales.pression(" 12/8 "));
        assertEquals(70.2, ConstantesVitales.poids("70.2"), 0.001);
        assertEquals(16, ConstantesVitales.respiration("16"));
    }

    @Test(expected = MetierException.class)
    public void temperatureInvraisemblable() {
        ConstantesVitales.temperature("99");
    }

    @Test(expected = MetierException.class)
    public void pressionMalFormee() {
        ConstantesVitales.pression("12-8");
    }

    @Test
    public void pressionEnregistreeEnMmHg() {
        assertArrayEquals(new int[]{120, 80}, ConstantesVitales.pression("120/80"));
        assertArrayEquals(new int[]{135, 85}, ConstantesVitales.pression("13,5/8.5"));
        assertArrayEquals("unités mélangées", new int[]{120, 80}, ConstantesVitales.pression("12/80"));
        assertArrayEquals(new int[]{300, 200}, ConstantesVitales.pression("300/200"));
        assertArrayEquals(new int[]{40, 20}, ConstantesVitales.pression("4/2"));
    }

    @Test
    public void pressionInvraisemblableOuInversee() {
        for (String faux : new String[]{"310/90", "120/210", "3/1", "8/12", "120/120", "12/8/5", "12.55/8"}) {
            try {
                ConstantesVitales.pression(faux);
                fail(faux);
            } catch (MetierException attendu) {
                assertTrue(attendu.getMessage().contains("Pression") || attendu.getMessage().contains("pression"));
            }
        }
    }

    @Test(expected = MetierException.class)
    public void poidsNonNumerique() {
        ConstantesVitales.poids("abc");
    }
}
