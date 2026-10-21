package My_application.dao.jdbc;

import My_application.dao.DataAccessException;
import My_application.dao.DoublonException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Petit utilitaire JDBC commun à tous les DAO (équivalent simplifié d'un
 * « JdbcTemplate »).
 * <ul>
 * <li>Toutes les requêtes sont des {@link PreparedStatement} : les valeurs
 * sont passées en paramètres, jamais concaténées dans le SQL. Plus d'injection
 * SQL, et les apostrophes (« maux d'estomac ») ne cassent plus les requêtes.</li>
 * <li>{@code Statement} et {@code ResultSet} sont toujours fermés
 * (try-with-resources) ; l'ancien code en laissait des dizaines ouverts.</li>
 * <li>Les {@link SQLException} sont converties en {@link DataAccessException}
 * ({@link DoublonException} pour une clé déjà prise).</li>
 * <li>Chaque requête prend le verrou d'accès à la base (celui du
 * {@link TransactionManager}) : la connexion partagée ne sert qu'à un thread
 * à la fois.</li>
 * </ul>
 */
public class JdbcHelper {

    private final ConnectionProvider provider;
    private final Lock verrou;

    public JdbcHelper(ConnectionProvider provider) {
        this(provider, new ReentrantLock());
    }

    public JdbcHelper(ConnectionProvider provider, Lock verrou) {
        this.provider = provider;
        this.verrou = verrou;
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        verrou.lock();
        try {
            Connection c = provider.getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                lier(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> resultat = new ArrayList<>();
                    while (rs.next()) {
                        resultat.add(mapper.map(rs));
                    }
                    return resultat;
                }
            }
        } catch (SQLException e) {
            throw erreur(sql, e);
        } finally {
            verrou.unlock();
        }
    }

    /**
     * Requête dont les lignes sont regroupées selon la valeur entière de
     * {@code colonneCle} (ordre des lignes conservé). Sert à charger en une
     * requête les lignes de toutes les consultations d'un patient.
     */
    public <T> Map<Integer, List<T>> queryParCle(String sql, final String colonneCle,
            final RowMapper<T> mapper, Object... params) {
        final Map<Integer, List<T>> groupes = new LinkedHashMap<>();
        query(sql, new RowMapper<Void>() {
            @Override
            public Void map(ResultSet rs) throws SQLException {
                int cle = rs.getInt(colonneCle);
                List<T> groupe = groupes.get(cle);
                if (groupe == null) {
                    groupe = new ArrayList<>();
                    groupes.put(cle, groupe);
                }
                groupe.add(mapper.map(rs));
                return null;
            }
        }, params);
        return groupes;
    }

    /** Renvoie le premier résultat, ou {@code null} si la requête ne renvoie rien. */
    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> liste = query(sql, mapper, params);
        return liste.isEmpty() ? null : liste.get(0);
    }

    /** INSERT / UPDATE / DELETE ; renvoie le nombre de lignes modifiées. */
    public int update(String sql, Object... params) {
        verrou.lock();
        try {
            Connection c = provider.getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                lier(ps, params);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw erreur(sql, e);
        } finally {
            verrou.unlock();
        }
    }

    /**
     * INSERT dans une table à clé auto-incrémentée ; renvoie la clé générée.
     * Remplace l'ancienne technique « insert puis select where date=... » qui
     * pouvait récupérer le mauvais identifiant.
     */
    public int insertAndGetKey(String sql, Object... params) {
        verrou.lock();
        try {
            Connection c = provider.getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                lier(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
                throw new DataAccessException("Aucune clé générée pour : " + sql);
            }
        } catch (SQLException e) {
            throw erreur(sql, e);
        } finally {
            verrou.unlock();
        }
    }

    /** Exécute la même requête pour chaque jeu de paramètres (lot JDBC). */
    public void batch(String sql, List<Object[]> lignes) {
        if (lignes.isEmpty()) {
            return;
        }
        verrou.lock();
        try {
            Connection c = provider.getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (Object[] params : lignes) {
                    lier(ps, params);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw erreur(sql, e);
        } finally {
            verrou.unlock();
        }
    }

    /** Échappe les jokers de LIKE pour une recherche « commence par ». */
    public static String commencePar(String prefixe) {
        String p = prefixe == null ? "" : prefixe;
        p = p.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return p + "%";
    }

    private static void lier(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object v = params[i];
            if (v == null) {
                ps.setNull(i + 1, Types.NULL);
            } else if (v instanceof Boolean) {
                ps.setBoolean(i + 1, (Boolean) v);
            } else if (v instanceof LocalDate) {
                // Types java.time du domaine : convertis ici, pilote JDBC 4.2 non requis.
                ps.setDate(i + 1, Date.valueOf((LocalDate) v));
            } else if (v instanceof LocalTime) {
                ps.setTime(i + 1, Time.valueOf((LocalTime) v));
            } else if (v instanceof LocalDateTime) {
                ps.setTimestamp(i + 1, Timestamp.valueOf((LocalDateTime) v));
            } else {
                ps.setObject(i + 1, v);
            }
        }
    }

    /** Code d'erreur MySQL/MariaDB « Duplicate entry » (ER_DUP_ENTRY). */
    static final int MYSQL_DOUBLON = 1062;

    private static DataAccessException erreur(String sql, SQLException e) {
        String message = "Erreur SQL (" + e.getMessage() + ") pour : " + sql;
        if (e.getErrorCode() == MYSQL_DOUBLON) {
            return new DoublonException(message, e);
        }
        return new DataAccessException(message, e);
    }
}
