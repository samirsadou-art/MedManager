package My_application.dao.jdbc;

import My_application.dao.SalleAttenteDao;
import My_application.entities.Passage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class JdbcSalleAttenteDao implements SalleAttenteDao {

    private final JdbcHelper jdbc;

    public JdbcSalleAttenteDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    private static LocalDateTime moment(ResultSet rs, String colonne) throws SQLException {
        Timestamp t = rs.getTimestamp(colonne);
        return t == null ? null : t.toLocalDateTime();
    }

    private static final RowMapper<Passage> MAPPER = new RowMapper<Passage>() {
        @Override
        public Passage map(ResultSet rs) throws SQLException {
            Passage p = new Passage();
            p.setIdPassage(rs.getInt("idPassage"));
            p.setPatient(Mappers.PATIENT.map(rs));
            p.setIdMedecin(rs.getInt("idMedecin"));
            p.setIdRendezVous((Integer) rs.getObject("idRendezVous"));
            p.setHeurePrevue(Mappers.heure(rs, "heure"));
            p.setJour(Mappers.jour(rs, "jour"));
            p.setArriveeLe(moment(rs, "arriveeLe"));
            p.setAppeleLe(moment(rs, "appeleLe"));
            p.setSortieLe(moment(rs, "sortieLe"));
            p.setUrgence(rs.getInt("priorite") > 0);
            p.setEtat(rs.getInt("etat"));
            return p;
        }
    };

    @Override
    public void insert(Passage p) {
        int id = jdbc.insertAndGetKey("INSERT INTO salle_attente (idPatient, idMedecin, idRendezVous, jour, "
                + "arriveeLe, priorite, etat) VALUES (?,?,?,?,?,?,?)",
                p.getPatient().getIdentPat(), p.getIdMedecin(), p.getIdRendezVous(), p.getJour(), p.getArriveeLe(),
                p.isUrgence() ? 1 : 0, p.getEtat());
        p.setIdPassage(id);
    }

    /** Fiche complète du patient : le médecin ouvre son dossier depuis la salle d'attente. */
    @Override
    public List<Passage> findDuJour(LocalDate jour) {
        return jdbc.query("SELECT s.idPassage, s.idPatient, s.idMedecin, s.idRendezVous, s.jour, s.arriveeLe, "
                + "s.appeleLe, s.sortieLe, s.priorite, s.etat, r.heure, p.* "
                + "FROM salle_attente s JOIN patient p ON p.idPatient = s.idPatient "
                + "LEFT JOIN rendezvous r ON r.idRendezVous = s.idRendezVous "
                + "WHERE s.jour = ? ORDER BY s.arriveeLe", MAPPER, jour);
    }

    @Override
    public boolean mettreAJour(Passage p, int etatAttendu) {
        return jdbc.update("UPDATE salle_attente SET etat=?, appeleLe=?, sortieLe=?, priorite=? "
                + "WHERE idPassage=? AND etat=?",
                p.getEtat(), p.getAppeleLe(), p.getSortieLe(), p.isUrgence() ? 1 : 0, p.getIdPassage(),
                etatAttendu) > 0;
    }
}
