package My_application.dao.jdbc;

import My_application.dao.PathologieDao;
import My_application.entities.pathologie;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcPathologieDao implements PathologieDao {

    static final RowMapper<pathologie> MAPPER = new RowMapper<pathologie>() {
        @Override
        public pathologie map(ResultSet rs) throws SQLException {
            return new pathologie(rs.getInt("idpathologie"), rs.getString("designation"));
        }
    };

    private final JdbcHelper jdbc;

    public JdbcPathologieDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<pathologie> findByDesignationCommencantPar(String prefixe) {
        return jdbc.query("SELECT idpathologie, designation FROM pathologies WHERE designation LIKE ? "
                + "ORDER BY designation", MAPPER, JdbcHelper.commencePar(prefixe));
    }

    @Override
    public pathologie findByDesignation(String designation) {
        return jdbc.queryOne("SELECT idpathologie, designation FROM pathologies WHERE designation=?",
                MAPPER, designation);
    }

    @Override
    public pathologie insert(String designation) {
        int id = jdbc.insertAndGetKey("INSERT INTO pathologies (designation) VALUES (?)", designation);
        return new pathologie(id, designation);
    }
}
