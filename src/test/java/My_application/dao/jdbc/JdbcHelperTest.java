package My_application.dao.jdbc;

import static My_application.fake.FauxJdbc.ligne;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.dao.DataAccessException;
import My_application.dao.DoublonException;
import My_application.fake.FauxJdbc;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class JdbcHelperTest {

    private final FauxJdbc base = new FauxJdbc();
    private final JdbcHelper jdbc = new JdbcHelper(base);

    private static final RowMapper<String> NOM = new RowMapper<String>() {
        @Override
        public String map(ResultSet rs) throws SQLException {
            return rs.getString("nom");
        }
    };

    @Test
    public void lesValeursSontPasseesEnParametresEtNonConcatenees() {
        jdbc.query("SELECT nom FROM t WHERE nom LIKE ?", NOM, "maux d'estomac%");
        FauxJdbc.Execution e = base.derniere();
        assertEquals("SELECT nom FROM t WHERE nom LIKE ?", e.sql);
        assertEquals("maux d'estomac%", e.param(1));
    }

    @Test
    public void statementsEtResultSetsSontToujoursFermes() {
        base.quand("FROM t", ligne("nom", "a"), ligne("nom", "b"));
        List<String> r = jdbc.query("SELECT nom FROM t", NOM);
        jdbc.update("DELETE FROM t WHERE id=?", 3);
        jdbc.insertAndGetKey("INSERT INTO t (nom) VALUES (?)", "c");
        assertEquals(Arrays.asList("a", "b"), r);
        assertEquals(0, base.statementsOuverts);
        assertEquals(0, base.resultSetsOuverts);
    }

    @Test
    public void queryOneRenvoieNullSiAucuneLigne() {
        assertNull(jdbc.queryOne("SELECT nom FROM vide", NOM));
    }

    @Test
    public void laCleGenereeEstRenvoyee() {
        int id = jdbc.insertAndGetKey("INSERT INTO t (nom) VALUES (?)", "x");
        assertTrue(id > 0);
    }

    @Test
    public void lesErreursSqlDeviennentDesDataAccessException() {
        base.echouerSur("FROM t");
        try {
            jdbc.query("SELECT nom FROM t", NOM);
            fail();
        } catch (DataAccessException e) {
            assertTrue(e.getCause() instanceof SQLException);
        }
    }

    @Test
    public void uneCleDejaPriseDevientUneDoublonException() {
        base.echouerSur("INSERT INTO rendezvous", JdbcHelper.MYSQL_DOUBLON);
        try {
            jdbc.update("INSERT INTO rendezvous (idUser) VALUES (?)", 1);
            fail();
        } catch (DoublonException e) {
            assertEquals(JdbcHelper.MYSQL_DOUBLON, ((SQLException) e.getCause()).getErrorCode());
        }
    }

    @Test
    public void uneAutreErreurSqlNEstPasUnDoublon() {
        base.echouerSur("FROM t", 1452); // clé étrangère absente
        try {
            jdbc.query("SELECT nom FROM t", NOM);
            fail();
        } catch (DataAccessException e) {
            assertFalse(e instanceof DoublonException);
        }
    }

    @Test
    public void lesJokersDeLikeSontEchappes() {
        assertEquals("50\\%%", JdbcHelper.commencePar("50%"));
        assertEquals("a\\_b%", JdbcHelper.commencePar("a_b"));
        assertEquals("%", JdbcHelper.commencePar(null));
    }

    @Test
    public void unBatchVideNeFaitRien() {
        jdbc.batch("INSERT INTO t VALUES (?)", java.util.Collections.<Object[]>emptyList());
        assertFalse(base.preparees.contains("INSERT INTO t VALUES (?)"));
    }
}
