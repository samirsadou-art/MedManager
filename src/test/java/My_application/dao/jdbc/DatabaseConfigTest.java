package My_application.dao.jdbc;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import org.junit.After;
import org.junit.Test;

public class DatabaseConfigTest {

    @After
    public void nettoyer() {
        System.clearProperty("gestimed.db.password");
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
