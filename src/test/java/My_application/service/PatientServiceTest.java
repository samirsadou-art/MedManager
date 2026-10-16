package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import My_application.entities.patient;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.sql.Date;
import java.time.LocalDate;
import org.junit.Test;

public class PatientServiceTest {

    private final MemoireDAOFactory base = Donnees.base();
    private final PatientService service = new PatientService(base);

    @Test
    public void creationNormaliseNomEtPrenom() {
        patient p = Donnees.patient(0, "  haddad ", "  KARIMA", LocalDate.of(1975, 1, 1), false);
        service.creer(p);
        assertTrue(p.getIdentPat() > 0);
        assertEquals("HADDAD", p.getNom());
        assertEquals("Karima", p.getPrenom());
        assertEquals(p, base.patients.get(p.getIdentPat()));
    }

    @Test(expected = MetierException.class)
    public void nomObligatoire() {
        service.creer(Donnees.patient(0, " "));
    }

    @Test(expected = MetierException.class)
    public void naissanceDansLeFutur() {
        service.creer(Donnees.patient(0, "X", "Y", LocalDate.now().plusDays(1), true));
    }

    @Test(expected = MetierException.class)
    public void numeroDeDossierDejaUtilise() {
        service.creer(Donnees.patient(1, "DOUBLON"));
    }

    @Test(expected = MetierException.class)
    public void sexeObligatoire() {
        patient p = Donnees.patient(0, "X");
        p.setSexe(null);
        service.creer(p);
    }

    @Test
    public void rechercheParDebutDuNom() {
        assertEquals(1, service.rechercher("bel").size());
        assertEquals(2, service.rechercher(null).size());
    }

    @Test
    public void dateMinimale() {
        patient p = Donnees.patient(0, "ANCIEN");
        p.setDat_nais(Date.valueOf(LocalDate.now().minusYears(131)));
        try {
            service.creer(p);
            org.junit.Assert.fail("date refusée attendue");
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("invraisemblable"));
        }
    }
}
