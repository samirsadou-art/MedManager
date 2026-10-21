package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.dao.DoublonException;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.patient;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;

public class RendezVousServiceTest {

    /** « Maintenant » figé : jeudi 8 octobre 2026, 10 h 20. */
    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 8, 10, 20);
    private static final LocalDate DEMAIN = LocalDate.of(2026, 10, 9);

    private MemoireDAOFactory base;
    private RendezVousService service;
    private User secretaire;

    @Before
    public void preparer() {
        base = Donnees.base();
        ZoneId zone = ZoneId.systemDefault();
        Clock horloge = Clock.fixed(MAINTENANT.atZone(zone).toInstant(), zone);
        service = new RendezVousService(base, horloge, 8, 9, 4);
        secretaire = Donnees.utilisateur(1);
        for (int i = 10; i < 20; i++) {
            base.patients.put(i, Donnees.patient(i, "PATIENT" + i));
        }
    }

    private Rendez_Vous prendre(int idPatient, LocalDate jour, int heure) {
        Rendez_Vous r = service.proposer(base.patients.get(idPatient), secretaire, jour, heure);
        service.confirmer(r);
        return r;
    }

    @Test
    public void premierCreneauLibreDeLHeureParQuartDHeure() {
        assertEquals(LocalTime.of(9, 0), prendre(10, DEMAIN, 9).getHeure().toLocalTime());
        assertEquals(LocalTime.of(9, 15), prendre(11, DEMAIN, 9).getHeure().toLocalTime());
        assertEquals(LocalTime.of(9, 30), prendre(12, DEMAIN, 9).getHeure().toLocalTime());
        assertEquals(LocalTime.of(9, 45), prendre(13, DEMAIN, 9).getHeure().toLocalTime());
    }

    @Test
    public void uneHeureCompleteEstRefusee() {
        for (int i = 10; i < 14; i++) {
            prendre(i, DEMAIN, 9);
        }
        try {
            service.proposer(base.patients.get(14), secretaire, DEMAIN, 9);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("complet"));
        }
    }

    @Test
    public void unCreneauLibereEstReattribue() {
        prendre(10, DEMAIN, 9);
        Rendez_Vous quart = prendre(11, DEMAIN, 9);
        prendre(12, DEMAIN, 9);
        service.annuler(quart, base.patients.get(11));
        assertEquals(LocalTime.of(9, 15), prendre(13, DEMAIN, 9).getHeure().toLocalTime());
    }

    @Test(expected = MetierException.class)
    public void unPatientNaQuUnRendezVousParHeure() {
        prendre(10, DEMAIN, 9);
        prendre(10, DEMAIN, 9);
    }

    @Test
    public void lesCreneauxDejaPassesSontIgnores() {
        // Il est 10 h 20 : dans l'heure de 10 h, seul 10 h 30 ou 10 h 45 sont possibles.
        assertEquals(LocalTime.of(10, 30), prendre(10, MAINTENANT.toLocalDate(), 10).getHeure().toLocalTime());
    }

    @Test(expected = MetierException.class)
    public void pasDeRendezVousDansLePasse() {
        service.proposer(base.patients.get(10), secretaire, MAINTENANT.toLocalDate(), 9);
    }

    @Test(expected = MetierException.class)
    public void cabinetFerme() {
        service.proposer(base.patients.get(10), secretaire, DEMAIN, 18);
    }

    @Test
    public void laConfirmationDetecteUnCreneauPrisEntreTemps() {
        // Deux postes proposent le même créneau au même moment...
        Rendez_Vous poste1 = service.proposer(base.patients.get(10), secretaire, DEMAIN, 11);
        Rendez_Vous poste2 = service.proposer(base.patients.get(11), secretaire, DEMAIN, 11);
        service.confirmer(poste1);
        try {
            service.confirmer(poste2);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("autre patient"));
        }
        assertEquals(1, base.rendezVous.size());
        assertEquals(1, base.rollbacks);
    }

    @Test
    public void unDoublonRefuseParLaBaseDevientUnMessageMetier() {
        // Course perdue : l'autre poste a inséré entre la vérification et
        // l'insertion, et la clé primaire (date, heure) refuse la ligne.
        Rendez_Vous r = service.proposer(base.patients.get(10), secretaire, DEMAIN, 11);
        base.echouerSur("rdv.insert", new DoublonException("Duplicate entry", null));
        try {
            service.confirmer(r);
            fail();
        } catch (MetierException e) {
            assertEquals(RendezVousService.CRENEAU_PRIS, e.getMessage());
        }
        assertEquals(0, base.rendezVous.size());
        assertEquals(1, base.rollbacks);
    }

    @Test
    public void annulerNeSupprimeQueLeRendezVousDuPatient() {
        Rendez_Vous a = prendre(10, DEMAIN, 9);
        prendre(11, DEMAIN, 9);
        service.annuler(a, base.patients.get(10));
        assertEquals(1, base.rendezVous.size());
        assertEquals(11, base.rendezVous.get(0).getPatient().getIdentPat());
    }

    @Test(expected = MetierException.class)
    public void onNAnnulePasLeRendezVousDUnAutrePatient() {
        Rendez_Vous a = prendre(10, DEMAIN, 9);
        service.annuler(a, base.patients.get(11));
    }

    @Test
    public void semaineDuDimancheAuSamedi() {
        assertEquals(LocalDate.of(2026, 10, 4), RendezVousService.dimancheDeLaSemaine(LocalDate.of(2026, 10, 8)));
        assertEquals(LocalDate.of(2026, 10, 4), RendezVousService.dimancheDeLaSemaine(LocalDate.of(2026, 10, 4)));
        prendre(10, LocalDate.of(2026, 10, 10), 9); // samedi : dans la semaine
        prendre(11, LocalDate.of(2026, 10, 11), 9); // dimanche suivant : hors semaine
        assertEquals(1, service.deLaSemaine(LocalDate.of(2026, 10, 4)).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void leNombreDeCreneauxDoitDiviserLHeure() {
        new RendezVousService(base, Clock.systemDefaultZone(), 8, 9, 7);
    }

    @Test(expected = MetierException.class)
    public void aucunPatient() {
        patient aucun = null;
        service.proposer(aucun, secretaire, DEMAIN, 9);
    }
}
