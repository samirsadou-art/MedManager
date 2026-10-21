package My_application.outils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import My_application.AppContext;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;

/** L'outil reçoit son assemblage : il se teste sans MySQL. */
public class GestionComptesTest {

    private final MemoireDAOFactory base = new MemoireDAOFactory();
    private final ByteArrayOutputStream sortie = new ByteArrayOutputStream();
    private final ByteArrayOutputStream erreurs = new ByteArrayOutputStream();
    private AppContext ctx;

    @Before
    public void preparer() {
        base.utilisateurs.put(2, Donnees.utilisateur(2));
        ctx = new AppContext(base);
    }

    private int lancer(String... args) throws Exception {
        return GestionComptes.executer(args, ctx, new PrintStream(sortie, true, "UTF-8"),
                new PrintStream(erreurs, true, "UTF-8"));
    }

    @Test
    public void listeAfficheLesUtilisateursSansIdentifiant() throws Exception {
        assertEquals(0, lancer("liste"));
        String texte = new String(sortie.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(texte, texte.contains("(aucun)"));
        assertTrue(texte, texte.contains("actif"));
    }

    @Test
    public void desactiverPuisActiverUnCompte() throws Exception {
        assertEquals(0, lancer("desactiver", "2"));
        assertTrue(base.inactifs.contains(2));
        assertEquals(0, lancer("activer", "2"));
        assertFalse(base.inactifs.contains(2));
    }

    @Test
    public void sansCommandeAfficheLUsage() throws Exception {
        assertEquals(2, lancer());
        assertFalse(new String(erreurs.toByteArray(), StandardCharsets.UTF_8).isEmpty());
    }
}
