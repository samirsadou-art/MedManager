package My_application.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import My_application.service.DossierService;
import My_application.service.PatientService;
import My_application.service.RendezVousService;
import My_application.service.SalleAttenteService;
import My_application.service.Session;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/** Accueil et liste des patients côté salle d'attente, avec de fausses vues. */
public class SalleAttenteControllerTest {

    private MemoireDAOFactory base;
    private RendezVousService rendezVous;
    private SalleAttenteService salle;
    private DossierService dossiers;

    @Before
    public void preparer() {
        base = Donnees.base();
        base.medecins.put(2, Donnees.medecin(2));
        rendezVous = new RendezVousService(base);
        salle = new SalleAttenteService(base, rendezVous);
        dossiers = new DossierService(base);
    }

    private ControleursTest.FausseAccueil vue() {
        return new ControleursTest.FausseAccueil();
    }

    private AccueilController accueil(ControleursTest.FausseAccueil vue, Integer medecinDeLaFile,
            boolean annoncer) {
        return new AccueilController(vue, dossiers, rendezVous, Executeur.IMMEDIAT, true)
                .brancherSalleAttente(salle, medecinDeLaFile, () -> base.medecins.get(1), annoncer);
    }

    /** Rendez-vous d'aujourd'hui, enregistré directement (l'heure peut être passée). */
    private Rendez_Vous rdv(int idPatient, String heure, int idMedecin) {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(base.patients.get(idPatient));
        r.setUtilisateur(Donnees.utilisateur(1));
        r.setDate(LocalDate.now());
        r.setHeure(LocalTime.parse(heure));
        r.setIdMedecin(idMedecin);
        base.getRendezVousDao().insert(r);
        return r;
    }

    @Test
    public void laSecretairePointeUneArriveeEtLaFileSAffiche() {
        ControleursTest.FausseAccueil vue = vue();
        AccueilController c = accueil(vue, null, true);
        Rendez_Vous r = rdv(1, "08:00", 2);
        assertTrue(c.peutPointer(r));
        c.arrivee(r);
        assertTrue(vue.erreurs.isEmpty());
        assertEquals(1, vue.file.size());
        assertEquals("Dr SAHAB Said", vue.medecins.get(2));
        assertFalse("déjà pointé", c.peutPointer(r));
        assertFalse(c.peutPointer(null));

        c.arrivee(r);
        assertEquals(Collections.singletonList("Ce patient est déjà pointé."), vue.erreurs);
    }

    @Test
    public void unRendezVousPasseNonHonoreEstMarqueAbsent() {
        ControleursTest.FausseAccueil vue = vue();
        AccueilController c = accueil(vue, null, true);
        Rendez_Vous r = rdv(1, "00:00", 1);
        assertTrue(c.peutMarquerAbsent(r));
        c.absent(r);
        assertEquals(Rendez_Vous.ETAT_ABSENT, base.rendezVous.get(0).getEtat());
        assertFalse(c.peutMarquerAbsent(null));
    }

    @Test
    public void leTimerNeRelitQueSiUnPosteAChangeQuelqueChose() {
        ControleursTest.FausseAccueil vue = vue();
        AccueilController c = accueil(vue, null, false);
        c.rafraichir();
        assertEquals(1, vue.affichagesSalle);
        c.verifierEvenements();
        assertEquals("rien n'a changé", 1, vue.affichagesSalle);

        // Un autre poste met un patient en salle d'attente.
        salle.arriveeSansRendezVous(base.patients.get(2), 1);
        c.verifierEvenements();
        assertEquals(2, vue.affichagesSalle);
        assertEquals(1, vue.file.size());
    }

    @Test
    public void unePanneDuTimerNeMontreAucunMessage() {
        ControleursTest.FausseAccueil vue = vue();
        AccueilController c = accueil(vue, null, false);
        base.echouerSur("evenement.dernier");
        c.verifierEvenements();
        assertTrue(vue.erreurs.isEmpty());
        assertEquals(0, vue.affichagesSalle);
        c.verifierEvenements();
        assertEquals("le tour suivant réussit et recharge", 1, vue.affichagesSalle);
    }

    @Test
    public void sansSalleDAttenteLeTimerNeFaitRien() {
        ControleursTest.FausseAccueil vue = vue();
        AccueilController c = new AccueilController(vue, dossiers, rendezVous, Executeur.IMMEDIAT);
        c.verifierEvenements();
        c.purgerEvenements();
        assertFalse(c.peutPointer(rdv(1, "08:00", 1)));
        assertEquals(0, vue.affichagesSalle);
        assertNull(vue.file);
    }

    @Test
    public void laSecretaireEstPreveneQuandUnMedecinAppelle() {
        ControleursTest.FausseAccueil secretaire = vue();
        AccueilController accueilSecretaire = accueil(secretaire, null, true);
        salle.arriveeSansRendezVous(base.patients.get(1), 1);
        accueilSecretaire.rafraichir();
        assertTrue("rien à annoncer au premier affichage", secretaire.annonces.isEmpty());

        ControleursTest.FausseAccueil medecin = vue();
        AccueilController accueilMedecin = accueil(medecin, 1, false);
        accueilMedecin.appelerSuivant();
        assertEquals("le dossier du patient appelé s'ouvre", 1, medecin.dossiers.size());
        assertEquals(1, medecin.dossiers.get(0).getMalade().getIdentPat());
        assertTrue(medecin.annonces.isEmpty());

        accueilSecretaire.verifierEvenements();
        assertEquals(1, secretaire.annonces.size());
        assertTrue(secretaire.annonces.get(0), secretaire.annonces.get(0).startsWith("AMRANI"));
        assertTrue(secretaire.annonces.get(0).contains("Dr SAHAB Said"));
        accueilSecretaire.rafraichir();
        assertEquals("annoncé une seule fois", 1, secretaire.annonces.size());

        accueilMedecin.appelerSuivant();
        assertEquals(Collections.singletonList("Aucun patient n'attend dans votre file."), medecin.erreurs);
    }

    @Test
    public void leMedecinAppelleUnPatientChoisiPuisReprendSonDossier() {
        ControleursTest.FausseAccueil vue = vue();
        AccueilController c = accueil(vue, 1, false);
        salle.arriveeSansRendezVous(base.patients.get(1), 1);
        Passage p = salle.arriveeSansRendezVous(base.patients.get(2), 1);
        c.appeler(p);
        assertEquals(Passage.EN_CONSULTATION, p.getEtat());
        assertEquals(1, vue.dossiers.size());
        c.appeler(p);
        assertEquals("reprend le dossier sans rappeler", 2, vue.dossiers.size());
        assertTrue(vue.erreurs.isEmpty());
        c.appeler(null);
        assertEquals(2, vue.dossiers.size());
    }

    @Test
    public void laSecretaireNePeutPasAppeler() {
        Session session = new Session(base, 1, 1);
        User u = Donnees.utilisateur(5);
        u.setRole(Role.SECRETAIRE);
        session.ouvrir(u);
        salle = new SalleAttenteService(base, rendezVous, session, java.time.Clock.systemDefaultZone());
        ControleursTest.FausseAccueil vue = vue();
        AccueilController c = accueil(vue, null, true);
        c.arrivee(rdv(1, "08:00", 1));
        c.appelerSuivant();
        assertEquals(1, vue.erreurs.size());
        assertTrue(vue.erreurs.get(0).contains("médecin"));
        assertTrue(vue.dossiers.isEmpty());
    }

    @Test
    public void urgenceEtDepartSansConsultation() {
        ControleursTest.FausseAccueil vue = vue();
        AccueilController c = accueil(vue, null, true);
        salle.arriveeSansRendezVous(base.patients.get(1), 1);
        Passage p = salle.arriveeSansRendezVous(base.patients.get(2), 1);
        c.urgence(p);
        assertTrue(p.isUrgence());
        assertEquals("l'urgence passe en tête", 2, vue.file.get(0).getPatient().getIdentPat());
        c.urgence(p);
        assertFalse(p.isUrgence());
        c.parti(p);
        assertEquals(Passage.PARTI, p.getEtat());
        assertEquals(1, vue.file.size());
        c.urgence(null);
        c.parti(null);
        c.urgence(p);
        assertEquals(1, vue.erreurs.size());
    }

    @Test
    public void laSecretaireEncaisseEtLibereLePatientConsulte() {
        ControleursTest.FausseAccueil secretaire = vue();
        AccueilController c = new AccueilController(secretaire, dossiers, rendezVous, Executeur.IMMEDIAT, false)
                .brancherSalleAttente(salle, null, () -> base.medecins.get(1), true, true);
        salle.arriveeSansRendezVous(base.patients.get(1), 1);
        c.rafraichir();
        ControleursTest.FausseAccueil medecin = vue();
        AccueilController accueilMedecin = accueil(medecin, 1, false);
        accueilMedecin.appelerSuivant();
        dossiers.nouvelleConsultation(medecin.dossiers.get(0));
        dossiers.enregistrer(medecin.dossiers.get(0), 2000);

        accueilMedecin.rafraichir();
        assertTrue("le médecin ne le voit plus", medecin.file.isEmpty());
        c.verifierEvenements();
        assertEquals(1, secretaire.file.size());
        Passage p = secretaire.file.get(0);
        assertEquals(Passage.A_REGLER, p.getEtat());
        String annonce = secretaire.annonces.get(secretaire.annonces.size() - 1);
        assertTrue(annonce, annonce.contains("a terminé sa consultation : 2 000 DA à régler"));

        assertFalse("montant invalide", c.liberer(p, "beaucoup"));
        assertEquals(1, secretaire.erreurs.size());
        assertEquals(Passage.A_REGLER, base.passages.get(0).getEtat());
        assertTrue(c.liberer(p, "2000"));
        assertTrue(secretaire.file.isEmpty());
        assertEquals(Integer.valueOf(2000), base.passages.get(0).getMontantRegle());
        assertFalse(c.liberer(null, "0"));

        c.finDeJournee();
        String bilan = secretaire.informations.get(0);
        assertTrue(bilan, bilan.contains("encaissé : 2 000 DA"));
        assertFalse(bilan, bilan.contains("pas encore libérés"));
    }

    @Test
    public void lesFraisAffiches() {
        assertEquals("frais à demander au médecin", AccueilController.texteFrais(null));
        assertEquals("consultation gratuite", AccueilController.texteFrais(0));
        assertEquals("12 500 DA à régler", AccueilController.texteFrais(12500));
        assertEquals("800 DA", AccueilController.montant(800));
    }

    @Test
    public void laFinDeJourneeMarqueLesAbsentsEtDonneLeBilan() {
        ControleursTest.FausseAccueil vue = vue();
        AccueilController c = accueil(vue, null, true);
        rdv(1, "00:00", 1);
        c.finDeJournee();
        assertEquals(1, vue.informations.size());
        String bilan = vue.informations.get(0);
        assertTrue(bilan, bilan.startsWith("1 rendez-vous non honoré(s) marqué(s) absent(s)."));
        assertTrue(bilan, bilan.contains("absents : 1"));

        c.finDeJournee();
        assertTrue(vue.informations.get(1).startsWith("Aucun rendez-vous en suspens."));

        base.echouerSur("rdv.findEntre");
        c.finDeJournee();
        assertEquals(2, vue.informations.size());
        assertEquals(1, vue.erreurs.size());
    }

    @Test
    public void laPurgeDesEvenementsEstSilencieuse() {
        ControleursTest.FausseAccueil vue = vue();
        base.echouerSur("evenement.purger");
        accueil(vue, null, true).purgerEvenements();
        assertTrue(vue.erreurs.isEmpty());
    }

    // ----- liste des patients -----

    @Test
    public void unPatientSansRendezVousEntreEnSalleDAttente() {
        Session session = new Session(base, 1, 1);
        User u = Donnees.utilisateur(5);
        u.setRole(Role.SECRETAIRE);
        session.ouvrir(u);
        ControleursTest.FausseListePatient vue = new ControleursTest.FausseListePatient();
        ListePatientController c = new ListePatientController(vue, new PatientService(base), dossiers, session,
                salle, rendezVous);
        assertEquals("deux médecins : la secrétaire choisit", 2, c.medecinsAChoisir().size());
        assertTrue(c.mettreEnSalle(base.patients.get(1), 2));
        assertEquals(1, vue.informations.size());
        assertEquals(2, salle.file(null).get(0).getIdMedecin());
        assertFalse("déjà en salle", c.mettreEnSalle(base.patients.get(1), 2));
        assertEquals(1, vue.erreurs.size());

        base.medecins.remove(2);
        assertTrue("un seul médecin : pas de choix", c.medecinsAChoisir().isEmpty());
    }

    @Test
    public void unMedecinNaPasAChoisirLeMedecin() {
        ControleursTest.FausseListePatient vue = new ControleursTest.FausseListePatient();
        ListePatientController c = new ListePatientController(vue, new PatientService(base), dossiers,
                new Session(base, 1, 1), salle, rendezVous);
        assertTrue(c.medecinsAChoisir().isEmpty());
        assertTrue(new ListePatientController(vue, new PatientService(base), dossiers, new Session(base, 1, 1))
                .medecinsAChoisir().isEmpty());
    }

}
