package My_application.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import My_application.fake.Donnees;
import java.util.Arrays;
import org.junit.Test;

public class DossierPatientTest {

    private consultation consultation(int id) {
        consultation c = new consultation(null, null);
        c.setIdConsul(id);
        return c;
    }

    @Test
    public void navigationEntreConsultations() {
        DossierPatient d = new DossierPatient(Donnees.patient(1, "A"), Donnees.medecin(1));
        consultation c1 = consultation(1);
        consultation c2 = consultation(2);
        consultation c3 = consultation(3);
        d.setConsultations(Arrays.asList(c1, c2, c3));

        assertEquals(2, d.getConsultPosition()); // ouvert sur la dernière
        assertFalse(d.HasNext());
        assertSame(c2, d.PreviousConsultation());
        assertSame(c1, d.PreviousConsultation());
        assertFalse(d.HasPrevious());
        assertSame(c1, d.PreviousConsultation()); // reste sur la première
        assertSame(c3, d.LastConsultation());
        assertSame(c1, d.FirstConsultation());
        assertSame(c2, d.NextConsultation());
    }

    @Test
    public void dossierVide() {
        DossierPatient d = new DossierPatient(Donnees.patient(1, "A"), Donnees.medecin(1));
        assertNull(d.FirstConsultation());
        assertNull(d.LastConsultation());
        assertFalse(d.HasNext());
        assertFalse(d.HasPrevious());
    }

    @Test
    public void consultationNonEnregistree() {
        DossierPatient d = new DossierPatient(Donnees.patient(1, "A"), Donnees.medecin(1));
        d.setConsultations(Arrays.asList(consultation(1)));
        assertNull(d.getConsultationNonEnregistree());
        consultation nouvelle = new consultation(null, null);
        d.ajouterConsultation(nouvelle);
        assertSame(nouvelle, d.getConsultationNonEnregistree());
        assertTrue(d.HasNext());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void positionInvalideRefusee() {
        new DossierPatient(Donnees.patient(1, "A"), null).SetPosition(0);
    }
}
