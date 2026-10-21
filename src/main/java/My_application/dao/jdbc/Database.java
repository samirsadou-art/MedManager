package My_application.dao.jdbc;

import My_application.dao.DataAccessException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Fournisseur de connexion de l'application de bureau (remplace l'ancienne
 * classe {@code connexion}).
 * <p>
 * Une seule connexion est partagée par toute l'application, comme avant,
 * mais elle est rouverte automatiquement si le serveur l'a fermée (délai
 * d'inactivité MySQL), et aucune boîte de dialogue Swing n'est affichée ici :
 * l'erreur remonte sous forme de {@link DataAccessException} jusqu'au
 * contrôleur, qui seul décide comment prévenir l'utilisateur.
 */
public final class Database implements ConnectionProvider, AutoCloseable {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    private static Database instance;

    private final DatabaseConfig config;
    private Connection connexion;
    private long derniereUtilisation;

    static final int SECONDES_AVANT_VALIDATION = 30;

    public Database(DatabaseConfig config) {
        this.config = config;
        if (config.estCompteAdministrateur()) {
            LOG.warning("Connexion à MySQL avec le compte « " + config.getUser() + " » : utiliser un compte "
                    + "dédié aux droits limités (sql/creer_compte_gestimed.sql).");
        }
    }

    /** Instance unique, configurée à partir de {@code db.proprieties}. */
    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database(DatabaseConfig.charger());
        }
        return instance;
    }

    /**
     * Connexion partagée. Sa validité n'est vérifiée par le serveur
     * ({@code isValid}, un aller-retour réseau) qu'après
     * {@value #SECONDES_AVANT_VALIDATION} s d'inactivité, et non avant chaque
     * requête : c'est après une longue pause que MySQL coupe les connexions
     * inactives.
     */
    @Override
    public synchronized Connection getConnection() throws SQLException {
        long maintenant = System.nanoTime();
        boolean inactive = maintenant - derniereUtilisation > TimeUnit.SECONDS.toNanos(SECONDES_AVANT_VALIDATION);
        if (connexion == null || connexion.isClosed() || (inactive && !estValide(connexion))) {
            connexion = ouvrir();
        }
        derniereUtilisation = maintenant;
        return connexion;
    }

    private static boolean estValide(Connection c) {
        try {
            return c.isValid(2);
        } catch (SQLException | AbstractMethodError e) {
            // Anciens pilotes (Connector/J 5.0) : isValid non implémenté.
            return true;
        }
    }

    private Connection ouvrir() throws SQLException {
        if (config.getDriver() != null && !config.getDriver().isEmpty()) {
            try {
                Class.forName(config.getDriver());
            } catch (ClassNotFoundException e) {
                throw new DataAccessException("Pilote JDBC absent du classpath : " + config.getDriver(), e);
            }
        }
        return DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
    }

    @Override
    public synchronized void close() {
        if (connexion != null) {
            try {
                connexion.close();
            } catch (SQLException ignored) {
                // fermeture en fin d'application : rien d'utile à faire
            }
            connexion = null;
        }
    }
}
