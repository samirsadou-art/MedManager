package My_application.dao.jdbc;

import My_application.dao.RendezVousDao;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            r.setUtilisateur(u);
            r.setDate(Mappers.jour(rs, "date"));
            r.setHeure(Mappers.heure(rs, "heure"));
            Timestamp prisLe = rs.getTimestamp("prisLe");
            if (prisLe != null) {
                r.setDateFix(prisLe.toLocalDateTime().toLocalDate());
                r.setHeureFix(prisLe.toLocalDateTime().toLocalTime());
            }
            r.setEtat(rs.getByte("etat"));
            return r;
        }
    };

    @Override
    public void insert(Rendez_Vous rdv) {
        // prisLe (migration V8) : date et heure de la prise ; sans elles, l'heure du serveur.
        LocalDateTime prisLe = rdv.getDateFix() == null || rdv.getHeureFix() == null ? null
                : LocalDateTime.of(rdv.getDateFix(), rdv.getHeureFix());
        jdbc.update("INSERT INTO rendezvous (idUser, idPatient, date, heure, prisLe) "
                + "VALUES (?,?,?,?,COALESCE(?, CURRENT_TIMESTAMP))",
                rdv.getUtilisateur().getIdent(), rdv.getPatient().getIdentPat(), rdv.getDate(), rdv.getHeure(),
                prisLe);
    }

    @Override
    public boolean delete(Rendez_Vous rdv) {
        return jdbc.update("DELETE FROM rendezvous WHERE idPatient=? AND date=? AND heure=?",
                rdv.getPatient().getIdentPat(), rdv.getDate(), rdv.getHeure()) > 0;
    }

    @Override
    public List<Rendez_Vous> findEntre(LocalDate debut, LocalDate fin) {
        // Une requête avec jointures au lieu de 1 + 2×N requêtes auparavant.
        return jdbc.query("SELECT r.idUser, r.idPatient, r.date, r.heure, r.prisLe, r.etat, "
                + "p.nomPatient, p.prenomPatient, p.date_Nais_Patient, p.sexePatient, "
                + "u.nom, u.prenom, u.qualite "
                + "FROM rendezvous r JOIN patient p ON p.idPatient = r.idPatient "
                + "LEFT JOIN utilisateur u ON u.idUtilisateur = r.idUser "
                + "WHERE r.date BETWEEN ? AND ? ORDER BY r.date, r.heure", MAPPER, debut, fin);
    }
}
