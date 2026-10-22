package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.entities.MessageInterne;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import My_application.service.MessagerieService.Conversation;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class MessagerieServiceTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 22, 9, 30);

    private MemoireDAOFactory base;
    private Clock horloge;

    @Before
    public void preparer() {
        base = Donnees.base();
        ZoneId zone = ZoneId.systemDefault();
        horloge = Clock.fixed(MAINTENANT.atZone(zone).toInstant(), zone);
        membre(1, "SAHAB", Role.MEDECIN);
        membre(2, "AMRANI", Role.MEDECIN);
        membre(3, "BELKACEM", Role.SECRETAIRE);
        membre(4, "ADMIN", Role.ADMIN);
        membre(5, "ANCIEN", Role.SECRETAIRE);
        base.inactifs.add(5);
        // Membre sans identifiants : il ne peut pas lire, on ne lui écrit pas.
        base.utilisateurs.put(6, new User(6, "SANSCOMPTE", "x", ""));
    }

    private void membre(int id, String nom, Role role) {
        User u = new User(id, nom, "Prénom" + id, "");
        u.setRole(role);
        base.utilisateurs.put(id, u);
        base.logins.put(id, nom.toLowerCase());
    }

    /** Service tel que le voit l'utilisateur {@code id}. */
    private MessagerieService de(final int id) {
        return new MessagerieService(base, () -> id, ControleAcces.AUCUN, horloge);
    }

    @Test
    public void lesFilsSontToutLeCabinetPuisLesMedecinsPuisLesAutres() {
        List<Conversation> l = de(3).conversations();
        assertEquals(3, l.size());
        assertTrue(l.get(0).isTousLeCabinet());
        assertNull(l.get(0).getIdAutre());
        assertEquals("AMRANI Prénom2", l.get(1).getLibelle());
        assertEquals("Médecin", l.get(1).getDetail());
        assertEquals("SAHAB Prénom1", l.get(2).getLibelle());
    }

    @Test
    public void unMessagePriveEstNonLuJusquALaLecture() {
        MessageInterne m = de(3).envoyer(1, "  Le patient de 10 h est arrivé.  ", Donnees.patient(1, "AMRANI"));
        assertEquals("Le patient de 10 h est arrivé.", m.getTexte());
        assertEquals(MAINTENANT, m.getEnvoyeLe());

        List<Conversation> chezLeMedecin = de(1).conversations();
        assertEquals(1, MessagerieService.total(chezLeMedecin));
        assertEquals("non-lu dans le fil de la secrétaire", 1, nonLusDe(chezLeMedecin, 3));
        assertEquals("rien chez l'expéditeur", 0, MessagerieService.total(de(3).conversations()));
        assertEquals("ni chez un autre médecin", 0, MessagerieService.total(de(2).conversations()));

        List<MessageInterne> fil = de(1).lire(3);
        assertEquals(1, fil.size());
        assertEquals("BELKACEM Prénom3", fil.get(0).getNomExpediteur());
        assertEquals(1, fil.get(0).getPatient().getIdentPat());
        assertEquals(0, MessagerieService.total(de(1).conversations()));
        assertTrue("accusé de lecture", de(3).lire(1).get(0).isLuParDestinataire());
    }

    private static int nonLusDe(List<Conversation> l, int membre) {
        for (Conversation c : l) {
            if (c.getIdAutre() != null && c.getIdAutre() == membre) {
                return c.getNonLus();
            }
        }
        return -1;
    }

    @Test
    public void unMessageATousEstNonLuChezChacunSaufLExpediteur() {
        de(1).envoyer(null, "Réunion à 13 h", null);
        assertEquals(1, de(2).conversations().get(0).getNonLus());
        assertEquals(1, de(3).conversations().get(0).getNonLus());
        assertEquals(0, de(1).conversations().get(0).getNonLus());
        de(2).lire(null);
        assertEquals(0, de(2).conversations().get(0).getNonLus());
        assertEquals(1, de(3).conversations().get(0).getNonLus());
        assertTrue(de(1).dernierMessage() > 0);
    }

    @Test
    public void lesFilsPrivesNeSeMelentPas() {
        de(1).envoyer(3, "pour la secrétaire", null);
        de(2).envoyer(3, "de l'autre médecin", null);
        de(3).envoyer(1, "réponse", null);
        List<MessageInterne> fil = de(3).lire(1);
        assertEquals(2, fil.size());
        assertEquals("pour la secrétaire", fil.get(0).getTexte());
        assertEquals("réponse", fil.get(1).getTexte());
        assertEquals("le message du Dr AMRANI reste non lu", 1, de(3).conversations().get(1).getNonLus());
    }

    @Test
    public void messagesRefuses() {
        refuse(() -> de(1).envoyer(null, "   ", null), "Le message est vide.");
        refuse(() -> de(1).envoyer(null, null, null), "Le message est vide.");
        StringBuilder long_ = new StringBuilder();
        for (int i = 0; i <= MessagerieService.LONGUEUR_MAX; i++) {
            long_.append('x');
        }
        refuse(() -> de(1).envoyer(null, long_.toString(), null), "trop long");
        refuse(() -> de(1).envoyer(5, "compte désactivé", null), "plus de compte actif");
        refuse(() -> de(1).envoyer(4, "administrateur", null), "plus de compte actif");
        refuse(() -> de(1).envoyer(1, "à soi-même", null), "plus de compte actif");
        assertTrue(base.messages.isEmpty());
    }

    private static void refuse(Runnable action, String extrait) {
        try {
            action.run();
            fail("refus attendu : " + extrait);
        } catch (MetierException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(extrait));
        }
    }

    @Test
    public void sansConnexionPasDeMessagerie() {
        MessagerieService anonyme = new MessagerieService(base, () -> null);
        refuse(anonyme::conversations, MessagerieService.NON_CONNECTE);
        refuse(() -> anonyme.envoyer(null, "x", null), MessagerieService.NON_CONNECTE);
        anonyme.quitter();
        assertTrue(base.presents.isEmpty());
    }

    @Test
    public void lAdministrateurNUtilisePasLaMessagerie() {
        Session session = new Session(base, 1, 1);
        session.ouvrir(base.utilisateurs.get(4));
        MessagerieService s = new MessagerieService(base, session::getIdUtilisateurConnecte, session, horloge);
        refuse(s::conversations, "secrétaire, médecin");
    }

    @Test
    public void presenceDesMembres() {
        de(2).signalerPresence("CABINET-2");
        assertEquals("CABINET-2", base.presents.get(2));
        List<Conversation> l = de(3).conversations();
        assertTrue(l.get(1).isEnLigne());
        assertFalse(l.get(2).isEnLigne());
        de(2).quitter();
        assertFalse(de(3).conversations().get(1).isEnLigne());
    }

    @Test
    public void lesMessagesDePlusDUnAnSontSupprimes() {
        de(1).envoyer(null, "ancien", null);
        base.messages.get(0).setEnvoyeLe(MAINTENANT.minusMonths(13));
        de(1).envoyer(null, "récent", null);
        assertEquals(1, de(1).purger());
        assertEquals("récent", base.messages.get(0).getTexte());
    }

    @Test
    public void unFilNAfficheQueLesDerniersMessages() {
        for (int i = 0; i < MessagerieService.LIMITE_FIL + 5; i++) {
            de(1).envoyer(null, "message " + i, null);
        }
        List<MessageInterne> fil = de(2).lire(null);
        assertEquals(MessagerieService.LIMITE_FIL, fil.size());
        assertEquals("message " + (MessagerieService.LIMITE_FIL + 4), fil.get(fil.size() - 1).getTexte());
        assertEquals("tout le fil est marqué lu", 0, de(2).conversations().get(0).getNonLus());
    }
}
