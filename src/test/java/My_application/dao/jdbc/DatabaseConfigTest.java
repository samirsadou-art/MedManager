package My_application.dao.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import org.junit.After;
import org.junit.Test;

public class DatabaseConfigTest {

    @After
    public void nettoyer() {
        System.clearProperty("gestimed.db.password");
        System.clearProperty("gestimed.config");
    }

    private static File fichierLocal(String contenu) throws IOException {
        File f = File.createTempFile("gestimed", ".properties");
        f.deleteOnExit();
        Files.write(f.toPath(), contenu.getBytes(StandardCharsets.ISO_8859_1));
        return f;
    }

    @Test
    public void leFichierLocalSurchargeLaRessource() throws IOException {
        File f = fichierLocal("password = du-fichier-local\ndbName = autre\n");
        System.setProperty("gestimed.config", f.getAbsolutePath());
        DatabaseConfig c = DatabaseConfig.charger();
        assertEquals("du-fichier-local", c.getPassword());
        assertTrue(c.getUrl().startsWith("jdbc:mysql://localhost:3306/autre?"));
        assertEquals("root", c.getUser());
    }

    @Test
    public void unePropriteSystemeSurchargeLeFichierLocal() throws IOException {
        File f = fichierLocal("password = du-fichier-local\n");
        System.setProperty("gestimed.config", f.getAbsolutePath());
        System.setProperty("gestimed.db.password", "secret");
        assertEquals("secret", DatabaseConfig.charger().getPassword());
    }

    @Test(expected = My_application.dao.DataAccessException.class)
    public void fichierLocalDemandeMaisIntrouvable() {
        System.setProperty("gestimed.config", "inexistant-gestimed.properties");
        DatabaseConfig.charger();
    }

    @Test
    public void laRessourceDuProjetNeContientPasDeMotDePasse() {
        Properties p = new Properties();
        try (java.io.InputStream in = DatabaseConfig.class.getResourceAsStream(DatabaseConfig.RESSOURCE_PAR_DEFAUT)) {
            p.load(in);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        assertEquals("", p.getProperty("password", "").trim());
    }

    @Test
    public void lectureDuFichierDuProjet() {
        DatabaseConfig c = DatabaseConfig.charger();
        assertEquals("jdbc:mysql://localhost:3306/medicale?useSSL=false&allowPublicKeyRetrieval=true"
                + "&characterEncoding=UTF-8", c.getUrl());
        assertEquals("root", c.getUser());
        assertEquals("com.mysql.cj.jdbc.Driver", c.getDriver());
    }

    @Test
    public void unePropriteSystemeSurchargeLeFichier() {
        Properties p = new Properties();
        p.setProperty("password", "dans-le-fichier");
        System.setProperty("gestimed.db.password", "secret");
        assertEquals("secret", DatabaseConfig.depuisProperties(p).getPassword());
    }

    @Test
    public void sansParametresLUrlEstInchangee() {
        Properties p = new Properties();
        p.setProperty("url", "jdbc:mysql://serveur:3306/");
        p.setProperty("dbName", "medicale");
        assertEquals("jdbc:mysql://serveur:3306/medicale", DatabaseConfig.depuisProperties(p).getUrl());
    }

    @Test(expected = My_application.dao.DataAccessException.class)
    public void fichierIntrouvable() {
        DatabaseConfig.charger("/inexistant.properties");
    }
}
