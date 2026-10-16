package My_application.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class analyseTest {

    @Test
    public void unResultatVideRemetLaNoteAZero() {
        analyse a = new analyse();
        a.setResultat("1.2 g/l");
        a.setNote(3);
        a.setResultat("");
        assertEquals(0, a.getNote());
    }

    @Test
    public void unResultatNullNeLevePasDException() {
        analyse a = new analyse();
        a.setResultat(null);
        assertEquals("", a.getResultat());
    }

    @Test
    public void egaliteParDesignation() {
        analyse a = new analyse();
        a.setDesign("GLYCEMIE");
        analyse b = new analyse();
        b.setDesign("GLYCEMIE");
        assertTrue(a.egal(b));
        assertFalse(a.egal(null));
        assertFalse(a.egal(new analyse()));
    }
}
