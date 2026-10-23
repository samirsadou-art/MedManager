package My_application.controller;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.DossierPatient;
import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.entities.medecin;
import My_application.entities.consultation;
import My_application.entities.patient;
import My_application.entities.symptome;
import My_application.fake.Donnees;
import My_application.fake.FausseVue;
import My_application.fake.MemoireDAOFactory;
import My_application.report.RapportService;
import My_application.service.AuthentificationService;
import My_application.service.CatalogueService;
import My_application.service.DossierService;
import My_application.service.JournalAcces;
import My_application.service.MembresService;
import My_application.service.PatientService;
import My_application.service.RendezVousService;
import My_application.service.SalleAttenteService;
import My_application.service.Session;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests des contrôleurs MVC avec de fausses vues : on vérifie ce que la vue
 * reçoit (données, messages) sans ouvrir aucune fenêtre Swing.
 */
public class ControleursTest {

    private MemoireDAOFactory base;

    @Rule
    public TemporaryFolder dossierDocuments = new TemporaryFolder();

    @Before
    public void preparer() {
        base = Donnees.base();
    }

    // ----- liste des patients -----

    static class FausseListePatient extends FausseVue implements ListePatientVue {
        List<patient> affiches;
        DossierPatient dossierOuvert;

        @Override
        public void afficherPatients(List<patient> patients) {
            affiches = patients;
        }

        @Override
        public void afficherDossier(DossierPatient dossier) {
            dossierOuvert = dossier;
        }
    }

    private ListePatientController listePatient(FausseListePatient vue) {
        return new ListePatientController(vue, new PatientService(base), new DossierService(base),
                new Session(base, 1, 1));
    }

    @Test
    public void laRechercheAfficheLesPatients() {
        FausseListePatient vue = new FausseListePatient();
        listePatient(vue).rechercher("AM");
        assertEquals(1, vue.affiches.size());
        assertEquals("AMRANI", vue.affiches.get(0).getNom());
    }

    @Test
    public void unePanneDeBaseDevientUnMessageEtNonUnPlantage() {
        FausseListePatient vue = new FausseListePatient();
        base.echouerSur("patient.findByNom");
        listePatient(vue).rechercher("A");
        assertNull(vue.affiches);
        assertEquals(1, vue.erreurs.size());
        assertTrue(vue.erreurs.get(0).contains("base de données"));
        // Le détail technique va dans le journal, jamais à l'écran.
        assertFalse(vue.erreurs.get(0).contains("Panne simulée"));
        assertFalse(vue.erreurs.get(0).contains("patient.findByNom"));
    }

    @Test
    public void ouvrirUnDossierChargeLeMedecinTraitant() {
        FausseListePatient vue = new FausseListePatient();
        assertTrue(listePatient(vue).ouvrirDossier(base.patients.get(2)));
        assertEquals("BELKACEM", vue.dossierOuvert.getMalade().getNom());
        assertEquals("SAHAB", vue.dossierOuvert.getMedTraitant().getNom());
    }

    @Test
    public void medecinIntrouvable() {
        FausseListePatient vue = new FausseListePatient();
        base.medecins.clear();
        assertFalse(listePatient(vue).ouvrirDossier(base.patients.get(2)));
        assertTrue(vue.erreurs.get(0).contains("introuvable"));
    }

    // ----- dossier patient -----

    private DossierPatientController dossier(FausseVue vue) {
        DossierService ds = new DossierService(base);
        DossierPatient d = ds.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        // jamais le dossier réel ~/GestiMed/Documents pendant les tests
        RapportService rapports = new RapportService(dossierDocuments.getRoot(), new RapportService.Afficheur() {
            @Override
            public void ouvrir(java.io.File pdf) throws java.io.IOException {
                throw new java.io.IOException("aucune visionneuse");
            }

            @Override
            public boolean imprimer(java.io.File pdf) throws java.io.IOException {
                throw new java.io.IOException("aucune visionneuse");
            }
        });
        return new DossierPatientController(vue, d, ds, new CatalogueService(base), rapports);
    }

    @Test
    public void unSymptomeTapeLibrementEstCreeDansLeReferentiel() {
        FausseVue vue = new FausseVue();
        DossierPatientController ctrl = dossier(vue);
        consultation c = ctrl.nouvelleConsultation();
        assertTrue(ctrl.ajouterSymptome(c, null, "Douleur thoracique"));
        assertEquals(1, base.symptomes.size());
        assertEquals("Douleur thoracique", c.getSymptomes().get(0).getDesignation());
    }

    @Test
    public void unSymptomeChoisiDansLaListeNEstPasRecree() {
        FausseVue vue = new FausseVue();
        DossierPatientController ctrl = dossier(vue);
        symptome toux = new symptome(5, "Toux");
        base.symptomes.add(toux);
        consultation c = ctrl.nouvelleConsultation();
        ctrl.ajouterSymptome(c, toux, "Toux");
        assertEquals(1, base.symptomes.size());
    }

    @Test
    public void enregistrementEnEchecSignaleEtRenvoieFaux() {
        FausseVue vue = new FausseVue();
        DossierPatientController ctrl = dossier(vue);
        ctrl.nouvelleConsultation();
        base.echouerSur("consultation.insert");
        assertFalse(ctrl.enregistrer());
        assertEquals(1, vue.erreurs.size());
        assertTrue(ctrl.aDesModifications());
        assertTrue(ctrl.enregistrer());
        assertFalse(ctrl.aDesModifications());
    }

    @Test
    public void uneDeuxiemeNouvelleConsultationEstRefuseeAvecUnMessage() {
        FausseVue vue = new FausseVue();
        DossierPatientController ctrl = dossier(vue);
        assertNotNull(ctrl.nouvelleConsultation());
        assertNull(ctrl.nouvelleConsultation());
        assertTrue(vue.erreurs.get(0).contains("déjà"));
    }

    @Test
    public void constanteInvalideRefusee() {
        FausseVue vue = new FausseVue();
        DossierPatientController ctrl = dossier(vue);
        consultation c = ctrl.nouvelleConsultation();
        assertFalse(ctrl.saisirConstante(c, DossierPatientController.Constante.TEMPERATURE, "120"));
        assertEquals(consultation.NON_RENSEIGNE, c.getTemperature(), 0);
        assertTrue(ctrl.saisirConstante(c, DossierPatientController.Constante.TEMPERATURE, "37.2"));
        assertEquals(37.2, c.getTemperature(), 0.001);
    }

    @Test
    public void imprimerSansOrdonnanceAfficheUnMessage() {
        FausseVue vue = new FausseVue();
        DossierPatientController ctrl = dossier(vue);
        ctrl.ordonnance(ctrl.nouvelleConsultation(), false);
        assertTrue(vue.erreurs.get(0).contains("ordonnance"));
    }

    @Test
    public void visionneuseAbsenteLeMessageDitOuEstLeDocument() {
        FausseVue vue = new FausseVue();
        DossierPatientController ctrl = dossier(vue);
        consultation c = ctrl.nouvelleConsultation();
        c.getOrdoPat().ajouter(Donnees.medicament(1, "DOLIPRANE 1G", 1, 3, 2, 5));
        ctrl.ordonnance(c, false);
        assertTrue(vue.erreurs.get(0), vue.erreurs.get(0).contains(dossierDocuments.getRoot().getAbsolutePath()));
        assertFalse(vue.erreurs.get(0).contains("aucune visionneuse"));
    }

    @Test
    public void impressionIndisponibleLeDocumentEstOuvertEtLUtilisateurPrevenu() throws Exception {
        FausseVue vue = new FausseVue();
        final List<java.io.File> ouverts = new ArrayList<>();
        RapportService rapports = new RapportService(java.nio.file.Files.createTempDirectory("gestimed").toFile(),
                new RapportService.Afficheur() {
                    @Override
                    public void ouvrir(java.io.File pdf) {
                        ouverts.add(pdf);
                    }

                    @Override
                    public boolean imprimer(java.io.File pdf) {
                        ouverts.add(pdf);
                        return false;
                    }
                });
        DossierService ds = new DossierService(base);
        DossierPatient d = ds.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        DossierPatientController ctrl = new DossierPatientController(vue, d, ds, new CatalogueService(base), rapports);
        consultation c = ctrl.nouvelleConsultation();
        c.getOrdoPat().ajouter(Donnees.medicament(1, "DOLIPRANE 1G", 1, 3, 2, 5));
        ctrl.ordonnance(c, true);
        assertEquals(1, ouverts.size());
        assertTrue(ouverts.get(0).getName().endsWith(".pdf"));
        assertEquals(DossierPatientController.IMPRESSION_INDISPONIBLE, vue.informations.get(0));
        assertTrue(vue.erreurs.isEmpty());
    }

    @Test
    public void leMedecinIndiqueLesFraisDuPatientEnSalleDAttente() {
        FausseVue vue = new FausseVue();
        SalleAttenteService salle = new SalleAttenteService(base, new RendezVousService(base));
        salle.arriveeSansRendezVous(base.patients.get(1), 1);
        DossierPatientController ctrl = dossier(vue);
        assertFalse("pas de nouvelle consultation", ctrl.fraisADemander());
        ctrl.nouvelleConsultation();
        assertTrue(ctrl.fraisADemander());
        assertEquals("", ctrl.fraisProposes());
        assertFalse(ctrl.enregistrer("-3"));
        assertEquals(1, vue.erreurs.size());
        assertTrue(ctrl.enregistrer("1 500"));
        assertEquals(Integer.valueOf(1500), base.passages.get(0).getFrais());
        assertEquals("1500", ctrl.fraisProposes());
    }

    // ----- connexion -----

    @Test
    public void connexionRefuseeAfficheLeMessageEtEffaceLeMotDePasse() {
        FausseVue vue = new FausseVue();
        Session session = new Session(base, 1, 1);
        ConnexionController ctrl = new ConnexionController(vue,
                new AuthentificationService(base, session, JournalAcces.AUCUN));
        assertEquals(Boolean.TRUE, ctrl.premiereUtilisation());
        char[] mdp = "Cabinet2026".toCharArray();
        assertNotNull(ctrl.creerPremierCompte("medecin", mdp, "Cabinet2026".toCharArray()));
        assertArrayEquals(new char[mdp.length], mdp);
        char[] faux = "Mauvais2026".toCharArray();
        assertNull(ctrl.connecter("medecin", faux));
        assertEquals("Identifiant ou mot de passe incorrect.", vue.erreurs.get(0));
        assertArrayEquals(new char[faux.length], faux);
        assertNotNull(ctrl.connecter("medecin", "Cabinet2026".toCharArray()));
    }

    @Test
    public void motDePasseProvisoireRemplacePuisChangeParLeMembre() {
        FausseVue vue = new FausseVue();
        Session session = new Session(base, 1, 1);
        ConnexionController ctrl = new ConnexionController(vue,
                new AuthentificationService(base, session, JournalAcces.AUCUN));
        base.logins.put(1, "medecin");
        base.empreintes.put(1, My_application.service.MotDePasse.hacher("Provisoire1".toCharArray()));
        base.aChanger.add(1);
        User u = ctrl.connecter("medecin", "Provisoire1".toCharArray());
        assertTrue(u.isDoitChanger());
        char[] court = "abc".toCharArray();
        assertNull(ctrl.remplacerMotDePasseProvisoire(court, "abc".toCharArray()));
        assertEquals(1, vue.erreurs.size());
        assertArrayEquals(new char[3], court);
        assertNotNull(ctrl.remplacerMotDePasseProvisoire("Choisi2026".toCharArray(), "Choisi2026".toCharArray()));
        assertTrue(session.estOuverte());

        char[] actuel = "Choisi2026".toCharArray();
        assertTrue(ctrl.changerMonMotDePasse(actuel, "Nouveau2026".toCharArray(), "Nouveau2026".toCharArray()));
        assertArrayEquals(new char[actuel.length], actuel);
        assertTrue(vue.informations.get(0).contains("changé"));
        assertFalse(ctrl.changerMonMotDePasse("Faux20266".toCharArray(), "Autre2026".toCharArray(),
                "Autre2026".toCharArray()));
        assertEquals(2, vue.erreurs.size());

        ctrl.connecter("medecin", "Nouveau2026".toCharArray());
        ctrl.abandonnerConnexion();
    }

    // ----- rendez-vous -----

    @Test
    public void unePanneDeBasePendantLApercuEstSignaleeSansDetailTechnique() {
        FausseVue vue = new FausseVue();
        RendezVousController ctrl = new RendezVousController(vue, new RendezVousService(base),
                base.patients.get(1), Donnees.utilisateur(1));
        base.echouerSur("rdv.findEntre");
        assertNull(ctrl.apercu(LocalDate.now().plusDays(1), 9));
        assertEquals(1, vue.erreurs.size());
        assertTrue(vue.erreurs.get(0).contains("base de données"));
        assertFalse(vue.erreurs.get(0).contains("Panne simulée"));
    }

    @Test
    public void listeDesMedecinsVidePlutotQuUnePanne() {
        FausseVue vue = new FausseVue();
        RendezVousController ctrl = new RendezVousController(vue, new RendezVousService(base), null, null);
        assertEquals(1, ctrl.medecins().size());
        base.echouerSur("medecin.findAll");
        assertTrue(ctrl.medecins().isEmpty());
        assertEquals(1, vue.erreurs.size());
    }

    @Test
    public void leRendezVousEstProposeAvecLeMedecinChoisiDansLAgenda() {
        RendezVousController ctrl = new RendezVousController(new FausseVue(), new RendezVousService(base),
                base.patients.get(1), Donnees.utilisateur(1));
        LocalDate jour = LocalDate.now().plusDays(1);
        assertEquals(Integer.valueOf(RendezVousService.MEDECIN_PAR_DEFAUT), ctrl.getIdMedecinReservation());
        ctrl.setMedecin(2);
        assertEquals(Integer.valueOf(2), ctrl.apercu(jour, 9).getIdMedecin());
        assertEquals(Integer.valueOf(2), ctrl.proposer(jour, 10).getIdMedecin());
        ctrl.setMedecin(null);
        assertEquals(Integer.valueOf(RendezVousService.MEDECIN_PAR_DEFAUT), ctrl.proposer(jour, 10).getIdMedecin());
    }

    @Test
    public void priseDeRendezVousComplete() {
        FausseVue vue = new FausseVue();
        RendezVousController ctrl = new RendezVousController(vue, new RendezVousService(base),
                base.patients.get(1), Donnees.utilisateur(1));
        LocalDate jour = LocalDate.now().plusDays(1);
        Rendez_Vous r = ctrl.proposer(jour, 9);
        assertNotNull(r);
        assertTrue(ctrl.confirmer(r));
        assertEquals("Prise du rendez-vous réussie !", vue.informations.get(0));
        // Le même patient, la même heure : refus affiché à l'utilisateur.
        assertNull(ctrl.proposer(jour, 9));
        assertEquals(1, vue.erreurs.size());
        // L'aperçu (simple clic) ne dérange pas l'utilisateur avec un message.
        assertNull(ctrl.apercu(jour, 9));
        assertEquals(1, vue.erreurs.size());
        assertTrue(ctrl.peutAnnuler(r));
        assertFalse(ctrl.peutMarquerAbsent(r));
        assertTrue(ctrl.annuler(r));
        assertEquals(Rendez_Vous.ETAT_ANNULE, base.rendezVous.get(0).getEtat());
        assertFalse(ctrl.peutAnnuler(r));
        // Une seconde annulation (affichage périmé) est refusée avec un message.
        assertFalse(ctrl.annuler(r));
        assertEquals(2, vue.erreurs.size());
    }

    @Test
    public void deplacementEtRechercheDepuisLAgendaSansPatient() {
        FausseVue vue = new FausseVue();
        RendezVousController pourPatient = new RendezVousController(vue, new RendezVousService(base),
                base.patients.get(1), Donnees.utilisateur(1));
        LocalDate jour = LocalDate.now().plusDays(1);
        Rendez_Vous r = pourPatient.proposer(jour, 9);
        assertTrue(pourPatient.confirmer(r));
        // La secrétaire ouvre l'agenda sans patient : elle peut déplacer.
        RendezVousController agenda = new RendezVousController(vue, new RendezVousService(base), null, null);
        assertTrue(agenda.peutDeplacer(r));
        Rendez_Vous n = agenda.proposerDeplacement(r, jour, 11);
        assertEquals(LocalTime.of(11, 0), n.getHeure());
        assertTrue(agenda.deplacer(r, n));
        assertTrue(vue.informations.get(vue.informations.size() - 1).startsWith("Rendez-vous déplacé au "));
        assertEquals(LocalTime.of(11, 0), base.rendezVous.get(0).getHeure());
        // Une heure fermée : refus affiché, pas d'exception.
        assertNull(agenda.proposerDeplacement(r, jour, 20));
        assertTrue(vue.erreurs.get(vue.erreurs.size() - 1).contains("fermé"));
        assertEquals(1, agenda.rechercher(base.patients.get(1).getNom()).size());
        // Sans service de rapports, la liste du jour ne fait rien.
        agenda.listeDuJour(jour, false);
    }

    @Test
    public void listeDuJourOuverteOuImprimee() throws Exception {
        java.io.File dossier = dossierDocuments.newFolder("rdv");
        final List<java.io.File> ouverts = new ArrayList<>();
        RapportService rapports = new RapportService(dossier, new RapportService.Afficheur() {
            @Override
            public void ouvrir(java.io.File pdf) {
                ouverts.add(pdf);
            }

            @Override
            public boolean imprimer(java.io.File pdf) {
                return false;
            }
        }, 0);
        FausseVue vue = new FausseVue();
        RendezVousController ctrl = new RendezVousController(vue, new RendezVousService(base), rapports,
                base.patients.get(1), Donnees.utilisateur(1));
        LocalDate jour = LocalDate.now().plusDays(1);
        ctrl.listeDuJour(jour, false);
        assertTrue(vue.erreurs.get(0).startsWith("Aucun rendez-vous le "));
        assertTrue(ctrl.confirmer(ctrl.proposer(jour, 9)));
        ctrl.listeDuJour(jour, false);
        assertEquals(1, ouverts.size());
        assertTrue(ouverts.get(0).getName().startsWith("RendezVous_"));
        ctrl.listeDuJour(jour, true);
        assertEquals(RendezVousController.IMPRESSION_INDISPONIBLE, vue.informations.get(vue.informations.size() - 1));
    }

    // ----- accueil -----

    static class FausseAccueil extends FausseVue implements AccueilVue {
        List<Rendez_Vous> rdv;
        List<consultation> consultations;
        List<Passage> file;
        Map<Integer, String> medecins;
        final List<String> annonces = new ArrayList<>();
        final List<DossierPatient> dossiers = new ArrayList<>();
        int affichagesSalle;

        @Override
        public void afficherSalleAttente(List<Passage> f, Map<Integer, String> m, LocalDateTime maintenant) {
            file = f;
            medecins = m;
            affichagesSalle++;
        }

        @Override
        public void annoncerAppel(String annonce) {
            annonces.add(annonce);
        }

        @Override
        public void ouvrirDossier(DossierPatient dossier) {
            dossiers.add(dossier);
        }

        /** Frais saisis quand le médecin doit terminer la consultation en cours ; null : il renonce. */
        String fraisFinDeConsultation;
        final List<String> fraisProposes = new ArrayList<>();

        @Override
        public boolean terminerConsultationEnCours(Passage enCours, String proposes,
                java.util.function.Predicate<String> terminer) {
            fraisProposes.add(proposes);
            return fraisFinDeConsultation != null && terminer.test(fraisFinDeConsultation);
        }

        @Override
        public void afficherRendezVousDuJour(List<Rendez_Vous> rendezVous) {
            rdv = rendezVous;
        }

        @Override
        public void afficherConsultationsDuJour(List<consultation> c) {
            consultations = c;
        }
    }

    @Test
    public void accueilAfficheLActiviteDuJour() {
        DossierService ds = new DossierService(base);
        DossierPatient d = ds.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        ds.nouvelleConsultation(d);
        ds.enregistrer(d);
        FausseAccueil vue = new FausseAccueil();
        new AccueilController(vue, ds, new RendezVousService(base), Executeur.IMMEDIAT).rafraichir();
        assertEquals(1, vue.consultations.size());
        assertTrue(vue.erreurs.isEmpty());
    }

    @Test
    public void accueilLitEnArrierePlanEtNAfficheQuEnsuite() {
        final List<Runnable> suites = new ArrayList<>();
        Executeur differe = new Executeur() {
            @Override
            public void executer(Runnable travail, Runnable suite) {
                travail.run();
                suites.add(suite);
            }
        };
        FausseAccueil vue = new FausseAccueil();
        new AccueilController(vue, new DossierService(base), new RendezVousService(base), differe).rafraichir();
        assertNull("rien n'est affiché depuis le fil de lecture", vue.consultations);
        suites.get(0).run();
        assertNotNull(vue.consultations);
    }

    @Test
    public void accueilSignaleUnePanneDeLectureSansDetailTechnique() {
        base.echouerSur("consultation.findByDate");
        FausseAccueil vue = new FausseAccueil();
        new AccueilController(vue, new DossierService(base), new RendezVousService(base), Executeur.IMMEDIAT)
                .rafraichir();
        assertEquals(Arrays.asList(ControleurBase.ERREUR_BASE), vue.erreurs);
        assertNull(vue.consultations);
    }

    @Test
    public void accueilDeLaSecretaireSansConsultations() {
        Session session = new Session(base, 1, 1);
        User secretaire = Donnees.utilisateur(5);
        secretaire.setRole(Role.SECRETAIRE);
        session.ouvrir(secretaire);
        // Si les consultations étaient lues, cette panne serait signalée.
        base.echouerSur("consultation.findByDate");
        FausseAccueil vue = new FausseAccueil();
        new AccueilController(vue, new DossierService(base, JournalAcces.AUCUN, session), new RendezVousService(base),
                Executeur.IMMEDIAT, false).rafraichir();
        assertTrue(vue.erreurs.isEmpty());
        assertTrue(vue.consultations.isEmpty());
        assertNotNull(vue.rdv);
    }

    // ----- membres du cabinet -----

    private static final class FausseMembres extends FausseVue implements MembresVue {
        List<User> membres;

        @Override
        public void afficherMembres(List<User> m, List<medecin> medecins) {
            membres = m;
        }
    }

    @Test
    public void membresListesPourLAdministrateurSeulement() {
        Session session = new Session(base, 1, 1);
        MembresService service = new MembresService(base, JournalAcces.AUCUN, session);
        FausseMembres vue = new FausseMembres();
        User admin = Donnees.utilisateur(6);
        admin.setRole(Role.ADMIN);
        session.ouvrir(admin);
        new MembresController(vue, service).rafraichir();
        assertEquals(1, vue.membres.size());

        FausseMembres autre = new FausseMembres();
        session.ouvrir(Donnees.utilisateur(1));
        new MembresController(autre, service).rafraichir();
        assertNull(autre.membres);
        assertEquals(1, autre.erreurs.size());
        assertTrue(autre.erreurs.get(0).contains("administrateur"));
    }

    @Test
    public void lAdministrateurGereLesComptesEtLesFichesDepuisLaPage() {
        Session session = new Session(base, 1, 1);
        MembresService service = new MembresService(base, JournalAcces.AUCUN, session);
        User admin = Donnees.utilisateur(6);
        admin.setRole(Role.ADMIN);
        base.utilisateurs.put(6, admin);
        session.ouvrir(admin);
        FausseMembres vue = new FausseMembres();
        MembresController ctrl = new MembresController(vue, service);

        char[] mdp = "Provisoire1".toCharArray();
        assertTrue(ctrl.creerCompte("benali", "amina", Role.SECRETAIRE, null, null, "amina", mdp,
                "Provisoire1".toCharArray()));
        assertArrayEquals(new char[mdp.length], mdp);
        assertTrue(vue.informations.get(0).contains("amina"));
        assertEquals("la liste est relue après chaque changement", 3, vue.membres.size());
        User amina = null;
        for (User u : vue.membres) {
            if ("amina".equals(u.getLogin())) {
                amina = u;
            }
        }
        assertTrue(amina.isDoitChanger());

        assertFalse(ctrl.creerCompte("x", "y", Role.SECRETAIRE, null, null, "amina",
                "Provisoire1".toCharArray(), "Provisoire1".toCharArray()));
        assertTrue(vue.erreurs.get(0).contains("déjà utilisé"));

        assertTrue(ctrl.modifier(amina.getIdent(), "benali", "amina", Role.SECRETAIRE, null));
        assertTrue(ctrl.motDePasseProvisoire(amina, "Nouveau2026".toCharArray(), "Nouveau2026".toCharArray()));
        assertTrue(ctrl.definirActif(amina, false));
        assertTrue(base.inactifs.contains(amina.getIdent()));
        assertFalse(ctrl.definirActif(admin, false));

        medecin fiche = Donnees.medecin(1);
        fiche.setSpecialite("Cardiologie");
        assertTrue(ctrl.modifierMedecin(fiche));
        assertEquals("Cardiologie", base.medecins.get(1).getSpecialite());
        assertTrue(ctrl.definirMedecinActif(fiche, false));
        assertFalse(base.medecins.get(1).isActif());
    }

    // ----- fiche patient (inscription, modification) -----

    /** Fausse fiche : répond aux doublons proposés comme demandé. */
    private static final class FausseFiche extends FausseVue implements FichePatientVue {
        int reponse = FichePatientVue.CREER_QUAND_MEME;
        List<patient> proposes;

        @Override
        public int proposerDoublons(List<patient> doublons) {
            proposes = doublons;
            return reponse;
        }
    }

    private static FichePatientController.Saisie saisie(String nom, String prenom, String naissance, String tel) {
        FichePatientController.Saisie s = new FichePatientController.Saisie();
        s.nom = nom;
        s.prenom = prenom;
        s.dateNaissance = naissance;
        s.telephone = tel;
        return s;
    }

    @Test
    public void inscriptionDUnNouveauPatient() {
        FausseFiche vue = new FausseFiche();
        FichePatientController.Saisie s = saisie("ziani", "nadia", "29/02/1992", "0555 12 34 56");
        s.masculin = false;
        s.email = " Nadia.Ziani@Exemple.DZ ";
        s.adresse = "12 rue des Oliviers";
        s.wilaya = "Tizi Ouzou";
        FichePatientController.Resultat r = new FichePatientController(vue, new PatientService(base)).inscrire(s);
        assertNotNull(r);
        assertFalse(r.existant);
        assertEquals("ZIANI", r.patient.getNom());
        assertEquals(LocalDate.of(1992, 2, 29), r.patient.getDat_nais());
        assertEquals("0555123456", r.patient.getTelephone());
        assertEquals("nadia.ziani@exemple.dz", r.patient.getEmail());
        assertEquals("12 rue des Oliviers", r.patient.getLocalite());
        assertNull("aucun doublon à proposer", vue.proposes);
        assertTrue(vue.erreurs.isEmpty());
    }

    @Test
    public void unDoublonProposeEstChoisiAuLieuDeCreer() {
        patient existant = base.patients.get(1);
        existant.setTelephone("0661000000");
        int avant = base.patients.size();
        FausseFiche vue = new FausseFiche();
        vue.reponse = 0;
        FichePatientController.Resultat r = new FichePatientController(vue, new PatientService(base))
                .inscrire(saisie("Inconnu", "x", "01/01/2000", "+213 661 00 00 00"));
        assertTrue(r.existant);
        assertEquals(existant, r.patient);
        assertEquals(avant, base.patients.size());
    }

    @Test
    public void doublonIgnoreOuInscriptionAnnulee() {
        patient existant = base.patients.get(1);
        String naissance = existant.getDat_nais().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int avant = base.patients.size();
        FausseFiche vue = new FausseFiche();
        vue.reponse = FichePatientVue.ANNULER;
        PatientService service = new PatientService(base);
        assertNull(new FichePatientController(vue, service).inscrire(
                saisie(existant.getNom(), "Autre", naissance, "0770112233")));
        assertEquals(1, vue.proposes.size());
        assertEquals(avant, base.patients.size());

        vue.reponse = FichePatientVue.CREER_QUAND_MEME;
        FichePatientController.Resultat r = new FichePatientController(vue, service).inscrire(
                saisie(existant.getNom(), "Autre", naissance, "0770112233"));
        assertFalse(r.existant);
        assertEquals(avant + 1, base.patients.size());
    }

    @Test
    public void modificationDeLaFiche() {
        patient p = base.patients.get(2);
        FichePatientController.Saisie s = FichePatientController.Saisie.depuis(p);
        s.telephone = "026 12 34 56";
        s.ville = "Fréha";
        FausseFiche vue = new FausseFiche();
        patient modifie = new FichePatientController(vue, new PatientService(base)).modifier(p, s);
        assertEquals("026123456", modifie.getTelephone());
        assertEquals("Fréha", base.patients.get(2).getVille());
        assertEquals(2, modifie.getIdentPat());
        assertTrue(vue.informations.get(0).contains("enregistrée"));
    }

    @Test
    public void dateDeNaissanceInvalide() {
        FausseFiche vue = new FausseFiche();
        assertNull(new FichePatientController(vue, new PatientService(base))
                .inscrire(saisie("X", "Y", "31/02/1990", "0555123456")));
        assertTrue(vue.erreurs.get(0).contains("jj/mm/aaaa"));
    }

    @Test
    public void numeroDeDossierNonNumeriqueEtTelephoneManquant() {
        FausseFiche vue = new FausseFiche();
        FichePatientController.Saisie s = saisie("X", "Y", "01/01/1990", "0555123456");
        s.numero = "12#";
        FichePatientController ctrl = new FichePatientController(vue, new PatientService(base));
        assertNull(ctrl.inscrire(s));
        assertTrue(vue.erreurs.get(0).contains("numéro de dossier"));
        assertNull(ctrl.inscrire(saisie("X", "Y", "01/01/1990", " ")));
        assertTrue(vue.erreurs.get(1).contains("téléphone"));
    }
}
