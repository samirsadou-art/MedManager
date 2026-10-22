package My_application.outils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import My_application.AppContext;
import My_application.entities.Role;
import My_application.entities.User;
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
        base.medecins.put(1, Donnees.medecin(1));
        base.utilisateurs.put(2, Donnees.utilisateur(2));
        ctx = new AppContext(base);
    }

    /** Mot de passe tapé au clavier (null : pas de console). */
    private String tape = "Accueil2026";

    private int lancer(String... args) throws Exception {
        return GestionComptes.executer(args, ctx, new PrintStream(sortie, true, "UTF-8"),
                new PrintStream(erreurs, true, "UTF-8"), invite -> tape == null ? null : tape.toCharArray());
    }

    private String sortie() {
        return new String(sortie.toByteArray(), StandardCharsets.UTF_8);
    }

    private String erreurs() {
        return new String(erreurs.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    public void ajouterCreeLeMembreEtSesIdentifiants() throws Exception {
        assertEquals(0, lancer("ajouter", "secretaire", "Amina", "benali", "amina"));
        User cree = base.utilisateurs.get(3);
        assertEquals(Role.SECRETAIRE, cree.getRole());
        assertEquals("BENALI", cree.getNom());
        assertEquals("amina", base.logins.get(3));
        assertTrue(sortie(), sortie().contains("identifiant amina"));
        assertEquals(0, lancer("ajouter", "medecin", "dr.amrani", "amrani", "lina", "1"));
        assertEquals(Integer.valueOf(1), base.utilisateurs.get(4).getIdMedecin());
        assertEquals(0, lancer("liste"));
        assertTrue(sortie(), sortie().contains("Secrétaire"));
    }

    @Test
    public void ajouterRefuseUnIdentifiantPrisAvantDeCreerLeMembre() throws Exception {
        assertEquals(0, lancer("ajouter", "admin", "admin", "admin", "cabinet"));
        int membres = base.utilisateurs.size();
        assertEquals(1, lancer("ajouter", "secretaire", "admin", "benali", "amina"));
        assertEquals(membres, base.utilisateurs.size());
        assertTrue(erreurs(), erreurs().contains("déjà utilisé"));
    }

    @Test
    public void roleInconnuOuSansConsole() throws Exception {
        assertEquals(1, lancer("ajouter", "chef", "x.y", "nom", "prenom"));
        assertTrue(erreurs(), erreurs().contains("Rôle inconnu"));
        tape = null;
        assertEquals(1, lancer("ajouter", "secretaire", "amina", "benali", "amina"));
        assertTrue(erreurs(), erreurs().contains("Pas de console"));
        assertEquals(1, lancer("definir", "2", "amina"));
    }

    @Test
    public void definirEtChangerDeRole() throws Exception {
        assertEquals(0, lancer("definir", "2", "amina"));
        assertEquals("amina", base.logins.get(2));
        assertEquals(0, lancer("role", "2", "secretaire"));
        assertEquals(Role.SECRETAIRE, base.utilisateurs.get(2).getRole());
        assertEquals(0, lancer("role", "2", "medecin", "1"));
        assertEquals(Integer.valueOf(1), base.utilisateurs.get(2).getIdMedecin());
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
