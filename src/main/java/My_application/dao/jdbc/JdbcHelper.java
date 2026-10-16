package My_application.dao.jdbc;

import My_application.dao.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Petit utilitaire JDBC commun à tous les DAO (équivalent simplifié d'un
 * « JdbcTemplate »).
 * <ul>
 * <li>Toutes les requêtes sont des {@link PreparedStatement} : les valeurs
 * sont passées en paramètres, jamais concaténées dans le SQL. Plus d'injection
 * SQL, et les apostrophes (« maux d'estomac ») ne cassent plus les requêtes.</li>
 * <li>{@code Statement} et {@code ResultSet} sont toujours fermés
 * (try-with-resources) ; l'ancien code en laissait des dizaines ouverts.</li>
 * <li>Les {@link SQLException} sont converties en {@link DataAccessException}.</li>
 * </ul>
 */
public class JdbcHelper {

    private final ConnectionProvider provider;

    public JdbcHelper(ConnectionProvider provider) {
        this.provider = provider;
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
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
        }
    }

    /** Renvoie le premier résultat, ou {@code null} si la requête ne renvoie rien. */
    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> liste = query(sql, mapper, params);
        return liste.isEmpty() ? null : liste.get(0);
    }

    /** INSERT / UPDATE / DELETE ; renvoie le nombre de lignes modifiées. */
    public int update(String sql, Object... params) {
        try {
            Connection c = provider.getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                lier(ps, params);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw erreur(sql, e);
        }
    }

    /**
     * INSERT dans une table à clé auto-incrémentée ; renvoie la clé générée.
     * Remplace l'ancienne technique « insert puis select where date=... » qui
     * pouvait récupérer le mauvais identifiant.
     */
    public int insertAndGetKey(String sql, Object... params) {
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
        }
    }

    /** Exécute la même requête pour chaque jeu de paramètres (lot JDBC). */
    public void batch(String sql, List<Object[]> lignes) {
        if (lignes.isEmpty()) {
            return;
        }
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
            } else {
                ps.setObject(i + 1, v);
            }
        }
    }

    private static DataAccessException erreur(String sql, SQLException e) {
        return new DataAccessException("Erreur SQL (" + e.getMessage() + ") pour : " + sql, e);
    }
}
