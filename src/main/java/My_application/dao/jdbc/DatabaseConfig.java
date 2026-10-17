package My_application.dao.jdbc;

import My_application.dao.DataAccessException;
import java.io.File;
import java.io.FileInputStream;
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
 * <p>
 * Ordre de priorité (du plus fort au plus faible) :
 * <ol>
 * <li>propriétés système {@code -Dgestimed.db.*} ;</li>
 * <li>fichier local {@value #FICHIER_EXTERNE} : chemin donné par
 * {@code -Dgestimed.config}, sinon dans le répertoire courant, sinon dans le
 * répertoire de l'utilisateur ;</li>
 * <li>ressource {@value #RESSOURCE_PAR_DEFAUT} du classpath (sans mot de passe).</li>
 * </ol>
 */
public final class DatabaseConfig {

    public static final String RESSOURCE_PAR_DEFAUT = "/My_application/Files_Res/db.proprieties";

    /** Fichier local, hors du jar et hors de git, qui peut contenir le mot de passe. */
    public static final String FICHIER_EXTERNE = "gestimed.properties";

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
        File local = fichierLocal();
        if (local != null) {
            surcharger(p, local);
        }
        return depuisProperties(p);
    }

    /** Fichier de configuration local, ou null s'il n'y en a pas. */
    static File fichierLocal() {
        String chemin = System.getProperty("gestimed.config");
        if (chemin != null) {
            File f = new File(chemin);
            if (!f.isFile()) {
                throw new DataAccessException("Fichier de configuration introuvable : " + f.getAbsolutePath());
            }
            return f;
        }
        File[] candidats = {
            new File(FICHIER_EXTERNE),
            new File(System.getProperty("user.home"), FICHIER_EXTERNE)
        };
        for (File f : candidats) {
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    /** Les valeurs du fichier remplacent celles déjà présentes dans {@code p}. */
    static void surcharger(Properties p, File fichier) {
        Properties locales = new Properties();
        try (InputStream in = new FileInputStream(fichier)) {
            locales.load(in);
        } catch (IOException e) {
            throw new DataAccessException("Lecture impossible de " + fichier.getAbsolutePath(), e);
        }
        p.putAll(locales);
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
