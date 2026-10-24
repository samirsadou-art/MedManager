package My_application.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.MessageInterne;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.fake.Donnees;
import My_application.fake.FausseVue;
import My_application.fake.MemoireDAOFactory;
import My_application.service.MessagerieService;
import My_application.service.MessagerieService.Conversation;
import My_application.service.PatientService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Messagerie avec une fausse fenêtre. */
public class MessagerieControllerTest {

    private MemoireDAOFactory base;

    static final class FausseMessagerie extends FausseVue implements MessagerieVue {
        List<Conversation> conversations;
        List<MessageInterne> messages;
        Integer fil;
        final List<Integer> nonLus = new ArrayList<>();
        int sons;
        boolean affichee = true;

        @Override
        public void afficherConversations(List<Conversation> c) {
            conversations = c;
        }

        @Override
        public void afficherMessages(Integer autre, List<MessageInterne> m) {
            fil = autre;
            messages = m;
        }

        @Override
        public void afficherNonLus(int total) {
            nonLus.add(total);
        }

        @Override
        public void signalerNouveauMessage() {
            sons++;
        }

        @Override
        public boolean estAffichee() {
            return affichee;
        }
    }

    @Before
    public void preparer() {
        base = Donnees.base();
        for (int id = 1; id <= 3; id++) {
            User u = new User(id, "MEMBRE" + id, "p", "");
            u.setRole(id == 3 ? Role.SECRETAIRE : Role.MEDECIN);
            base.utilisateurs.put(id, u);
            base.logins.put(id, "m" + id);
        }
    }

    private MessagerieService service(int moi) {
        return new MessagerieService(base, () -> moi);
    }

    private MessagerieController controleur(FausseMessagerie vue, int moi) {
        return new MessagerieController(vue, service(moi), new PatientService(base), Executeur.IMMEDIAT);
    }

    @Test
    public void ouvrirUnFilLeMarqueLuEtMetAJourLeBadge() {
        service(1).envoyer(3, "bonjour", null);
        FausseMessagerie vue = new FausseMessagerie();
        MessagerieController c = controleur(vue, 3);
        c.rafraichir();
        assertNull("aucun fil choisi : rien n'est lu", vue.messages);
        assertEquals(Collections.singletonList(1), vue.nonLus);
        c.ouvrir(1);
        assertEquals(Integer.valueOf(1), vue.fil);
        assertEquals(1, vue.messages.size());
        assertEquals(Integer.valueOf(0), vue.nonLus.get(vue.nonLus.size() - 1));
        assertEquals(0, vue.sons);
    }

    @Test
    public void leMinuteurSonneQuandUnMessageArrive() {
        FausseMessagerie vue = new FausseMessagerie();
        MessagerieController c = controleur(vue, 3);
        c.verifier();
        int lectures = base.requetes;
        c.verifier();
        assertEquals("rien de nouveau : une seule requête", lectures + 1, base.requetes);

        service(1).envoyer(null, "réunion", null);
        c.verifier();
        assertEquals(1, vue.sons);
        assertEquals(Integer.valueOf(1), vue.nonLus.get(vue.nonLus.size() - 1));

        // Fil ouvert et fenêtre affichée : le message suivant est lu aussitôt.
        c.ouvrir(null);
        service(2).envoyer(null, "j'arrive", null);
        c.verifier();
        assertEquals(2, vue.messages.size());
        assertEquals(Integer.valueOf(0), vue.nonLus.get(vue.nonLus.size() - 1));

        // Fenêtre masquée : il reste non lu.
        vue.affichee = false;
        service(2).envoyer(null, "encore", null);
        c.verifier();
        assertEquals(2, vue.messages.size());
        assertEquals(Integer.valueOf(1), vue.nonLus.get(vue.nonLus.size() - 1));
        assertEquals(2, vue.sons);
    }

    @Test
    public void unePanneDuMinuteurNAfficheRien() {
        FausseMessagerie vue = new FausseMessagerie();
        MessagerieController c = controleur(vue, 3);
        base.echouerSur("message.dernier");
        c.verifier();
        assertTrue(vue.erreurs.isEmpty());
        assertTrue(vue.nonLus.isEmpty());
        c.verifier();
        assertEquals(1, vue.nonLus.size());
    }

    @Test
    public void envoyerDansLeFilOuvert() {
        FausseMessagerie vue = new FausseMessagerie();
        MessagerieController c = controleur(vue, 3);
        assertFalse(c.envoyer("sans fil", null));
        assertEquals(1, vue.erreurs.size());

        c.ouvrir(2);
        assertTrue(c.envoyer("Le patient de 11 h annule.", base.patients.get(1)));
        assertEquals(1, vue.messages.size());
        assertEquals(Integer.valueOf(2), base.messages.get(0).getIdDestinataire());
        assertFalse(c.envoyer("  ", null));
        assertEquals(2, vue.erreurs.size());
        assertEquals("Le message est vide.", vue.erreurs.get(1));
    }

    @Test
    public void citerEtOuvrirUnPatient() {
        FausseMessagerie vue = new FausseMessagerie();
        MessagerieController c = controleur(vue, 3);
        assertEquals(1, c.rechercherPatients("AMR").size());
        assertEquals("AMRANI", c.fichePatient(Donnees.patient(1, "?")).getNom());
        base.echouerSur("patient.findByNom");
        assertTrue(c.rechercherPatients("AMR").isEmpty());
        assertEquals(1, vue.erreurs.size());
    }

    @Test
    public void presencePurgeEtFermeture() {
        FausseMessagerie vue = new FausseMessagerie();
        MessagerieController c = controleur(vue, 3);
        c.signalerPresence("ACCUEIL");
        assertEquals("ACCUEIL", base.presents.get(3));
        c.purger();
        c.quitter();
        assertTrue(base.presents.isEmpty());
        base.echouerSur("presence.signaler");
        c.signalerPresence("ACCUEIL");
        base.echouerSur("presence.quitter");
        c.quitter();
        assertTrue(vue.erreurs.isEmpty());
    }
}
