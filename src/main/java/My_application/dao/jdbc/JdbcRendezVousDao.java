package My_application.dao.jdbc;

import My_application.dao.RendezVousDao;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JdbcRendezVousDao implements RendezVousDao {

    /** Colonnes lues avec le patient et l'utilisateur qui a pris le rendez-vous. */
    private static final String SELECT = "SELECT r.idRendezVous, r.idUser, r.idPatient, r.idMedecin, r.date, "
            + "r.heure, r.motif, r.prisLe, r.etat, "
            + "p.nomPatient, p.prenomPatient, p.date_Nais_Patient, p.sexePatient, p.telPatient, "
            + "u.nom, u.prenom, u.qualite "
            + "FROM rendezvous r JOIN patient p ON p.idPatient = r.idPatient "
            + "LEFT JOIN utilisateur u ON u.idUtilisateur = r.idUser ";

    private final JdbcHelper jdbc;

    public JdbcRendezVousDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Rendez_Vous> MAPPER = new RowMapper<Rendez_Vous>() {
        @Override
        public Rendez_Vous map(ResultSet rs) throws SQLException {
            Rendez_Vous r = new Rendez_Vous();
            r.setIdRendezVous((Integer) rs.getObject("idRendezVous"));
            r.setPatient(Mappers.PATIENT.map(rs));
            User u = new User(rs.getInt("idUser"), rs.getString("nom"), rs.getString("prenom"),
                    rs.getString("qualite"));
            r.setUtilisateur(u);
            r.setDate(Mappers.jour(rs, "date"));
            r.setHeure(Mappers.heure(rs, "heure"));
            r.setMotif(rs.getString("motif"));
            Timestamp prisLe = rs.getTimestamp("prisLe");
            if (prisLe != null) {
                r.setDateFix(prisLe.toLocalDateTime().toLocalDate());
                r.setHeureFix(prisLe.toLocalDateTime().toLocalTime());
            }
            r.setIdMedecin((Integer) rs.getObject("idMedecin"));
            r.setEtat(rs.getByte("etat"));
            return r;
        }
    };

    @Override
    public void insert(Rendez_Vous rdv) {
        // prisLe (migration V8) : date et heure de la prise ; sans elles, l'heure du serveur.
        LocalDateTime prisLe = rdv.getDateFix() == null || rdv.getHeureFix() == null ? null
                : LocalDateTime.of(rdv.getDateFix(), rdv.getHeureFix());
        int id = jdbc.insertAndGetKey("INSERT INTO rendezvous (idUser, idPatient, idMedecin, date, heure, motif, "
                + "prisLe, etat) VALUES (?,?,?,?,?,?,COALESCE(?, CURRENT_TIMESTAMP),?)",
                rdv.getUtilisateur().getIdent(), rdv.getPatient().getIdentPat(), rdv.getIdMedecin(), rdv.getDate(),
                rdv.getHeure(), rdv.getMotif(), prisLe, rdv.getEtat());
        rdv.setIdRendezVous(id);
    }

    @Override
    public boolean changerEtat(int idRendezVous, int etatAttendu, int nouvelEtat) {
        return jdbc.update("UPDATE rendezvous SET etat=? WHERE idRendezVous=? AND etat=?",
                nouvelEtat, idRendezVous, etatAttendu) > 0;
    }

    @Override
    public boolean deplacer(int idRendezVous, LocalDate jour, LocalTime heure, int idMedecin) {
        return jdbc.update("UPDATE rendezvous SET date=?, heure=?, idMedecin=? WHERE idRendezVous=? AND etat=?",
                jour, heure, idMedecin, idRendezVous, Rendez_Vous.ETAT_PREVU) > 0;
    }

    @Override
    public List<Rendez_Vous> findEntre(LocalDate debut, LocalDate fin) {
        // Une requête avec jointures au lieu de 1 + 2×N requêtes auparavant.
        return jdbc.query(SELECT + "WHERE r.date BETWEEN ? AND ? ORDER BY r.date, r.heure, r.idMedecin",
                MAPPER, debut, fin);
    }

    @Override
    public List<Rendez_Vous> findDesPatients(Collection<Integer> idPatients) {
        if (idPatients.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder marques = new StringBuilder();
        List<Object> params = new ArrayList<>();
        for (Integer id : idPatients) {
            marques.append(marques.length() == 0 ? "?" : ",?");
            params.add(id);
        }
        return jdbc.query(SELECT + "WHERE r.idPatient IN (" + marques + ") ORDER BY r.date DESC, r.heure DESC",
                MAPPER, params.toArray());
    }

    @Override
    public int compterPrevus(int idMedecin, LocalDate jour) {
        Long n = jdbc.queryOne("SELECT COUNT(*) AS n FROM rendezvous WHERE idMedecin=? AND etat=? AND date >= ?",
                rs -> rs.getLong("n"), idMedecin, Rendez_Vous.ETAT_PREVU, jour);
        return n == null ? 0 : n.intValue();
    }
}
