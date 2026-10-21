package My_application.controller;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.DossierPatient;
import My_application.entities.Rendez_Vous;
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
import My_application.service.PatientService;
import My_application.service.RendezVousService;
import My_application.service.Session;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests des contrôleurs MVC avec de fausses vues : on vérifie ce que la vue
 * reçoit (données, messages) sans ouvrir aucune fenêtre Swing.
 */
public class ControleursTest {

    private MemoireDAOFactory base;

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
        return new DossierPatientController(vue, d, ds, new CatalogueService(base), new RapportService());
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
        assertTrue(ctrl.annuler(r));
        assertEquals(0, base.rendezVous.size());
    }

    // ----- accueil -----

    static class FausseAccueil extends FausseVue implements AccueilVue {
        List<Rendez_Vous> rdv = new ArrayList<>();
        List<consultation> consultations = new ArrayList<>();

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
        new AccueilController(vue, ds, new RendezVousService(base)).rafraichir();
        assertEquals(1, vue.consultations.size());
        assertTrue(vue.erreurs.isEmpty());
    }

    // ----- nouveau patient -----

    @Test
    public void nouveauPatientDepuisLaSaisie() {
        FausseVue vue = new FausseVue();
        NouveauPatientController ctrl = new NouveauPatientController(vue, new PatientService(base));
        NouveauPatientController.Saisie s = new NouveauPatientController.Saisie();
        s.nom = "ziani";
        s.prenom = "nadia";
        s.dateNaissance = "29/02/1992";
        s.masculin = false;
        patient p = ctrl.enregistrer(s);
        assertNotNull(p);
        assertEquals("ZIANI", p.getNom());
        assertEquals(LocalDate.of(1992, 2, 29), p.getDat_nais().toLocalDate());
        assertTrue(vue.informations.get(0).contains("enregistré"));
    }

    @Test
    public void dateDeNaissanceInvalide() {
        FausseVue vue = new FausseVue();
        NouveauPatientController ctrl = new NouveauPatientController(vue, new PatientService(base));
        NouveauPatientController.Saisie s = new NouveauPatientController.Saisie();
        s.nom = "X";
        s.prenom = "Y";
        s.dateNaissance = "31/02/1990"; // n'existe pas
        assertNull(ctrl.enregistrer(s));
        assertTrue(vue.erreurs.get(0).contains("jj/mm/aaaa"));
    }

    @Test
    public void numeroDeDossierNonNumerique() {
        FausseVue vue = new FausseVue();
        NouveauPatientController.Saisie s = new NouveauPatientController.Saisie();
        s.numero = "A12";
        s.nom = "X";
        s.prenom = "Y";
        s.dateNaissance = "01/01/1990";
        assertNull(new NouveauPatientController(vue, new PatientService(base)).enregistrer(s));
        assertTrue(vue.erreurs.get(0).contains("nombre"));
    }
}
