package My_application.dao.jdbc;

import My_application.dao.JournalDao;
import My_application.entities.EntreeJournal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class JdbcJournalDao implements JournalDao {

    private static final RowMapper<EntreeJournal> ENTREE = new RowMapper<EntreeJournal>() {
        @Override
        public EntreeJournal map(ResultSet rs) throws SQLException {
            return new EntreeJournal(rs.getTimestamp("dateHeure").toLocalDateTime(),
                    (Integer) rs.getObject("idUtilisateur"), rs.getString("action"),
                    (Integer) rs.getObject("idPatient"), rs.getString("detail"), rs.getString("poste"));
        }
    };

    private final JdbcHelper jdbc;

    public JdbcJournalDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void ajouter(EntreeJournal e) {
        jdbc.update("INSERT INTO journal_acces (dateHeure, idUtilisateur, action, idPatient, detail, poste) "
                + "VALUES (?,?,?,?,?,?)", Timestamp.valueOf(e.getDateHeure()), e.getIdUtilisateur(),
                e.getAction(), e.getIdPatient(), e.getDetail(), e.getPoste());
    }

    @Override
    public List<EntreeJournal> findByPatient(int idPatient) {
        return jdbc.query("SELECT dateHeure, idUtilisateur, action, idPatient, detail, poste FROM journal_acces "
                + "WHERE idPatient=? ORDER BY dateHeure DESC, idJournal DESC", ENTREE, idPatient);
    }
}
