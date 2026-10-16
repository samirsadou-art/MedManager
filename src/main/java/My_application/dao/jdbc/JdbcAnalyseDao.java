package My_application.dao.jdbc;

import My_application.dao.AnalyseDao;
import My_application.entities.CategAnalyse;
import My_application.entities.analyse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcAnalyseDao implements AnalyseDao {

    private static final RowMapper<analyse> MAPPER = new RowMapper<analyse>() {
        @Override
        public analyse map(ResultSet rs) throws SQLException {
            analyse a = new analyse();
            a.setIdAnal(rs.getInt("idanalyse"));
            a.setDesign(rs.getString("designation"));
            a.setObserv("");
            return a;
        }
    };

    private final JdbcHelper jdbc;

    public JdbcAnalyseDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<analyse> findByDesignationCommencantPar(String prefixe) {
        return jdbc.query("SELECT idanalyse, designation FROM analyse WHERE designation LIKE ? "
                + "ORDER BY designation", MAPPER, JdbcHelper.commencePar(prefixe));
    }

    @Override
    public List<analyse> findByCategorie(int idCategorie) {
        return jdbc.query("SELECT idanalyse, designation FROM analyse WHERE idcategorie=? ORDER BY designation",
                MAPPER, idCategorie);
    }

    @Override
    public List<CategAnalyse> findCategories() {
        return jdbc.query("SELECT idcateganalyse, categorie FROM categanalyse ORDER BY categorie",
                new RowMapper<CategAnalyse>() {
                    @Override
                    public CategAnalyse map(ResultSet rs) throws SQLException {
                        return new CategAnalyse(rs.getInt("idcateganalyse"), rs.getString("categorie"));
                    }
                });
    }

    @Override
    public analyse insert(String designation, int idCategorie) {
        int id = jdbc.insertAndGetKey("INSERT INTO analyse (designation, idcategorie) VALUES (?,?)",
                designation, idCategorie);
        analyse a = new analyse();
        a.setIdAnal(id);
        a.setDesign(designation);
        return a;
    }
}
