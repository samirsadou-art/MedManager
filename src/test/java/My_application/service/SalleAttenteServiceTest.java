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
        salle.terminerConsultation(appele, 1500);
        Passage dix = salle.appelerSuivant(1);
        assertEquals(10, patientDe(dix));
        salle.terminerConsultation(dix, null);
        try {
            salle.appelerSuivant(1);
            fail("le patient sans rendez-vous attend le médecin 2");
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("Aucun patient"));
        }
        assertEquals(12, patientDe(salle.appelerSuivant(2)));
    }

    @Test
    public void uneSeuleConsultationALaFoisParMedecin() {
        Rendez_Vous r = rdv(10, "09:00", 1);
        salle.arrivee(r);
        salle.arriveeSansRendezVous(base.patients.get(11), 1);
        salle.arriveeSansRendezVous(base.patients.get(12), 2);
        Passage enCours = salle.appelerSuivant(1);
        assertEquals(10, patientDe(salle.enConsultation(1)));
        assertNull(salle.enConsultation(2));
        try {
            salle.appelerSuivant(1);
            fail("la consultation de 10 n'est pas terminée");
        } catch (MetierException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("est en cours"));
        }
        Passage suivant = salle.file(1).get(1);
        try {
            salle.appeler(suivant);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("terminez-la"));
        }
        assertEquals(Passage.EN_ATTENTE, suivant.getEtat());
        assertEquals("l'autre médecin appelle librement", 12, patientDe(salle.appelerSuivant(2)));

        long avant = salle.dernierEvenement();
        salle.terminerConsultation(enCours, 2000);
        assertEquals(avant + 1, salle.dernierEvenement());
        assertEquals(Passage.A_REGLER, base.passages.get(0).getEtat());
        assertEquals(Integer.valueOf(2000), base.passages.get(0).getFrais());
        assertNull("toujours au cabinet", base.passages.get(0).getSortieLe());
        assertEquals(Rendez_Vous.ETAT_TERMINE, base.rendezVous.get(0).getEtat());
        assertEquals(Integer.valueOf(2000), salle.fraisProposes(1));
        List<Passage> chezLeMedecin = salle.salleDuMedecin(1);
        assertEquals(2, chezLeMedecin.size());
        assertEquals("il attend sa sortie en bas de la liste du médecin", Passage.A_REGLER,
                chezLeMedecin.get(1).getEtat());
        assertEquals("en tête chez la secrétaire", Passage.A_REGLER, salle.presents(1).get(0).getEtat());
        assertEquals(11, patientDe(salle.appelerSuivant(1)));
        try {
            salle.terminerConsultation(enCours, 2000);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("pas en consultation"));
        }
    }

    @Test
    public void uneFinDeConsultationRateeNeChangeRien() {
        salle.arriveeSansRendezVous(base.patients.get(10), 1);
        Passage p = salle.appelerSuivant(1);
        try {
            salle.terminerConsultation(p, -5);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("montant"));
        }
        Passage vuAilleurs = salle.file(1).get(0);
        salle.terminerConsultation(vuAilleurs, null);
        try {
            salle.terminerConsultation(p, 1000);
            fail();
        } catch (MetierException e) {
            assertEquals(SalleAttenteService.MODIFIE_AILLEURS, e.getMessage());
        }
        assertEquals(Passage.EN_CONSULTATION, p.getEtat());
        assertNull(p.getFrais());
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
    public void laConsultationTermineeLaissePatientARegler() {
        Rendez_Vous r = rdv(10, "09:00", 1);
        salle.arrivee(r);
        salle.appelerSuivant(1);
        assertTrue(SalleAttenteService.cloreConsultation(base, 10, MAINTENANT.plusMinutes(20), 2000));
        assertTrue("il quitte la file du médecin", salle.file(null).isEmpty());
        assertEquals(Rendez_Vous.ETAT_TERMINE, base.rendezVous.get(0).getEtat());
        Passage p = salle.presents(null).get(0);
        assertEquals("il reste au cabinet, à régler", Passage.A_REGLER, p.getEtat());
        assertEquals(Integer.valueOf(2000), p.getFrais());
        assertNull(p.getSortieLe());
        assertFalse("pas de second passage à clore",
                SalleAttenteService.cloreConsultation(base, 10, MAINTENANT, 2000));
        assertFalse(SalleAttenteService.cloreConsultation(base, 11, MAINTENANT, null));
        try {
            salle.arriveeSansRendezVous(base.patients.get(10), 1);
            fail("pas pointé deux fois avant d'être libéré");
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("déjà dans la salle d'attente"));
        }
    }

    @Test
    public void laSecretaireEncaisseEtLibereLePatient() {
        salle.arriveeSansRendezVous(base.patients.get(10), 1);
        salle.arriveeSansRendezVous(base.patients.get(11), 1);
        salle.appelerSuivant(1);
        SalleAttenteService.cloreConsultation(base, 10, MAINTENANT, 1500);
        List<Passage> accueil = salle.presents(null);
        assertEquals("le patient à régler vient en tête", Passage.A_REGLER, accueil.get(0).getEtat());
        assertEquals(2, accueil.size());
        try {
            salle.liberer(accueil.get(1), 1500);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("terminée"));
        }
        long avant = salle.dernierEvenement();
        salle.liberer(accueil.get(0), 1000);
        Passage lu = base.passages.get(0);
        assertEquals(Passage.TERMINE, lu.getEtat());
        assertEquals(Integer.valueOf(1000), lu.getMontantRegle());
        assertEquals(MAINTENANT, lu.getRegleLe());
        assertEquals(MAINTENANT, lu.getSortieLe());
        assertEquals("les autres postes sont prévenus", avant + 1, salle.dernierEvenement());
        assertEquals(1, salle.presents(null).size());
        // Libéré, il peut revenir dans la journée.
        assertNotNull(salle.arriveeSansRendezVous(base.patients.get(10), 1));
        try {
            salle.liberer(lu, 1000);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("terminée"));
        }
    }

    @Test
    public void unPatientLibereSansPaiement() {
        salle.arriveeSansRendezVous(base.patients.get(10), 1);
        SalleAttenteService.cloreConsultation(base, 10, MAINTENANT, null);
        Passage p = salle.presents(1).get(0);
        assertNull(p.getFrais());
        salle.liberer(p, null);
        assertNull(base.passages.get(0).getMontantRegle());
        assertNull(base.passages.get(0).getRegleLe());
        assertNotNull(base.passages.get(0).getSortieLe());
    }

    @Test
    public void unPassageModifieAilleursNEstPasLibere() {
        salle.arriveeSansRendezVous(base.patients.get(10), 1);
        SalleAttenteService.cloreConsultation(base, 10, MAINTENANT, 800);
        Passage ici = salle.presents(null).get(0);
        salle.liberer(salle.presents(null).get(0), 800); // sur un autre poste
        try {
            salle.liberer(ici, 800);
            fail();
        } catch (MetierException e) {
            assertEquals(SalleAttenteService.MODIFIE_AILLEURS, e.getMessage());
        }
        assertEquals("l'objet affiché n'est pas modifié", Passage.A_REGLER, ici.getEtat());
        assertNull(ici.getMontantRegle());
    }

    @Test
    public void lesMontantsSaisis() {
        assertNull(SalleAttenteService.montant(null));
        assertNull(SalleAttenteService.montant("  "));
        assertEquals(Integer.valueOf(2000), SalleAttenteService.montant("2000"));
        assertEquals(Integer.valueOf(2500), SalleAttenteService.montant("2 500 DA"));
        assertEquals(Integer.valueOf(0), SalleAttenteService.montant("0"));
        for (String faux : new String[] {"-5", "12,50", "deux mille", "2000000"}) {
            try {
                SalleAttenteService.montant(faux);
                fail(faux);
            } catch (MetierException attendue) {
                assertTrue(attendue.getMessage().contains("ontant"));
            }
        }
        try {
            SalleAttenteService.cloreConsultation(base, 10, MAINTENANT, -1);
            fail();
        } catch (MetierException attendue) {
            assertTrue(attendue.getMessage().contains("compris entre"));
        }
    }

    @Test
    public void lesDerniersFraisDuMedecinSontProposes() {
        assertNull(SalleAttenteService.dernierFrais(base, 1));
        salle.arriveeSansRendezVous(base.patients.get(10), 1);
        SalleAttenteService.cloreConsultation(base, 10, MAINTENANT, 1500);
        salle.arriveeSansRendezVous(base.patients.get(11), 1);
        SalleAttenteService.cloreConsultation(base, 11, MAINTENANT, 2000);
        salle.arriveeSansRendezVous(base.patients.get(12), 1);
        SalleAttenteService.cloreConsultation(base, 12, MAINTENANT, null);
        assertEquals(Integer.valueOf(2000), SalleAttenteService.dernierFrais(base, 1));
        assertNull(SalleAttenteService.dernierFrais(base, 2));
        assertFalse(SalleAttenteService.estDansLaFile(base, 13, AUJOURDHUI));
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
        SalleAttenteService.cloreConsultation(base, 11, MAINTENANT, 2000);
        salle.partiSansConsultation(salle.file(1).get(0));
        assertEquals("seul le rendez-vous passé et non pointé", 1, salle.finDeJournee());
        SalleAttenteService.Bilan b = salle.bilan();
        assertEquals(1, b.getVus());
        assertEquals(1, b.getARegler());
        assertEquals(0, b.getEncaisse());
        salle.liberer(salle.presents(null).get(0), 2000);
        b = salle.bilan();
        assertEquals(1, b.getVus());
        assertEquals(0, b.getARegler());
        assertEquals(2000, b.getEncaisse());
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
