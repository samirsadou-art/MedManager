package My_application.dao.jdbc;

import My_application.dao.DataAccessException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Évolution du schéma par migrations versionnées (Flyway).
 * <p>
 * Les scripts sont dans {@code src/main/resources/db/migration}
 * ({@code V1__schema_initial.sql}, {@code V2__authentification.sql}, ...) et
 * la table {@code flyway_schema_history} retient ceux déjà appliqués.
 * <p>
 * Deux usages :
 * <ul>
 * <li>{@link #migrer()}, avec un compte administrateur (outil
 * {@code My_application.outils.MigrationBase}) : le compte {@code gestimed}
 * de l'application n'a pas le droit de modifier les tables ;</li>
 * <li>{@link #etat()}, au démarrage, avec le compte de l'application : une
 * base en retard sur le code est signalée avant toute saisie.</li>
 * </ul>
 * Une base créée avant Flyway (scripts sql/ manuels) n'a pas d'historique :
 * {@link #migrer()} l'adopte d'abord en déduisant sa version des tables
 * présentes, sans réexécuter ce qui existe déjà.
 */
public final class Migrations {

    private static final Logger LOG = Logger.getLogger("GestiMed");
    /** Flyway est bavard en INFO : seuls ses avertissements vont au journal (référence gardée). */
    private static final Logger LOG_FLYWAY = Logger.getLogger("org.flywaydb");

    static {
        LOG_FLYWAY.setLevel(Level.WARNING);
    }

    public static final String EMPLACEMENT = "classpath:db/migration";
    static final String TABLE_HISTORIQUE = "flyway_schema_history";

    private final String url;
    private final String user;
    private final String password;

    public Migrations(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Migrations(DatabaseConfig config) {
        this(config.getUrl(), config.getUser(), config.getPassword());
    }

    /** Configuration commune ; {@code clean} reste interdit (valeur par défaut). */
    public static FluentConfiguration configuration(String url, String user, String password) {
        return Flyway.configure()
                .dataSource(url, user, password)
                .locations(EMPLACEMENT)
                .table(TABLE_HISTORIQUE);
    }

    /** Situation de la base par rapport aux migrations livrées avec le code. */
    public static final class Etat {
        private final boolean suivie;
        private final String versionBase;
        private final String versionCode;
        private final int enAttente;

        Etat(boolean suivie, String versionBase, String versionCode, int enAttente) {
            this.suivie = suivie;
            this.versionBase = versionBase;
            this.versionCode = versionCode;
            this.enAttente = enAttente;
        }

        /** Faux pour une base créée avant Flyway (pas encore d'historique). */
        public boolean estSuivie() {
            return suivie;
        }

        /** Version appliquée, ou null si la base n'est pas suivie. */
        public String getVersionBase() {
            return versionBase;
        }

        /** Dernière version livrée avec le code. */
        public String getVersionCode() {
            return versionCode;
        }

        public int getEnAttente() {
            return enAttente;
        }

        /** Vrai si la base suivie a des migrations à appliquer : le code ne peut pas s'en servir. */
        public boolean estEnRetard() {
            return suivie && enAttente > 0;
        }

        @Override
        public String toString() {
            return suivie
                    ? "base en version " + versionBase + ", code en version " + versionCode
                    + (enAttente > 0 ? " (" + enAttente + " migration(s) à appliquer)" : " (à jour)")
                    : "base non suivie par les migrations, code en version " + versionCode;
        }
    }

    /** Lecture seule : utilisable avec le compte de l'application. */
    public Etat etat() {
        try {
            MigrationInfoService info = configuration(url, user, password).load().info();
            MigrationInfo courante = info.current();
            // Dernière version livrée avec le code : seules comptent les migrations
            // présentes dans le classpath (pas celles que seule la base connaît).
            MigrationVersion versionCode = null;
            for (MigrationInfo m : info.all()) {
                if (m.getVersion() != null && m.getState().isResolved()
                        && (versionCode == null || m.getVersion().compareTo(versionCode) > 0)) {
                    versionCode = m.getVersion();
                }
            }
            return new Etat(courante != null,
                    courante == null ? null : courante.getVersion().getVersion(),
                    versionCode == null ? null : versionCode.getVersion(), info.pending().length);
        } catch (FlywayException e) {
            throw new DataAccessException("Lecture de la version du schéma impossible", e);
        }
    }

    /**
     * Applique les migrations en attente (compte administrateur). Une base
     * existante sans historique est d'abord adoptée à sa version réelle.
     *
     * @return le nombre de migrations appliquées
     */
    public int migrer() {
        try {
            String versionExistante = versionDUneBaseNonSuivie();
            if (versionExistante != null) {
                LOG.info("Base existante sans historique de migrations : adoptée en version " + versionExistante);
                configuration(url, user, password)
                        .baselineVersion(versionExistante)
                        .baselineDescription("Base existante adoptée")
                        .load().baseline();
            }
            int appliquees = configuration(url, user, password).load().migrate().migrationsExecuted;
            LOG.info("Migrations appliquées : " + appliquees);
            return appliquees;
        } catch (FlywayException e) {
            throw new DataAccessException("Échec de la migration du schéma : " + e.getMessage(), e);
        }
    }

    /**
     * Version d'une base créée avant Flyway, déduite de ses tables ; null si
     * la base a déjà un historique ou si elle est vide (tout sera appliqué).
     */
    String versionDUneBaseNonSuivie() {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            if (tableExiste(c, TABLE_HISTORIQUE) || !tableExiste(c, "utilisateur")) {
                return null;
            }
            // V2 a créé journal_acces (sql/migration_002_authentification.sql avant Flyway).
            return tableExiste(c, "journal_acces") ? "2" : "1";
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des tables existantes impossible", e);
        }
    }

    private static boolean tableExiste(Connection c, String table) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
