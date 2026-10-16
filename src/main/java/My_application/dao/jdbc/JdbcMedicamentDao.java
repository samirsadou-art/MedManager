package My_application.dao.jdbc;

import My_application.dao.MedicamentDao;
import My_application.entities.medicament;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcMedicamentDao implements MedicamentDao {

    private static final RowMapper<medicament> MAPPER = new RowMapper<medicament>() {
        @Override
        public medicament map(ResultSet rs) throws SQLException {
            medicament m = new medicament();
            m.setIdmed(rs.getInt("idMedicament"));
            m.setDesignmed(rs.getString("nomMedicament"));
            return m;
        }
    };

    private final JdbcHelper jdbc;

    public JdbcMedicamentDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<medicament> findByNomCommencantPar(String prefixe) {
        return jdbc.query("SELECT idMedicament, nomMedicament FROM medicaments WHERE nomMedicament LIKE ? "
                + "ORDER BY nomMedicament", MAPPER, JdbcHelper.commencePar(prefixe));
    }

    @Override
    public medicament findByNom(String nom) {
        return jdbc.queryOne("SELECT idMedicament, nomMedicament FROM medicaments WHERE nomMedicament=?",
                MAPPER, nom);
    }

    @Override
    public List<String> findFormes() {
        return jdbc.query("SELECT DISTINCT forme FROM medicaments WHERE forme IS NOT NULL AND forme <> '' "
                + "ORDER BY forme", new RowMapper<String>() {
                    @Override
                    public String map(ResultSet rs) throws SQLException {
                        return rs.getString("forme");
                    }
                });
    }

    @Override
    public medicament insert(String nom, String forme) {
        // type 'P' : médicament ajouté par le praticien (valeur d'origine).
        int id = jdbc.insertAndGetKey("INSERT INTO medicaments (nomMedicament, forme, type) VALUES (?,?,'P')",
                nom, forme == null || forme.isEmpty() ? null : forme);
        medicament m = new medicament();
        m.setIdmed(id);
        m.setDesignmed(nom);
        return m;
    }
}
