package My_application.dao.jdbc;

import My_application.dao.PrescriptionDao;
import My_application.entities.medicament;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdbcPrescriptionDao implements PrescriptionDao {

    private final JdbcHelper jdbc;

    public JdbcPrescriptionDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertAll(int idConsultation, List<medicament> medicaments) {
        List<Object[]> lignes = new ArrayList<>();
        for (medicament m : medicaments) {
            lignes.add(new Object[]{m.getIdmed(), idConsultation, m.getUnite(), m.getPosologie(),
                m.getDuree(), m.getMode()});
        }
        jdbc.batch("INSERT INTO prescrire (idMed, idConsul, unite, posologie, duree, mode) "
                + "VALUES (?,?,?,?,?,?)", lignes);
    }

    private static final RowMapper<medicament> LIGNE = new RowMapper<medicament>() {
        @Override
        public medicament map(ResultSet rs) throws SQLException {
            medicament m = new medicament();
            m.setIdmed(rs.getInt("idMedicament"));
            m.setDesignmed(rs.getString("nomMedicament"));
            m.setUnite(rs.getInt("unite"));
            m.setPosologie(rs.getInt("posologie"));
            m.setDuree(rs.getInt("duree"));
            m.setMode(rs.getInt("mode"));
            return m;
        }
    };

    @Override
    public List<medicament> findByConsultation(int idConsultation) {
        return jdbc.query("SELECT m.idMedicament, m.nomMedicament, p.unite, p.posologie, p.duree, p.mode "
                + "FROM prescrire p JOIN medicaments m ON m.idMedicament = p.idMed WHERE p.idConsul=?",
                LIGNE, idConsultation);
    }

    @Override
    public Map<Integer, List<medicament>> findByPatient(int idPatient) {
        return jdbc.queryParCle("SELECT p.idConsul, m.idMedicament, m.nomMedicament, p.unite, p.posologie, p.duree, p.mode "
                + "FROM prescrire p JOIN medicaments m ON m.idMedicament = p.idMed "
                + "JOIN consultation c ON c.idConsultation = p.idConsul WHERE c.idPatient=?",
                "idConsul", LIGNE, idPatient);
    }
}
