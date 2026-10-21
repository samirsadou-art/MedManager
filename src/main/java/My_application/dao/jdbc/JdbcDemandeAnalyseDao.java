package My_application.dao.jdbc;

import My_application.dao.DemandeAnalyseDao;
import My_application.entities.analyse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdbcDemandeAnalyseDao implements DemandeAnalyseDao {

    private final JdbcHelper jdbc;

    public JdbcDemandeAnalyseDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertAll(int idConsultation, List<analyse> analyses) {
        List<Object[]> lignes = new ArrayList<>();
        for (analyse a : analyses) {
            lignes.add(new Object[]{idConsultation, a.getIdAnal(), a.getObserv(), a.getResultat(), a.getNote()});
        }
        jdbc.batch("INSERT INTO demandanalyse (idconsultation, idAnalyse, observation, resultat, note) "
                + "VALUES (?,?,?,?,?)", lignes);
    }

    @Override
    public void updateResultats(int idConsultation, List<analyse> analyses) {
        List<Object[]> lignes = new ArrayList<>();
        for (analyse a : analyses) {
            lignes.add(new Object[]{a.getResultat(), a.getNote(), a.getIdAnal(), idConsultation});
        }
        jdbc.batch("UPDATE demandanalyse SET resultat=?, note=? WHERE idAnalyse=? AND idconsultation=?", lignes);
    }

    private static final RowMapper<analyse> LIGNE = new RowMapper<analyse>() {
        @Override
        public analyse map(ResultSet rs) throws SQLException {
            analyse a = new analyse();
            a.setIdAnal(rs.getInt("idanalyse"));
            a.setDesign(rs.getString("designation"));
            a.setObserv(rs.getString("observation"));
            a.setResultat(rs.getString("resultat"));
            a.setNote(rs.getInt("note"));
            return a;
        }
    };

    @Override
    public List<analyse> findByConsultation(int idConsultation) {
        return jdbc.query("SELECT a.idanalyse, a.designation, d.observation, d.resultat, d.note "
                + "FROM demandanalyse d JOIN analyse a ON a.idanalyse = d.idAnalyse WHERE d.idconsultation=?",
                LIGNE, idConsultation);
    }

    @Override
    public Map<Integer, List<analyse>> findByPatient(int idPatient) {
        return jdbc.queryParCle("SELECT d.idconsultation, a.idanalyse, a.designation, d.observation, d.resultat, d.note "
                + "FROM demandanalyse d JOIN analyse a ON a.idanalyse = d.idAnalyse "
                + "JOIN consultation c ON c.idConsultation = d.idconsultation WHERE c.idPatient=?",
                "idconsultation", LIGNE, idPatient);
    }
}
