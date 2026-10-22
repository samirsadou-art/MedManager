package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.entities.DossierPatient;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

/** Rôles des comptes : le dossier médical est réservé aux médecins, les membres aux administrateurs. */
public class RolesTest {

    private MemoireDAOFactory base;
    private Session session;
    private DossierService dossiers;
    private MembresService membres;

    @Before
    public void preparer() {
        base = Donnees.base();
        session = new Session(base, 1, 1);
        dossiers = new DossierService(base, JournalAcces.AUCUN, session);
        membres = new MembresService(base, JournalAcces.AUCUN, session);
    }

    private void connecter(Role role) {
        User u = Donnees.utilisateur(9);
        u.setRole(role);
        session.ouvrir(u);
    }

    private static void refuse(Runnable action, String fragment) {
        try {
            action.run();
            fail("action permise à tort");
        } catch (MetierException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(fragment));
        }
    }

    @Test
    public void codesDeRoleLusSansTenirCompteDeLaCasse() {
        assertEquals(Role.SECRETAIRE, Role.depuis(" secretaire "));
        assertEquals(Role.ADMIN, Role.depuis("Admin"));
        try {
            Role.depuis("chef");
            fail();
        } catch (IllegalArgumentException attendu) {
            assertTrue(attendu.getMessage().contains("chef"));
        }
        assertEquals("Administrateur", Role.ADMIN.getLibelle());
    }

    @Test
    public void unUtilisateurSansRoleExpliciteEstMedecin() {
        User u = new User();
        assertEquals(Role.MEDECIN, u.getRole());
        u.setRole(null);
        assertEquals(Role.MEDECIN, u.getRole());
    }

    @Test
    public void sansSessionOuverteAucunRoleNEstExige() {
        assertNull(session.getRole());
        session.exiger(Role.ADMIN);
        assertEquals(1, dossiers.ouvrirDossier(base.patients.get(1), null).getMalade().getIdentPat());
    }

    @Test
    public void laSecretaireNOuvrePasLeDossierMedical() {
        connecter(Role.SECRETAIRE);
        assertEquals(Role.SECRETAIRE, session.getRole());
        refuse(() -> dossiers.ouvrirDossier(base.patients.get(1), null), "médecin");
        refuse(() -> dossiers.consultationsDuJour(LocalDate.now()), "Secrétaire");
        DossierPatient d = new DossierPatient(base.patients.get(1), Donnees.medecin(1));
        refuse(() -> dossiers.nouvelleConsultation(d), "médecin");
        refuse(() -> dossiers.enregistrer(d), "médecin");
        assertTrue("aucune trace d'accès au dossier", base.journal.isEmpty());
    }

    @Test
    public void lAdministrateurNOuvrePasLeDossierMedical() {
        connecter(Role.ADMIN);
        refuse(() -> dossiers.ouvrirDossier(base.patients.get(1), null), "Administrateur");
    }

    @Test
    public void leMedecinOuvreLeDossierMaisPasLaListeDesMembres() {
        connecter(Role.MEDECIN);
        assertEquals(1, dossiers.ouvrirDossier(base.patients.get(1), null).getMalade().getIdentPat());
        refuse(() -> membres.lister(), "administrateur");
    }

    @Test
    public void lAdministrateurAjouteUneSecretaireEtUnMedecin() {
        connecter(Role.ADMIN);
        User s = membres.ajouter("  benali ", " AMINA ", Role.SECRETAIRE, 1);
        assertEquals("BENALI", s.getNom());
        assertEquals("Amina", s.getPrenom());
        assertNull("une secrétaire n'est rattachée à aucun médecin", s.getIdMedecin());
        User m = membres.ajouter("amrani", "lina", Role.MEDECIN, 1);
        assertEquals(Integer.valueOf(1), m.getIdMedecin());
        assertEquals(3, membres.lister().size());
    }

    @Test
    public void unMedecinDoitEtreRattacheAUneFicheExistante() {
        refuse(() -> membres.ajouter("amrani", "lina", Role.MEDECIN, null), "fiche médecin");
        refuse(() -> membres.ajouter("amrani", "lina", Role.MEDECIN, 42), "introuvable");
        refuse(() -> membres.ajouter(" ", "lina", Role.SECRETAIRE, null), "nom");
    }

    @Test
    public void leDernierAdministrateurNePeutNiPartirNiChangerDeRole() {
        User admin = membres.ajouter("admin", "cabinet", Role.ADMIN, null);
        AuthentificationService auth = new AuthentificationService(base, session, JournalAcces.AUCUN);
        refuse(() -> membres.definirRole(admin.getIdent(), Role.SECRETAIRE, null), "dernier");
        refuse(() -> auth.definirActif(admin.getIdent(), false), "dernier");
        User second = membres.ajouter("second", "admin", Role.ADMIN, null);
        auth.definirActif(admin.getIdent(), false);
        assertTrue(base.inactifs.contains(admin.getIdent()));
        // Le seul administrateur actif restant est protégé à son tour.
        refuse(() -> membres.definirRole(second.getIdent(), Role.MEDECIN, 1), "dernier");
    }

    @Test
    public void changementDeRoleEnregistre() {
        membres.definirRole(1, Role.SECRETAIRE, 1);
        assertEquals(Role.SECRETAIRE, base.utilisateurs.get(1).getRole());
        assertNull(base.utilisateurs.get(1).getIdMedecin());
        refuse(() -> membres.definirRole(77, Role.SECRETAIRE, null), "introuvable");
    }
}
