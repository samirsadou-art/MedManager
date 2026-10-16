package My_application.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import My_application.fake.Donnees;
import java.util.Arrays;
import org.junit.Test;

public class ordonnanceTest {

    @Test
    public void unMedicamentNEstPrescritQuUneFois() {
        ordonnance o = new ordonnance();
        assertTrue(o.ajouter(Donnees.medicament(1, "DOLIPRANE 1G", 1, 3, 2, 5)));
        assertFalse(o.ajouter(Donnees.medicament(1, "DOLIPRANE 1G", 2, 2, 0, 3)));
        assertEquals(1, o.getNombreMedicament());
    }

    @Test
    public void remplacerLeContenu() {
        ordonnance o = new ordonnance();
        o.ajouter(Donnees.medicament(1, "A", 1, 1, 0, 0));
        o.setMedicaments(Arrays.asList(Donnees.medicament(2, "B", 1, 1, 0, 0), Donnees.medicament(3, "C", 1, 1, 0, 0)));
        assertEquals(2, o.getNombreMedicament());
        assertEquals("B", o.getMedicament(0).getDesignmed());
    }
}
