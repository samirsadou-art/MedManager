package My_application.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConstantesVitalesTest {

    @Test
    public void champsVidesNonRenseignes() {
        assertEquals(-1, ConstantesVitales.temperature(" "), 0);
        assertEquals(-1, ConstantesVitales.pulsation(null));
        assertEquals("", ConstantesVitales.pression(""));
    }

    @Test
    public void valeursValides() {
        assertEquals(38.5, ConstantesVitales.temperature("38,5"), 0.001); // virgule acceptée
        assertEquals(72, ConstantesVitales.pulsation("72"));
        assertEquals("12/8", ConstantesVitales.pression(" 12/8 "));
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

    @Test(expected = MetierException.class)
    public void poidsNonNumerique() {
        ConstantesVitales.poids("abc");
    }
}
