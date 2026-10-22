package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.dao.PatientDao;
import My_application.entities.patient;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
        patient p = Donnees.patient(0, "DOUBLON");
        p.setTelephone("0555123456");
        p.setNumeroDossier(" 1 ");
        try {
            service.creer(p);
            org.junit.Assert.fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("déjà utilisé"));
        }
    }

    @Test
    public void numeroDeDossierAttribueAutomatiquement() {
        patient ancien = inscrire("ANCIEN", "0555000001", "2019/37");
        patient auto = inscrire("AUTO", "0555000002", null);
        int plusGrand = 0;
        for (patient p : base.patients.values()) {
            if (p.getNumeroDossier() != null && p.getNumeroDossier().matches("[0-9]+")
                    && p != auto) {
                plusGrand = Math.max(plusGrand, Integer.parseInt(p.getNumeroDossier()));
            }
        }
        assertEquals("2019/37", ancien.getNumeroDossier());
        assertEquals(String.valueOf(plusGrand + 1), auto.getNumeroDossier());
    }

    @Test
    public void numeroDeDossierSaisiNormaliseEtVerifie() {
        assertEquals("A-12", PatientService.normaliserNumeroDossier("  a-12 "));
        assertEquals("2019/37", PatientService.normaliserNumeroDossier("2019/37"));
        assertNull(PatientService.normaliserNumeroDossier("  "));
        for (String faux : new String[]{"12 34", "12#", "é1", "123456789012345678901"}) {
            try {
                PatientService.normaliserNumeroDossier(faux);
                org.junit.Assert.fail(faux);
            } catch (MetierException attendu) {
                assertTrue(attendu.getMessage().contains("numéro de dossier"));
            }
        }
    }

    @Test
    public void unAutrePosteAPrisLeNumeroAttribueOnEnPrendUnAutre() {
        final int[] appels = {0};
        PatientService concurrent = new PatientService(new MemoireDAOFactory() {
            {
                patients.putAll(base.patients);
            }

            @Override
            public PatientDao getPatientDao() {
                final PatientDao reel = super.getPatientDao();
                return (PatientDao) Proxy.newProxyInstance(PatientDao.class.getClassLoader(),
                        new Class<?>[]{PatientDao.class}, new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method m, Object[] a) throws Throwable {
                                if (m.getName().equals("prochainNumeroDossier") && appels[0]++ == 0) {
                                    return "1"; // déjà pris entre la lecture et l'insertion
                                }
                                try {
                                    return m.invoke(reel, a);
                                } catch (InvocationTargetException e) {
                                    throw e.getCause();
                                }
                            }
                        });
            }
        });
        patient p = Donnees.patient(0, "CONCURRENT");
        p.setTelephone("0555000009");
        concurrent.creer(p);
        assertEquals(2, appels[0]);
        assertTrue(!"1".equals(p.getNumeroDossier()));
    }

    @Test
    public void numeroDeDossierModifiable() {
        patient p = base.patients.get(2);
        p.setNumeroDossier("2019/40");
        service.modifier(p);
        assertEquals("2019/40", base.patients.get(2).getNumeroDossier());
        p.setNumeroDossier("1");
        try {
            service.modifier(p);
            org.junit.Assert.fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("déjà utilisé"));
        }
        p.setNumeroDossier(" ");
        try {
            service.modifier(p);
            org.junit.Assert.fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("obligatoire"));
        }
    }

    @Test
    public void rechercheParNumeroDeDossierEnTete() {
        patient p = inscrire("ZERHOUNI", "0555000005", "2019/37");
        assertEquals(p, service.rechercher("2019/37").get(0));
        assertEquals(p, service.rechercher(" 2019/37 ").get(0));
        assertEquals(base.patients.get(2), service.rechercher("2").get(0));
    }

    private patient inscrire(String nom, String telephone, String numero) {
        patient p = Donnees.patient(0, nom);
        p.setTelephone(telephone);
        p.setNumeroDossier(numero);
        return service.creer(p);
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
