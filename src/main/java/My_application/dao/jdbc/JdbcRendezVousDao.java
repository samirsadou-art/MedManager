package My_application.dao.jdbc;

import My_application.dao.RendezVousDao;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class JdbcRendezVousDao implements RendezVousDao {

    private final JdbcHelper jdbc;

    public JdbcRendezVousDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Rendez_Vous> MAPPER = new RowMapper<Rendez_Vous>() {
        @Override
        public Rendez_Vous map(ResultSet rs) throws SQLException {
            Rendez_Vous r = new Rendez_Vous();
            r.setPatient(Mappers.PATIENT.map(rs));
            User u = new User(rs.getInt("idUser"), rs.getString("nom"), rs.getString("prenom"),
                    rs.getString("qualite"));
            r.setUtilsateur(u);
            r.setDate(Mappers.jour(rs, "date"));
            r.setHeure(Mappers.heure(rs, "heure"));
            r.setDateFix(Mappers.jour(rs, "dateFix"));
            r.setHeureFix(Mappers.heure(rs, "heureFix"));
            r.setEtat(rs.getByte("etat"));
            return r;
        }
    };

    @Override
    public void insert(Rendez_Vous rdv) {
        jdbc.update("INSERT INTO rondezvous (idUser, idPatient, date, heure, dateFix, heureFix) VALUES (?,?,?,?,?,?)",
                rdv.getUtilsateur().getIdent(), rdv.getPatient().getIdentPat(), rdv.getDate(), rdv.getHeure(),
                rdv.getDateFix(), rdv.getHeureFix());
    }

    @Override
    public boolean delete(Rendez_Vous rdv) {
        return jdbc.update("DELETE FROM rondezvous WHERE idPatient=? AND date=? AND heure=?",
                rdv.getPatient().getIdentPat(), rdv.getDate(), rdv.getHeure()) > 0;
    }

    @Override
    public List<Rendez_Vous> findEntre(LocalDate debut, LocalDate fin) {
        // Une requête avec jointures au lieu de 1 + 2×N requêtes auparavant.
        return jdbc.query("SELECT r.idUser, r.idPatient, r.date, r.heure, r.dateFix, r.heureFix, r.etat, "
                + "p.nomPatient, p.prenomPatient, p.date_Nais_Patient, p.sexePatient, "
                + "u.nom, u.prenom, u.qualite "
                + "FROM rondezvous r JOIN patient p ON p.idPatient = r.idPatient "
                + "LEFT JOIN utilisateur u ON u.idUtilisateur = r.idUser "
                + "WHERE r.date BETWEEN ? AND ? ORDER BY r.date, r.heure", MAPPER, debut, fin);
    }
}
