package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class SalleAttenteServiceTest {

    /** « Maintenant » figé : jeudi 8 octobre 2026, 10 h 20. */
    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 8, 10, 20);
    private static final LocalDate AUJOURDHUI = MAINTENANT.toLocalDate();

    private MemoireDAOFactory base;
    private RendezVousService rendezVous;
    private SalleAttenteService salle;
    private Clock horloge;

    @Before
    public void preparer() {
        base = Donnees.base();
        ZoneId zone = ZoneId.systemDefault();
        horloge = Clock.fixed(MAINTENANT.atZone(zone).toInstant(), zone);
        rendezVous = new RendezVousService(base, horloge, 8, 9, 4);
        salle = new SalleAttenteService(base, rendezVous, ControleAcces.AUCUN, horloge);
        base.medecins.put(2, Donnees.medecin(2));
        for (int i = 10; i < 20; i++) {
            base.patients.put(i, Donnees.patient(i, "PATIENT" + i));
        }
    }

    /** Rendez-vous du jour enregistré directement (l'heure peut être passée). */
    private Rendez_Vous rdv(int idPatient, String heure, int idMedecin) {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(base.patients.get(idPatient));
        r.setUtilisateur(Donnees.utilisateur(1));
        r.setDate(AUJOURDHUI);
        r.setHeure(LocalTime.parse(heure));
        r.setIdMedecin(idMedecin);
        base.getRendezVousDao().insert(r);
        return r;
    }

    private static int patientDe(Passage p) {
        return p.getPatient().getIdentPat();
    }

    @Test
    public void laFileSuitLHeurePrevuePuisLArriveeDesPatientsSansRendezVous() {
        Rendez_Vous neufHeuresTrente = rdv(10, "09:30", 1);
        Rendez_Vous neufHeures = rdv(11, "09:00", 1);
        salle.arrivee(neufHeuresTrente);
        salle.arriveeSansRendezVous(base.patients.get(12), 1);
        salle.arrivee(neufHeures);
        assertEquals(Rendez_Vous.ETAT_ARRIVE, neufHeures.getEtat());
        assertEquals(Rendez_Vous.ETAT_ARRIVE, base.rendezVous.get(1).getEtat());
        List<Passage> file = salle.file(1);
        assertEquals(Arrays.asList(11, 10, 12), Arrays.asList(patientDe(file.get(0)), patientDe(file.get(1)),
                patientDe(file.get(2))));
        assertTrue(file.get(2).isSansRendezVous());
        assertEquals(LocalTime.of(9, 0), file.get(0).getHeurePrevue());
        assertEquals("chaque arrivée prévient les autres postes", 3, salle.dernierEvenement());
    }

    @Test
    public void uneUrgencePasseEnTeteDeLaFile() {
        salle.arrivee(rdv(10, "09:00", 1));
        Passage urgent = salle.arriveeSansRendezVous(base.patients.get(12), 1);
        salle.urgence(urgent, true);
        assertEquals(12, patientDe(salle.file(1).get(0)));
        salle.urgence(salle.file(1).get(0), false);
        assertEquals(10, patientDe(salle.file(1).get(0)));
    }

    @Test
    public void unPatientNEstQuUneFoisDansLaSalle() {
        Rendez_Vous r = rdv(10, "09:00", 1);
        salle.arrivee(r);
        try {
            salle.arrivee(r);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("déjà pointé"));
        }
        try {
            salle.arriveeSansRendezVous(base.patients.get(10), 2);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("déjà dans la salle d'attente"));
        }
        assertEquals(1, base.passages.size());
    }

    @Test
    public void seulUnRendezVousDuJourPeutEtrePointe() {
        Rendez_Vous demain = rendezVous.proposer(base.patients.get(10), Donnees.utilisateur(1),
                AUJOURDHUI.plusDays(1), 9);
        rendezVous.confirmer(demain);
        try {
            salle.arrivee(demain);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("aujourd'hui"));
        }
    }

    @Test
    public void unPatientEnRetardMarqueAbsentPeutEncoreArriver() {
        Rendez_Vous r = rdv(10, "09:00", 1);
        rendezVous.marquerAbsent(r);
        salle.arrivee(r);
        assertEquals(Rendez_Vous.ETAT_ARRIVE, r.getEtat());
    }

    @Test
    public void lePatientSansRendezVousVaChezLeMedecinChoisi() {
        RendezVousService accueil = new RendezVousService(base, horloge, 8, 9, 4, JournalAcces.AUCUN, () -> null);
        SalleAttenteService secretaire = new SalleAttenteService(base, accueil, ControleAcces.AUCUN, horloge);
        try {
            secretaire.arriveeSansRendezVous(base.patients.get(10), null);
            fail("deux médecins : il faut choisir");
        } catch (MetierException e) {
            assertEquals(RendezVousService.MEDECIN_A_CHOISIR, e.getMessage());
        }
        assertEquals(2, secretaire.arriveeSansRendezVous(base.patients.get(10), 2).getIdMedecin());
        assertTrue(secretaire.file(1).isEmpty());
        assertEquals(1, secretaire.file(2).size());
        assertEquals(1, secretaire.file(null).size());
    }

    @Test
    public void leMedecinAppelleLePatientSuivantDeSaFile() {
        Rendez_Vous r = rdv(11, "09:00", 1);
        salle.arrivee(rdv(10, "09:30", 1));
        salle.arrivee(r);
        salle.arriveeSansRendezVous(base.patients.get(12), 2);
        Passage appele = salle.appelerSuivant(null);
        assertEquals(11, patientDe(appele));
        assertEquals(Passage.EN_CONSULTATION, appele.getEtat());
        assertEquals(MAINTENANT, appele.getAppeleLe());
        assertEquals(Rendez_Vous.ETAT_EN_CONSULTATION, base.rendezVous.get(0).getEtat());
        List<Passage> file = salle.file(1);
        assertEquals("le patient en consultation reste affiché en tête", Passage.EN_CONSULTATION,
                file.get(0).getEtat());
        assertEquals(10, patientDe(salle.appelerSuivant(1)));
        try {
            salle.appelerSuivant(1);
            fail("le patient sans rendez-vous attend le médecin 2");
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("Aucun patient"));
        }
        assertEquals(12, patientDe(salle.appelerSuivant(2)));
    }

    @Test
    public void unAppelDejaFaitSurUnAutrePosteEstSignale() {
        salle.arriveeSansRendezVous(base.patients.get(10), 1);
        Passage vuIci = salle.file(1).get(0);
        Passage vuAilleurs = salle.file(1).get(0);
        salle.appeler(vuAilleurs);
        try {
            salle.appeler(vuIci);
            fail();
        } catch (MetierException e) {
            assertEquals(SalleAttenteService.MODIFIE_AILLEURS, e.getMessage());
        }
        assertEquals("l'objet affiché n'est pas modifié", Passage.EN_ATTENTE, vuIci.getEtat());
        assertNull(vuIci.getAppeleLe());
    }

    @Test
    public void laConsultationTermineLePassageEtLeRendezVous() {
        Rendez_Vous r = rdv(10, "09:00", 1);
        salle.arrivee(r);
        salle.appelerSuivant(1);
        assertTrue(SalleAttenteService.cloreConsultation(base, 10, MAINTENANT.plusMinutes(20)));
        assertTrue(salle.file(null).isEmpty());
        assertEquals(Rendez_Vous.ETAT_TERMINE, base.rendezVous.get(0).getEtat());
        assertFalse("pas de second passage à clore", SalleAttenteService.cloreConsultation(base, 10, MAINTENANT));
        assertFalse(SalleAttenteService.cloreConsultation(base, 11, MAINTENANT));
    }

    @Test
    public void unPatientRepartiSansEtreVu() {
        Rendez_Vous r = rdv(10, "09:00", 1);
        salle.arrivee(r);
        Passage p = salle.file(1).get(0);
        salle.partiSansConsultation(p);
        assertEquals(Passage.PARTI, base.passages.get(0).getEtat());
        assertEquals(Rendez_Vous.ETAT_ABSENT, base.rendezVous.get(0).getEtat());
        assertTrue(salle.file(1).isEmpty());
        try {
            salle.partiSansConsultation(p);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("attend"));
        }
    }

    @Test
    public void finDeJourneeEtBilan() {
        rdv(10, "09:00", 1); // jamais venu
        Rendez_Vous vu = rdv(11, "09:15", 1);
        Rendez_Vous parti = rdv(12, "09:30", 1);
        rendezVous.confirmer(rendezVous.proposer(base.patients.get(13), Donnees.utilisateur(1), AUJOURDHUI, 11));
        salle.arrivee(vu);
        salle.arrivee(parti);
        salle.arriveeSansRendezVous(base.patients.get(14), 1);
        salle.appelerSuivant(1);
        SalleAttenteService.cloreConsultation(base, 11, MAINTENANT);
        salle.partiSansConsultation(salle.file(1).get(0));
        assertEquals("seul le rendez-vous passé et non pointé", 1, salle.finDeJournee());
        SalleAttenteService.Bilan b = salle.bilan();
        assertEquals(1, b.getVus());
        assertEquals(1, b.getPartis());
        assertEquals(1, b.getEnAttente());
        assertEquals(1, b.getSansRendezVous());
        assertEquals("le non venu et le parti", 2, b.getAbsents());
        assertEquals(0, b.getAttenteMoyenne());
    }

    @Test
    public void attenteEnMinutes() {
        Passage p = new Passage();
        p.setArriveeLe(MAINTENANT.minusMinutes(42));
        assertEquals(42, p.minutesDAttente(MAINTENANT));
        p.setAppeleLe(MAINTENANT.minusMinutes(10));
        assertEquals(32, p.minutesDAttente(MAINTENANT));
    }

    @Test
    public void laSecretaireNAppellePasLesPatients() {
        ControleAcces secretaire = roles -> {
            if (!Arrays.asList(roles).contains(Role.SECRETAIRE)) {
                throw new MetierException("Réservé aux médecins.");
            }
        };
        SalleAttenteService s = new SalleAttenteService(base, rendezVous, secretaire, horloge);
        assertNotNull(s.arriveeSansRendezVous(base.patients.get(10), 1));
        try {
            s.appelerSuivant(1);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("médecins"));
        }
    }

    @Test
    public void lesEvenementsAnciensSontPurges() {
        assertEquals(0, salle.purgerEvenements());
    }
}
