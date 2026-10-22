package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.dao.DoublonException;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.medecin;
import My_application.entities.patient;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
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
        assertEquals(LocalTime.of(9, 0), prendre(10, DEMAIN, 9).getHeure());
        assertEquals(LocalTime.of(9, 15), prendre(11, DEMAIN, 9).getHeure());
        assertEquals(LocalTime.of(9, 30), prendre(12, DEMAIN, 9).getHeure());
        assertEquals(LocalTime.of(9, 45), prendre(13, DEMAIN, 9).getHeure());
    }

    @Test
    public void leRendezVousEstPrisAvecLeMedecinDuPoste() {
        assertEquals(Integer.valueOf(RendezVousService.MEDECIN_PAR_DEFAUT), prendre(10, DEMAIN, 9).getIdMedecin());
    }

    @Test
    public void deuxMedecinsRecoiventAuMemeCreneau() {
        int[] medecin = {1};
        ZoneId zone = ZoneId.systemDefault();
        RendezVousService parMedecin = new RendezVousService(base,
                Clock.fixed(MAINTENANT.atZone(zone).toInstant(), zone), 8, 9, 4, JournalAcces.AUCUN,
                () -> medecin[0]);
        Rendez_Vous r1 = parMedecin.proposer(base.patients.get(10), secretaire, DEMAIN, 9);
        parMedecin.confirmer(r1);
        medecin[0] = 2;
        Rendez_Vous r2 = parMedecin.proposer(base.patients.get(11), secretaire, DEMAIN, 9);
        parMedecin.confirmer(r2);
        assertEquals("créneau libre chez le médecin 2", LocalTime.of(9, 0), r2.getHeure());
        assertEquals(Integer.valueOf(2), r2.getIdMedecin());
        try {
            parMedecin.proposer(base.patients.get(10), secretaire, DEMAIN, 9);
            fail("un patient n'a qu'un rendez-vous par heure, tous médecins confondus");
        } catch (MetierException attendu) {
            assertTrue(attendu.getMessage().contains("par heure"));
        }
    }

    @Test
    public void listeDesMedecinsDuCabinetEtMedecinDuPoste() {
        medecin m = Donnees.medecin(2);
        m.setNom("AMRANI");
        base.medecins.put(2, m);
        assertEquals(2, service.medecins().size());
        assertEquals("AMRANI", service.medecins().get(0).getNom());
        assertEquals(Integer.valueOf(RendezVousService.MEDECIN_PAR_DEFAUT), service.getIdMedecinCourant());
    }

    @Test
    public void unRendezVousSansMedecinOccupeLeCreneauPourTous() {
        Rendez_Vous ancien = prendre(10, DEMAIN, 9);
        ancien.setIdMedecin(null);
        assertEquals(LocalTime.of(9, 15), prendre(11, DEMAIN, 9).getHeure());
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
        service.annuler(quart);
        assertEquals(LocalTime.of(9, 15), prendre(13, DEMAIN, 9).getHeure());
    }

    @Test(expected = MetierException.class)
    public void unPatientNaQuUnRendezVousParHeure() {
        prendre(10, DEMAIN, 9);
        prendre(10, DEMAIN, 9);
    }

    @Test
    public void lesCreneauxDejaPassesSontIgnores() {
        // Il est 10 h 20 : dans l'heure de 10 h, seul 10 h 30 ou 10 h 45 sont possibles.
        assertEquals(LocalTime.of(10, 30), prendre(10, MAINTENANT.toLocalDate(), 10).getHeure());
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
    public void annulerGardeLHistoriqueEtLibereLeCreneau() {
        Rendez_Vous a = prendre(10, DEMAIN, 9);
        prendre(11, DEMAIN, 9);
        assertTrue(service.peutEtreAnnule(a));
        service.annuler(a);
        assertEquals("rien n'est supprimé", 2, base.rendezVous.size());
        assertEquals(Rendez_Vous.ETAT_ANNULE, a.getEtat());
        assertFalse(service.peutEtreAnnule(a));
        List<Rendez_Vous> jour = service.duJour(DEMAIN);
        assertEquals("l'agenda ne montre plus l'annulé", 1, jour.size());
        assertEquals(11, jour.get(0).getPatient().getIdentPat());
        // Le créneau de 9 h est de nouveau libre, même pour le même patient.
        assertEquals(LocalTime.of(9, 0), prendre(10, DEMAIN, 9).getHeure());
    }

    @Test
    public void unRendezVousPasseNePeutPasEtreAnnule() {
        Rendez_Vous hier = ancien(10, MAINTENANT.toLocalDate().minusDays(1), "09:00");
        assertFalse(service.peutEtreAnnule(hier));
        try {
            service.annuler(hier);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("à venir"));
        }
    }

    @Test
    public void unRendezVousModifieSurUnAutrePosteEstSignale() {
        Rendez_Vous a = prendre(10, DEMAIN, 9);
        Rendez_Vous copie = copie(a);
        service.annuler(a); // premier poste
        try {
            service.annuler(copie); // second poste, avec son affichage périmé
            fail();
        } catch (MetierException e) {
            assertEquals(RendezVousService.MODIFIE_AILLEURS, e.getMessage());
        }
    }

    // ----- absent -----

    /** Rendez-vous enregistré directement (date passée, que le service refuserait). */
    private Rendez_Vous ancien(int idPatient, LocalDate jour, String heure) {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(base.patients.get(idPatient));
        r.setUtilisateur(secretaire);
        r.setDate(jour);
        r.setHeure(LocalTime.parse(heure));
        r.setIdMedecin(1);
        base.getRendezVousDao().insert(r);
        return r;
    }

    private static Rendez_Vous copie(Rendez_Vous r) {
        Rendez_Vous c = new Rendez_Vous();
        c.setIdRendezVous(r.getIdRendezVous());
        c.setPatient(r.getPatient());
        c.setUtilisateur(r.getUtilisateur());
        c.setDate(r.getDate());
        c.setHeure(r.getHeure());
        c.setIdMedecin(r.getIdMedecin());
        c.setEtat(r.getEtat());
        return c;
    }

    @Test
    public void unRendezVousPasseNonPointePeutEtreMarqueAbsent() {
        Rendez_Vous ceMatin = ancien(10, MAINTENANT.toLocalDate(), "09:15");
        Rendez_Vous demain = prendre(11, DEMAIN, 9);
        assertTrue(service.peutEtreMarqueAbsent(ceMatin));
        assertFalse("un rendez-vous à venir n'est pas encore manqué", service.peutEtreMarqueAbsent(demain));
        service.marquerAbsent(ceMatin);
        assertEquals(Rendez_Vous.ETAT_ABSENT, ceMatin.getEtat());
        assertFalse(service.peutEtreMarqueAbsent(ceMatin));
        assertEquals("l'absent reste visible dans l'agenda", 1, service.duJour(MAINTENANT.toLocalDate()).size());
        try {
            service.marquerAbsent(demain);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("passée"));
        }
    }

    // ----- déplacement -----

    @Test
    public void deplacerVersUneAutreHeure() {
        Rendez_Vous r = prendre(10, DEMAIN, 9);
        prendre(11, DEMAIN, 11);
        Rendez_Vous n = service.proposerDeplacement(r, DEMAIN, 11, null);
        assertEquals(LocalTime.of(11, 15), n.getHeure());
        assertEquals(r.getIdRendezVous(), n.getIdRendezVous());
        service.deplacer(r, n);
        assertEquals(1, base.rendezVous.stream().filter(x -> x.getHeure().equals(LocalTime.of(11, 15))).count());
        assertEquals(2, base.rendezVous.size());
        // L'ancien créneau de 9 h est libre.
        assertEquals(LocalTime.of(9, 0), prendre(12, DEMAIN, 9).getHeure());
    }

    @Test
    public void deplacerDansLaMemeHeureNeCompteQuUneFoisLeRendezVous() {
        Rendez_Vous r = prendre(10, DEMAIN, 9);
        Rendez_Vous n = service.proposerDeplacement(r, DEMAIN.plusDays(1), 9, null);
        assertEquals(LocalTime.of(9, 0), n.getHeure());
        // Même heure, même jour : son propre créneau ne l'empêche pas.
        assertEquals(LocalTime.of(9, 0), service.proposerDeplacement(r, DEMAIN, 9, null).getHeure());
    }

    @Test
    public void deplacerChezUnAutreMedecin() {
        Rendez_Vous r = prendre(10, DEMAIN, 9);
        Rendez_Vous n = service.proposerDeplacement(r, DEMAIN, 9, 2);
        service.deplacer(r, n);
        assertEquals(Integer.valueOf(2), r.getIdMedecin());
    }

    @Test
    public void deplacerVersUneHeureCompleteOuDejaPriseParLePatientEstRefuse() {
        Rendez_Vous r = prendre(10, DEMAIN, 9);
        for (int i = 11; i < 15; i++) {
            prendre(i, DEMAIN, 10);
        }
        try {
            service.proposerDeplacement(r, DEMAIN, 10, null);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("complet"));
        }
        prendre(10, DEMAIN, 11);
        try {
            service.proposerDeplacement(r, DEMAIN, 11, null);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("par heure"));
        }
    }

    @Test
    public void unCreneauPrisEntreTempsBloqueLeDeplacement() {
        Rendez_Vous r = prendre(10, DEMAIN, 9);
        Rendez_Vous n = service.proposerDeplacement(r, DEMAIN, 11, null);
        prendre(11, DEMAIN, 11); // un autre poste prend 11 h entre-temps
        try {
            service.deplacer(r, n);
            fail();
        } catch (MetierException e) {
            assertEquals(RendezVousService.CRENEAU_PRIS, e.getMessage());
        }
        assertEquals(LocalTime.of(9, 0), r.getHeure());
        assertEquals(1, base.rollbacks);
    }

    @Test
    public void unRendezVousAnnuleNePeutPlusEtreDeplace() {
        Rendez_Vous r = prendre(10, DEMAIN, 9);
        Rendez_Vous affiche = copie(r); // affichage périmé du premier poste
        Rendez_Vous n = service.proposerDeplacement(affiche, DEMAIN, 11, null);
        service.annuler(r); // un autre poste annule entre-temps
        try {
            service.deplacer(affiche, n);
            fail();
        } catch (MetierException e) {
            assertEquals(RendezVousService.MODIFIE_AILLEURS, e.getMessage());
        }
    }

    // ----- médecin obligatoire pour un poste sans médecin -----

    @Test
    public void unPosteSansMedecinDoitLeChoisirSiLeCabinetEnACourant() {
        ZoneId zone = ZoneId.systemDefault();
        RendezVousService accueil = new RendezVousService(base,
                Clock.fixed(MAINTENANT.atZone(zone).toInstant(), zone), 8, 9, 4, JournalAcces.AUCUN, () -> null);
        // Un seul médecin : il est choisi d'office.
        assertEquals(Integer.valueOf(1), accueil.proposer(base.patients.get(10), secretaire, DEMAIN, 9).getIdMedecin());
        base.medecins.put(2, Donnees.medecin(2));
        try {
            accueil.proposer(base.patients.get(10), secretaire, DEMAIN, 9);
            fail();
        } catch (MetierException e) {
            assertEquals(RendezVousService.MEDECIN_A_CHOISIR, e.getMessage());
        }
        Rendez_Vous r = accueil.proposer(base.patients.get(10), secretaire, DEMAIN, 9, 2);
        assertEquals(Integer.valueOf(2), r.getIdMedecin());
        accueil.confirmer(r);
        assertEquals(1, base.rendezVous.size());
    }

    // ----- motif, recherche, liste du jour -----

    @Test
    public void leMotifEstNettoyeEtLimite() {
        Rendez_Vous r = service.proposer(base.patients.get(10), secretaire, DEMAIN, 9);
        r.setMotif("  Contrôle   tension ");
        service.confirmer(r);
        assertEquals("Contrôle tension", r.getMotif());
        assertNull(RendezVousService.motif("   "));
        Rendez_Vous long_ = service.proposer(base.patients.get(11), secretaire, DEMAIN, 9);
        long_.setMotif(new String(new char[81]).replace('\0', 'x'));
        try {
            service.confirmer(long_);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("80"));
        }
    }

    @Test
    public void rechercheDesRendezVousDUnPatient() {
        base.patients.put(30, Donnees.patient(30, "ZIANI"));
        Rendez_Vous a = prendre(30, DEMAIN, 9);
        prendre(30, DEMAIN.plusDays(7), 10);
        prendre(10, DEMAIN, 10);
        service.annuler(a);
        List<Rendez_Vous> l = service.rechercher("zia");
        assertEquals("annulés compris", 2, l.size());
        assertEquals("du plus récent au plus ancien", DEMAIN.plusDays(7), l.get(0).getDate());
        assertTrue(service.rechercher("  ").isEmpty());
        assertTrue(service.rechercher("INCONNU").isEmpty());
    }

    @Test
    public void listeDuJourParMedecinPuisParHeure() {
        prendre(10, DEMAIN, 11);
        Rendez_Vous annule = prendre(11, DEMAIN, 9);
        service.annuler(annule);
        prendre(12, DEMAIN, 10);
        Rendez_Vous chez2 = service.proposer(base.patients.get(13), secretaire, DEMAIN, 8, 2);
        service.confirmer(chez2);
        List<Rendez_Vous> l = service.listeDuJour(DEMAIN, null);
        assertEquals(3, l.size());
        assertEquals(LocalTime.of(10, 0), l.get(0).getHeure());
        assertEquals(LocalTime.of(11, 0), l.get(1).getHeure());
        assertEquals(Integer.valueOf(2), l.get(2).getIdMedecin());
        assertEquals(1, service.listeDuJour(DEMAIN, 2).size());
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
