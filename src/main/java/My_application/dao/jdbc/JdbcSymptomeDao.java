package My_application.dao.jdbc;

import My_application.dao.SymptomeDao;
import My_application.entities.symptome;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcSymptomeDao implements SymptomeDao {

    static final RowMapper<symptome> MAPPER = new RowMapper<symptome>() {
        @Override
        public symptome map(ResultSet rs) throws SQLException {
            return new symptome(rs.getInt("idsymptome"), rs.getString("designation"));
        }
    };

    private final JdbcHelper jdbc;

    public JdbcSymptomeDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<symptome> findByDesignationCommencantPar(String prefixe) {
        return jdbc.query("SELECT idsymptome, designation FROM symptomes WHERE designation LIKE ? "
                + "ORDER BY designation", MAPPER, JdbcHelper.commencePar(prefixe));
    }

    @Override
    public symptome findByDesignation(String designation) {
        return jdbc.queryOne("SELECT idsymptome, designation FROM symptomes WHERE designation=?",
                MAPPER, designation);
    }

    @Override
    public symptome insert(String designation) {
        int id = jdbc.insertAndGetKey("INSERT INTO symptomes (designation) VALUES (?)", designation);
        return new symptome(id, designation);
    }
}
