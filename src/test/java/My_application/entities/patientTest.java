package My_application.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import org.junit.Test;

public class patientTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 8);

    private static patient ne(LocalDate naissance) {
        patient p = new patient();
        p.setDat_nais(Date.valueOf(naissance));
        return p;
    }

    @Test
    public void ageEnAnneesRevolues() {
        assertEquals(46, ne(LocalDate.of(1980, 5, 17)).getAge(AUJOURDHUI));
    }

    @Test
    public void laVeilleDeSonAnniversaireOnNaPasEncoreLAge() {
        // L'ancien calcul (jours / 365) se trompait autour des anniversaires.
        assertEquals(39, ne(LocalDate.of(1986, 10, 9)).getAge(AUJOURDHUI));
        assertEquals(40, ne(LocalDate.of(1986, 10, 8)).getAge(AUJOURDHUI));
    }

    @Test
    public void unNourrissonALAgeEnMoisNegatif() {
        assertEquals(-7, ne(LocalDate.of(2026, 3, 1)).getAge(AUJOURDHUI));
    }

    @Test
    public void dateDeNaissanceFutureOuAbsenteDonneZero() {
        assertEquals(0, ne(LocalDate.of(2027, 1, 1)).getAge(AUJOURDHUI));
        assertEquals(0, new patient().getAge(AUJOURDHUI));
    }

    @Test
    public void egaliteParIdentifiant() {
        patient a = new patient();
        a.setIdentPat(5);
        patient b = new patient();
        b.setIdentPat(5);
        assertEquals(a, b);
        assertNotEquals(a, new patient());
        assertFalse(new patient().equals(new patient())); // deux patients non enregistrés ne sont pas égaux
    }

    @Test
    public void sexeNullNEstPasMasculin() {
        patient p = new patient();
        assertFalse(p.isMasculin());
        p.setSexe(true);
        assertTrue(p.isMasculin());
    }
}
