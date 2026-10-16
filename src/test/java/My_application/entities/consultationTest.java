package My_application.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import My_application.fake.Donnees;
import org.junit.Test;

public class consultationTest {

    private final consultation c = new consultation(Donnees.patient(1, "A"), Donnees.medecin(1));

    @Test
    public void unSymptomeNEstAjouteQuUneFoisMemeAvecUneCasseDifferente() {
        assertTrue(c.addSymptome(new symptome(1, "Fièvre")));
        assertFalse(c.addSymptome(new symptome(2, "FIÈVRE")));
        assertEquals(1, c.getNombreSymptomes());
    }

    @Test
    public void unePathologieNEstAjouteeQuUneFois() {
        assertTrue(c.addPathologie(new pathologie(1, "GRIPPE")));
        assertFalse(c.addPathologie(new pathologie(1, "GRIPPE")));
        assertFalse(c.addPathologie(null));
        assertEquals(1, c.getNombrePathologies());
    }

    @Test
    public void retirerUnSymptome() {
        c.addSymptome(new symptome(1, "Toux"));
        c.addSymptome(new symptome(2, "Fièvre"));
        c.retirerSymptome(0);
        assertEquals("Fièvre", c.getSymptomes().get(0).getDesignation());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void laListeExposeeNEstPasModifiable() {
        c.getSymptomes().add(new symptome(1, "x"));
    }

    @Test
    public void uneOrdonnanceVideNeComptePas() {
        assertFalse(c.hasOrdonnance());
        c.getOrdoPat().ajouter(Donnees.medicament(1, "DOLIPRANE", 1, 3, 2, 5));
        assertTrue(c.hasOrdonnance());
        c.setOrdoPat(null);
        assertFalse(c.hasOrdonnance()); // plus de NullPointerException
    }

    @Test
    public void nouvelleTantQueNonEnregistree() {
        assertTrue(c.estNouvelle());
        c.setIdConsul(12);
        assertFalse(c.estNouvelle());
    }

    @Test
    public void constantesNonRenseigneesParDefaut() {
        assertEquals(consultation.NON_RENSEIGNE, c.getTemperature(), 0);
        assertEquals("", c.getPressionA());
        c.setPressionA(null);
        assertEquals("", c.getPressionA());
    }
}
