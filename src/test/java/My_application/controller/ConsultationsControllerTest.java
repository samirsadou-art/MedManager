package My_application.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.DossierPatient;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.entities.consultation;
import My_application.fake.Donnees;
import My_application.fake.FausseVue;
import My_application.fake.MemoireDAOFactory;
import My_application.service.DossierService;
import My_application.service.JournalAcces;
import My_application.service.PatientService;
import My_application.service.RendezVousService;
import My_application.service.Session;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/** Page « Consultations » du médecin : consultations d'une date choisie. */
public class ConsultationsControllerTest {

    private MemoireDAOFactory base;
    private DossierService dossiers;
    private final FausseConsultations vue = new FausseConsultations();

    static final class FausseConsultations extends FausseVue implements ConsultationsVue {
        LocalDate jour;
        List<consultation> consultations;
        Map<Integer, String> medecins;
        final List<DossierPatient> dossiers = new ArrayList<>();

        @Override
        public void afficherConsultations(LocalDate j, List<consultation> c, Map<Integer, String> m) {
            jour = j;
            consultations = c;
            medecins = m;
        }

        @Override
        public void ouvrirDossier(DossierPatient dossier) {
            dossiers.add(dossier);
        }
    }

    @Before
    public void preparer() {
        base = Donnees.base();
        dossiers = new DossierService(base);
        DossierPatient d = dossiers.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        dossiers.nouvelleConsultation(d);
        dossiers.enregistrer(d);
    }

    private ConsultationsController controleur(Executeur executeur) {
        return new ConsultationsController(vue, dossiers, new PatientService(base), new RendezVousService(base),
                () -> base.medecins.get(1), executeur);
    }

    @Test
    public void afficheAujourdhuiAuPremierAffichage() {
        controleur(Executeur.IMMEDIAT).rafraichir();
        assertEquals(LocalDate.now(), vue.jour);
        assertEquals(1, vue.consultations.size());
        assertEquals(base.medecins.get(1).toString(), vue.medecins.get(1));
        assertTrue(vue.erreurs.isEmpty());
    }

    @Test
    public void afficheLaDateChoisieEtLaGardePourActualiser() {
        ConsultationsController c = controleur(Executeur.IMMEDIAT);
        LocalDate hier = LocalDate.now().minusDays(1);
        c.afficher(hier);
        assertEquals(hier, vue.jour);
        assertTrue(vue.consultations.isEmpty());
        vue.jour = null;
        c.rafraichir();
        assertEquals(hier, vue.jour);
        assertEquals(hier, c.getJour());
    }

    @Test
    public void uneLectureDepasseeNEstPasAffichee() {
        final List<Runnable> suites = new ArrayList<>();
        Executeur differe = new Executeur() {
            @Override
            public void executer(Runnable travail, Runnable suite) {
                travail.run();
                suites.add(suite);
            }
        };
        ConsultationsController c = controleur(differe);
        c.afficher(LocalDate.now());
        c.afficher(LocalDate.now().minusDays(1));
        suites.get(1).run();
        suites.get(0).run();
        assertEquals("la date choisie en dernier reste affichée", LocalDate.now().minusDays(1), vue.jour);
        assertTrue(vue.consultations.isEmpty());
    }

    @Test
    public void ouvreLeDossierCompletDuPatient() {
        ConsultationsController c = controleur(Executeur.IMMEDIAT);
        c.afficher(LocalDate.now());
        c.ouvrirDossier(vue.consultations.get(0));
        assertEquals(1, vue.dossiers.size());
        assertEquals(base.patients.get(1).getNom(), vue.dossiers.get(0).getMalade().getNom());
        assertEquals(1, vue.dossiers.get(0).getConsultations().size());
    }

    @Test
    public void rienSansConsultationChoisie() {
        controleur(Executeur.IMMEDIAT).ouvrirDossier(null);
        assertTrue(vue.dossiers.isEmpty());
        assertTrue(vue.erreurs.isEmpty());
    }

    @Test
    public void patientIntrouvableSignale() {
        ConsultationsController c = controleur(Executeur.IMMEDIAT);
        c.afficher(LocalDate.now());
        consultation cons = vue.consultations.get(0);
        base.patients.remove(cons.getPatient().getIdentPat());
        c.ouvrirDossier(cons);
        assertTrue(vue.dossiers.isEmpty());
        assertEquals(1, vue.erreurs.size());
        assertTrue(vue.erreurs.get(0).contains("introuvable"));
    }

    @Test
    public void refuseALaSecretaire() {
        Session session = new Session(base, 1, 1);
        User secretaire = Donnees.utilisateur(5);
        secretaire.setRole(Role.SECRETAIRE);
        session.ouvrir(secretaire);
        dossiers = new DossierService(base, JournalAcces.AUCUN, session);
        controleur(Executeur.IMMEDIAT).afficher(LocalDate.now());
        assertNull(vue.consultations);
        assertEquals(1, vue.erreurs.size());
    }
}
