package My_application.dao.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/** Sans serveur : état du schéma et scripts livrés dans db/migration. */
public class MigrationsTest {

    @Test
    public void uneBaseSuivieAvecDesMigrationsEnAttenteEstEnRetard() {
        Migrations.Etat etat = new Migrations.Etat(true, "1", "2", 1);
        assertTrue(etat.estEnRetard());
        assertTrue(etat.toString().contains("1 migration(s) à appliquer"));
    }

    @Test
    public void uneBaseNonSuivieNEstPasBloquee() {
        // Base créée avant Flyway : tout paraît « en attente », mais rien ne doit
        // bloquer l'application tant qu'elle n'a pas été adoptée par MigrationBase.
        Migrations.Etat etat = new Migrations.Etat(false, null, "2", 2);
        assertFalse(etat.estEnRetard());
        assertFalse(etat.estSuivie());
    }

    @Test
    public void lesMigrationsSontNumeroteesSansTrouNiDoublon() throws Exception {
        File dossier = new File(Migrations.class.getResource("/db/migration").toURI());
        List<Integer> versions = new ArrayList<>();
        for (String nom : dossier.list()) {
            Matcher m = Pattern.compile("V(\\d+)__[a-z0-9_]+\\.sql").matcher(nom);
            assertTrue("Nom de migration non conforme : " + nom, m.matches());
            versions.add(Integer.parseInt(m.group(1)));
        }
        Collections.sort(versions);
        for (int i = 0; i < versions.size(); i++) {
            assertEquals(Integer.valueOf(i + 1), versions.get(i));
        }
        assertTrue(versions.size() >= 2);
    }

    @Test
    public void aucuneMigrationNeSupprimeDeDonnees() throws Exception {
        File dossier = new File(Migrations.class.getResource("/db/migration").toURI());
        for (File f : dossier.listFiles()) {
            String sql = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).toUpperCase();
            assertFalse(f.getName(), sql.matches("(?s).*\\b(DROP\\s+(TABLE|DATABASE)|TRUNCATE)\\b.*"));
        }
    }
}
