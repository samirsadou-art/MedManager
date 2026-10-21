package My_application.fake;

import My_application.dao.jdbc.ConnectionProvider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Faux pilote JDBC (proxies dynamiques) pour tester les DAO JDBC sans MySQL.
 * <p>
 * Il enregistre chaque requête préparée et les paramètres liés, renvoie des
 * lignes programmées à l'avance, simule les clés générées, et vérifie que
 * chaque {@code PreparedStatement} / {@code ResultSet} est bien fermé.
 */
public class FauxJdbc implements ConnectionProvider {

    /** Une exécution : la requête SQL et ses paramètres (index 1..n). */
    public static final class Execution {
        public final String sql;
        public final Map<Integer, Object> parametres;

        Execution(String sql, Map<Integer, Object> parametres) {
            this.sql = sql;
            this.parametres = parametres;
        }

        public Object param(int i) {
            return parametres.get(i);
        }

        @Override
        public String toString() {
            return sql + " " + parametres;
        }
    }

    public final List<Execution> executions = new ArrayList<>();
    public final List<String> preparees = new ArrayList<>();
    public int statementsOuverts;
    public int resultSetsOuverts;
    public int commits;
    public int rollbacks;
    public boolean autoCommit = true;
    public boolean connexionFermee;

    private final Map<String, List<Map<String, Object>>> resultats = new LinkedHashMap<>();
    private String sqlEnEchec;
    private int codeErreur;
    private int prochaineCle = 1000;

    /** Programme les lignes renvoyées par toute requête contenant {@code fragmentSql}. */
    @SafeVarargs
    public final void quand(String fragmentSql, Map<String, Object>... lignes) {
        resultats.put(fragmentSql, Arrays.asList(lignes));
    }

    /** Toute requête contenant ce fragment lèvera une SQLException. */
    public void echouerSur(String fragmentSql) {
        echouerSur(fragmentSql, 0);
    }

    /** Idem, avec le code d'erreur du SGBD (ex. 1062 : doublon MySQL). */
    public void echouerSur(String fragmentSql, int codeErreur) {
        this.sqlEnEchec = fragmentSql;
        this.codeErreur = codeErreur;
    }

    /** Construit une ligne : ligne("col1", v1, "col2", v2...). */
    public static Map<String, Object> ligne(Object... cv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < cv.length; i += 2) {
            m.put(((String) cv[i]).toLowerCase(), cv[i + 1]);
        }
        return m;
    }

    public Execution derniere() {
        return executions.get(executions.size() - 1);
    }

    @Override
    public Connection getConnection() {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{Connection.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
                        switch (m.getName()) {
                            case "prepareStatement":
                                return preparer((String) args[0]);
                            case "setAutoCommit":
                                autoCommit = (Boolean) args[0];
                                return null;
                            case "getAutoCommit":
                                return autoCommit;
                            case "commit":
                                commits++;
                                return null;
                            case "rollback":
                                rollbacks++;
                                return null;
                            case "close":
                                connexionFermee = true;
                                return null;
                            case "isClosed":
                                return connexionFermee;
                            case "isValid":
                                return true;
                            default:
                                return defaut(m);
                        }
                    }
                });
    }

    private PreparedStatement preparer(final String sql) throws SQLException {
        if (sqlEnEchec != null && sql.contains(sqlEnEchec)) {
            throw new SQLException("Erreur simulée", "23000", codeErreur);
        }
        preparees.add(sql);
        statementsOuverts++;
        final Map<Integer, Object> params = new HashMap<>();
        final int[] cle = new int[1];
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
                        String n = m.getName();
                        if (n.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) {
                            params.put((Integer) args[0], n.equals("setNull") ? null : args[1]);
                            return null;
                        }
                        switch (n) {
                            case "executeQuery":
                                executions.add(new Execution(sql, new HashMap<>(params)));
                                return resultSet(lignesPour(sql));
                            case "executeUpdate":
                                executions.add(new Execution(sql, new HashMap<>(params)));
                                cle[0] = ++prochaineCle;
                                return 1;
                            case "addBatch":
                                executions.add(new Execution(sql, new HashMap<>(params)));
                                return null;
                            case "executeBatch":
                                return new int[0];
                            case "getGeneratedKeys":
                                return resultSet(Collections.singletonList(ligne("GENERATED_KEY", cle[0])));
                            case "close":
                                statementsOuverts--;
                                return null;
                            default:
                                return defaut(m);
                        }
                    }
                });
    }

    private List<Map<String, Object>> lignesPour(String sql) {
        for (Map.Entry<String, List<Map<String, Object>>> e : resultats.entrySet()) {
            if (sql.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return Collections.emptyList();
    }

    private ResultSet resultSet(final List<Map<String, Object>> lignes) {
        resultSetsOuverts++;
        final int[] position = {-1};
        final Object[] derniereValeur = {null};
        return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ResultSet.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
                        String n = m.getName();
                        switch (n) {
                            case "next":
                                position[0]++;
                                return position[0] < lignes.size();
                            case "close":
                                resultSetsOuverts--;
                                return null;
                            case "wasNull":
                                return derniereValeur[0] == null;
                            case "getMetaData":
                                return metaData(lignes.isEmpty() ? new ArrayList<String>()
                                        : new ArrayList<>(lignes.get(0).keySet()));
                            default:
                                break;
                        }
                        if (n.startsWith("get") && args != null && args.length == 1) {
                            Map<String, Object> l = lignes.get(position[0]);
                            Object v = args[0] instanceof Integer
                                    ? new ArrayList<>(l.values()).get((Integer) args[0] - 1)
                                    : l.get(((String) args[0]).toLowerCase());
                            derniereValeur[0] = v;
                            return convertir(v, m.getReturnType());
                        }
                        return defaut(m);
                    }
                });
    }

    private static Object convertir(Object v, Class<?> type) {
        if (type == int.class) {
            return v == null ? 0 : ((Number) v).intValue();
        }
        if (type == double.class) {
            return v == null ? 0.0 : ((Number) v).doubleValue();
        }
        if (type == byte.class) {
            return v == null ? (byte) 0 : ((Number) v).byteValue();
        }
        if (type == boolean.class) {
            return v != null && (v instanceof Boolean ? (Boolean) v : ((Number) v).intValue() != 0);
        }
        return v;
    }

    private ResultSetMetaData metaData(final List<String> colonnes) {
        return (ResultSetMetaData) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        if (m.getName().equals("getColumnCount")) {
                            return colonnes.size();
                        }
                        if (m.getName().equals("getColumnLabel") || m.getName().equals("getColumnName")) {
                            return colonnes.get((Integer) args[0] - 1);
                        }
                        return defaut(m);
                    }
                });
    }

    private static Object defaut(Method m) {
        Class<?> t = m.getReturnType();
        if (t == boolean.class) {
            return false;
        }
        if (t == int.class) {
            return 0;
        }
        if (t == long.class) {
            return 0L;
        }
        if (t == short.class) {
            return (short) 0;
        }
        if (t == byte.class) {
            return (byte) 0;
        }
        return null;
    }
}
