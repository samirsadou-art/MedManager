package My_application.dao.jdbc;

import My_application.dao.DataAccessException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Paramètres de connexion à la base de données.
 * <p>
 * Le fichier est lu dans le classpath avec {@code getResourceAsStream} :
 * contrairement à l'ancien {@code new FileInputStream(getResource(..).getFile())},
 * cela fonctionne aussi quand l'application est empaquetée dans un .jar.
 * Chaque valeur peut être surchargée au lancement par une propriété système
 * (ex. {@code -Dgestimed.db.password=xxx}), ce qui évite de laisser un mot de
 * passe de production dans les sources.
 */
public final class DatabaseConfig {

    public static final String RESSOURCE_PAR_DEFAUT = "/My_application/Files_Res/db.proprieties";

    private final String url;
    private final String user;
    private final String password;
    private final String driver;

    public DatabaseConfig(String url, String user, String password, String driver) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.driver = driver;
    }

    /** Charge la configuration depuis le fichier de propriétés du classpath. */
    public static DatabaseConfig charger() {
        return charger(RESSOURCE_PAR_DEFAUT);
    }

    public static DatabaseConfig charger(String ressource) {
        Properties p = new Properties();
        try (InputStream in = DatabaseConfig.class.getResourceAsStream(ressource)) {
            if (in == null) {
                throw new DataAccessException("Fichier de configuration introuvable : " + ressource);
            }
            p.load(in);
        } catch (IOException e) {
            throw new DataAccessException("Lecture impossible de " + ressource, e);
        }
        return depuisProperties(p);
    }

    static DatabaseConfig depuisProperties(Properties p) {
        String base = valeur(p, "url", "jdbc:mysql://localhost:3306/");
        String nomBase = valeur(p, "dbName", "");
        String parametres = valeur(p, "parametres", "");
        String url = System.getProperty("gestimed.db.url",
                base + nomBase + (parametres.isEmpty() ? "" : "?" + parametres));
        String user = System.getProperty("gestimed.db.user", valeur(p, "userName", "root"));
        String password = System.getProperty("gestimed.db.password", valeur(p, "password", ""));
        String driver = System.getProperty("gestimed.db.driver", valeur(p, "driver", ""));
        return new DatabaseConfig(url, user, password, driver);
    }

    private static String valeur(Properties p, String cle, String defaut) {
        String v = p.getProperty(cle);
        return v == null ? defaut : v.trim();
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDriver() {
        return driver;
    }
}
