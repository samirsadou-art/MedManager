package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.dao.DataAccessException;
import My_application.entities.EntreeJournal;
import My_application.entities.User;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;

public class AuthentificationServiceTest {

    /** Horloge réglable pour le blocage après plusieurs échecs. */
    private static final class Horloge extends Clock {
        Instant maintenant = Instant.parse("2026-10-21T08:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return maintenant;
        }
    }

    private MemoireDAOFactory base;
    private Session session;
    private Horloge horloge;
    private AuthentificationService service;

    @Before
    public void preparer() {
        base = Donnees.base();
        session = new Session(base, 1, 1);
        horloge = new Horloge();
        service = new AuthentificationService(base, session,
                new JournalAccesEnBase(base.getJournalDao(), session, horloge, "POSTE-1"), horloge);
    }

    private void creerCompte(int id, String login, String mdp) {
        base.utilisateurs.put(id, Donnees.utilisateur(id));
        base.logins.put(id, login);
        base.empreintes.put(id, MotDePasse.hacher(mdp.toCharArray(), 1000));
    }

    private static char[] mdp(String s) {
        return s.toCharArray();
    }

    private String derniereAction() {
        return base.journal.get(base.journal.size() - 1).getAction();
    }

    // ----- première utilisation -----

    @Test
    public void premiereUtilisationCreeLeCompteDeLUtilisateurParDefautEtOuvreLaSession() {
        assertTrue(service.premiereUtilisation());
        User u = service.creerPremierCompte("  Dr.Sahab ", mdp("Cabinet2026"), mdp("Cabinet2026"));
        assertEquals(1, u.getIdent());
        assertEquals("dr.sahab", base.logins.get(1));
        assertTrue(MotDePasse.verifier(mdp("Cabinet2026"), base.empreintes.get(1)));
        assertTrue(session.estOuverte());
        assertEquals(Integer.valueOf(1), session.getIdUtilisateurConnecte());
        assertEquals(2, base.journal.size());
        assertEquals("CREATION_COMPTE", base.journal.get(0).getAction());
        assertEquals("CONNEXION", base.journal.get(1).getAction());
        assertEquals(Integer.valueOf(1), base.journal.get(1).getIdUtilisateur());
        assertEquals("POSTE-1", base.journal.get(1).getPoste());
        assertFalse(service.premiereUtilisation());
    }

    @Test
    public void lePremierCompteNePeutPasEtreRecreeUneFoisQuUnCompteExiste() {
        creerCompte(1, "secretaire", "Secret2026");
        try {
            service.creerPremierCompte("pirate", mdp("Pirate2026"), mdp("Pirate2026"));
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("existe déjà"));
        }
        assertEquals("secretaire", base.logins.get(1));
    }

    @Test
    public void confirmationDifferenteRefusee() {
        try {
            service.creerPremierCompte("medecin", mdp("Cabinet2026"), mdp("Cabinet2027"));
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("différents"));
        }
        assertTrue(base.logins.isEmpty());
        assertFalse(session.estOuverte());
    }

    @Test
    public void identifiantInvalideRefuse() {
        for (String l : new String[]{"ab", "dr sahab", "médecin", ""}) {
            try {
                AuthentificationService.normaliserLogin(l);
                fail(l);
            } catch (MetierException attendu) {
                // ok
            }
        }
        assertEquals("dr_sahab-2", AuthentificationService.normaliserLogin(" DR_Sahab-2 "));
    }

    // ----- connexion -----

    @Test
    public void connexionReussie() {
        creerCompte(1, "medecin", "Cabinet2026");
        User u = service.connecter(" Medecin ", mdp("Cabinet2026"));
        assertEquals(1, u.getIdent());
        assertTrue(session.estOuverte());
        assertEquals("CONNEXION", derniereAction());
    }

    @Test
    public void mauvaisMotDePasseIdentifiantInconnuEtCompteDesactiveDonnentLeMemeMessage() {
        creerCompte(1, "medecin", "Cabinet2026");
        creerCompte(2, "ancien", "Ancien2026");
        base.inactifs.add(2);
        String[][] essais = {{"medecin", "Mauvais2026"}, {"inconnu", "Cabinet2026"}, {"ancien", "Ancien2026"}};
        for (String[] essai : essais) {
            try {
                service.connecter(essai[0], mdp(essai[1]));
                fail(essai[0]);
            } catch (MetierException e) {
                assertEquals(AuthentificationService.REFUS, e.getMessage());
            }
        }
        assertFalse(session.estOuverte());
        assertEquals(3, base.journal.size());
        for (EntreeJournal e : base.journal) {
            assertEquals("ECHEC_CONNEXION", e.getAction());
            assertNull("personne n'est connecté", e.getIdUtilisateur());
        }
        assertEquals("identifiant : inconnu", base.journal.get(1).getDetail());
    }

    @Test
    public void champsVidesRefusesSansToucherALaBase() {
        int avant = base.requetes;
        try {
            service.connecter("  ", mdp(""));
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("Saisissez"));
        }
        assertEquals(avant, base.requetes);
    }

    @Test
    public void blocageTemporaireApresCinqEchecs() {
        creerCompte(1, "medecin", "Cabinet2026");
        for (int i = 0; i < AuthentificationService.TENTATIVES_MAX; i++) {
            try {
                service.connecter("medecin", mdp("Mauvais2026"));
                fail();
            } catch (MetierException attendu) {
                assertEquals(AuthentificationService.REFUS, attendu.getMessage());
            }
        }
        try {
            service.connecter("medecin", mdp("Cabinet2026"));
            fail("bloqué même avec le bon mot de passe");
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("Trop de tentatives"));
        }
        horloge.maintenant = horloge.maintenant.plus(AuthentificationService.ATTENTE).plus(Duration.ofSeconds(1));
        assertEquals(1, service.connecter("medecin", mdp("Cabinet2026")).getIdent());
    }

    @Test
    public void baseNonMigreeDonneUnMessageClair() {
        base.echouerSur("utilisateur.existeUnCompte",
                new DataAccessException("Erreur SQL", new SQLException("Unknown column 'login'", "42S22", 1054)));
        try {
            service.premiereUtilisation();
            fail();
        } catch (MetierException e) {
            assertEquals(AuthentificationService.BASE_NON_MIGREE, e.getMessage());
        }
    }

    @Test(expected = DataAccessException.class)
    public void uneAutrePanneDeBaseResteUneErreurTechnique() {
        base.echouerSur("utilisateur.existeUnCompte");
        service.premiereUtilisation();
    }

    // ----- outil GestionComptes -----

    @Test
    public void definirIdentifiantsRefuseUnLoginDejaPrisParUnAutre() {
        creerCompte(1, "medecin", "Cabinet2026");
        base.utilisateurs.put(2, Donnees.utilisateur(2));
        try {
            service.definirIdentifiants(2, "medecin", mdp("Secret2026"), mdp("Secret2026"));
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage().contains("déjà utilisé"));
        }
        service.definirIdentifiants(2, "secretaire", mdp("Secret2026"), mdp("Secret2026"));
        assertEquals("secretaire", base.logins.get(2));
        assertEquals("MODIFICATION_COMPTE", derniereAction());
    }

    @Test
    public void unCompteDesactiveNePeutPlusSeConnecter() {
        creerCompte(1, "medecin", "Cabinet2026");
        service.definirActif(1, false);
        try {
            service.connecter("medecin", mdp("Cabinet2026"));
            fail();
        } catch (MetierException e) {
            assertEquals(AuthentificationService.REFUS, e.getMessage());
        }
        assertFalse("désactiver tous les comptes ne rouvre pas la création du premier compte",
                service.premiereUtilisation());
    }
}
