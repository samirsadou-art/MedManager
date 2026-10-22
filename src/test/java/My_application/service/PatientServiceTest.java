package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.patient;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.time.LocalDate;
import org.junit.Test;

public class PatientServiceTest {

    private final MemoireDAOFactory base = Donnees.base();
    private final PatientService service = new PatientService(base);

    @Test
    public void creationNormaliseNomEtPrenom() {
        patient p = Donnees.patient(0, "  haddad ", "  KARIMA", LocalDate.of(1975, 1, 1), false);
        p.setTelephone("0550 11 22 33");
        service.creer(p);
        assertTrue(p.getIdentPat() > 0);
        assertEquals("HADDAD", p.getNom());
        assertEquals("Karima", p.getPrenom());
        assertEquals(p, base.patients.get(p.getIdentPat()));
        assertEquals("0550112233", p.getTelephone());
    }

    @Test
    public void telephoneObligatoireALInscription() {
        try {
            service.creer(Donnees.patient(0, "SANS", "Tel", LocalDate.of(1990, 1, 1), true));
            org.junit.Assert.fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("téléphone"));
        }
    }

    @Test
    public void formatsDeTelephone() {
        assertEquals("0555123456", PatientService.normaliserTelephone("0555 12 34 56"));
        assertEquals("0661223344", PatientService.normaliserTelephone("+213 661-22-33-44"));
        assertEquals("0770112233", PatientService.normaliserTelephone("00213 (770) 11.22.33"));
        assertEquals("026123456", PatientService.normaliserTelephone("026 12 34 56"));
        assertNull(PatientService.normaliserTelephone("  "));
        for (String faux : new String[]{"0555 12 34", "0855123456", "05551234567", "tel 0555", "026 12 34 5"}) {
            try {
                PatientService.normaliserTelephone(faux);
                org.junit.Assert.fail(faux);
            } catch (MetierException attendu) {
                // ok
            }
        }
    }

    @Test
    public void emailVerifie() {
        assertEquals("a.b@cabinet.dz", PatientService.normaliserEmail(" A.B@Cabinet.DZ "));
        assertNull(PatientService.normaliserEmail(""));
        try {
            PatientService.normaliserEmail("pas-une-adresse");
            org.junit.Assert.fail();
        } catch (MetierException attendu) {
            assertTrue(attendu.getMessage().contains("e-mail"));
        }
    }

    @Test
    public void doublonsParNomEtNaissanceOuParTelephone() {
        patient existant = base.patients.get(1);
        existant.setTelephone("0555000000");
        patient memeNom = Donnees.patient(0, " amrani ", "Autre", existant.getDat_nais(), true);
        assertEquals(1, service.doublonsPossibles(memeNom).size());
        patient memeTel = Donnees.patient(0, "Inconnu", "X", LocalDate.of(2001, 1, 1), true);
        memeTel.setTelephone("0555 00 00 00");
        assertEquals(1, service.doublonsPossibles(memeTel).size());
        // En modification, la fiche ne se trouve pas elle-même en double.
        assertTrue(service.doublonsPossibles(existant).isEmpty());
        assertTrue(service.doublonsPossibles(Donnees.patient(0, "Inconnu")).isEmpty());
    }

    @Test
    public void modificationDeLaFicheTracee() {
        patient p = base.patients.get(2);
        p.setVille("  Azazga ");
        p.setTelephone("0770 11 22 33");
        service.modifier(p);
        assertEquals("Azazga", base.patients.get(2).getVille());
        assertEquals("0770112233", base.patients.get(2).getTelephone());
        patient fantome = Donnees.patient(99, "FANTOME");
        try {
            service.modifier(fantome);
            org.junit.Assert.fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("n'existe pas"));
        }
    }

    @Test
    public void rechercheParTelephone() {
        base.patients.get(2).setTelephone("0661223344");
        assertEquals(1, service.rechercher("0661 22").size());
        assertEquals(1, service.rechercher("+213 661").size());
        assertEquals(0, service.rechercher("0770").size());
    }

    @Test(expected = MetierException.class)
    public void nomObligatoire() {
        service.creer(Donnees.patient(0, " "));
    }

    @Test(expected = MetierException.class)
    public void naissanceDansLeFutur() {
        service.creer(Donnees.patient(0, "X", "Y", LocalDate.now().plusDays(1), true));
    }

    @Test
    public void numeroDeDossierDejaUtilise() {
        patient p = Donnees.patient(1, "DOUBLON");
        p.setTelephone("0555123456");
        try {
            service.creer(p);
            org.junit.Assert.fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("déjà utilisé"));
        }
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
        p.setDat_nais(LocalDate.now().minusYears(131));
        try {
            service.creer(p);
            org.junit.Assert.fail("date refusée attendue");
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("invraisemblable"));
        }
    }
}
