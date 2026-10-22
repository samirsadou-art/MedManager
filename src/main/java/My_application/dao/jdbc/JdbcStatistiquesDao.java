package My_application.dao.jdbc;

import My_application.dao.StatistiquesDao;
import My_application.entities.Passage;
import My_application.entities.Statistiques;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Comptages en SQL (GROUP BY) : seuls des nombres sortent de la base. */
public class JdbcStatistiquesDao implements StatistiquesDao {

    private final JdbcHelper jdbc;

    public JdbcStatistiquesDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<LocalDate, Integer> consultationsParJour(LocalDate debut, LocalDate fin, Integer idMedecin) {
        Map<LocalDate, Integer> r = new LinkedHashMap<>();
        for (Object[] l : compter("SELECT dateConsultation AS cle, COUNT(*) AS n FROM consultation "
                + "WHERE dateConsultation BETWEEN ? AND ?" + medecin(idMedecin)
                + " GROUP BY dateConsultation ORDER BY dateConsultation", debut, fin, idMedecin, true)) {
            r.put((LocalDate) l[0], (Integer) l[1]);
        }
        return r;
    }

    @Override
    public Map<Integer, Integer> consultationsParMedecin(LocalDate debut, LocalDate fin) {
        Map<Integer, Integer> r = new LinkedHashMap<>();
        for (Object[] l : compter("SELECT idMedecin AS cle, COUNT(*) AS n FROM consultation "
                + "WHERE dateConsultation BETWEEN ? AND ? GROUP BY idMedecin", debut, fin, null, false)) {
            r.put((Integer) l[0], (Integer) l[1]);
        }
        return r;
    }

    @Override
    public int patientsDistincts(LocalDate debut, LocalDate fin, Integer idMedecin) {
        return nombre("SELECT COUNT(DISTINCT idPatient) AS n FROM consultation WHERE dateConsultation BETWEEN ? AND ?"
                + medecin(idMedecin), parametres(debut, fin, idMedecin));
    }

    @Override
    public int nouveauxPatients(LocalDate debut, LocalDate fin, Integer idMedecin) {
        // Première consultation de chaque patient (tous médecins confondus), puis filtre sur la période.
        return nombre("SELECT COUNT(DISTINCT c.idPatient) AS n FROM consultation c "
                + "JOIN (SELECT idPatient, MIN(dateConsultation) AS premiere FROM consultation GROUP BY idPatient) p "
                + "ON p.idPatient = c.idPatient AND p.premiere = c.dateConsultation "
                + "WHERE c.dateConsultation BETWEEN ? AND ?" + (idMedecin == null ? "" : " AND c.idMedecin=?"),
                parametres(debut, fin, idMedecin));
    }

    @Override
    public Map<Integer, Integer> rendezVousParEtat(LocalDate debut, LocalDate fin, Integer idMedecin) {
        Map<Integer, Integer> r = new LinkedHashMap<>();
        for (Object[] l : compter("SELECT etat AS cle, COUNT(*) AS n FROM rendezvous WHERE date BETWEEN ? AND ?"
                + medecin(idMedecin) + " GROUP BY etat", debut, fin, idMedecin, false)) {
            r.put((Integer) l[0], (Integer) l[1]);
        }
        return r;
    }

    @Override
    public Statistiques.SalleAttente salleAttente(LocalDate debut, LocalDate fin, Integer idMedecin) {
        List<Object> p = new ArrayList<>(Arrays.asList(Passage.PARTI));
        p.addAll(Arrays.asList(parametres(debut, fin, idMedecin)));
        Statistiques.SalleAttente s = jdbc.queryOne("SELECT COUNT(*) AS passages, "
                + "COALESCE(SUM(idRendezVous IS NULL), 0) AS sansRdv, COALESCE(SUM(priorite > 0), 0) AS urgences, "
                + "COALESCE(SUM(etat = ?), 0) AS partis, "
                + "AVG(CASE WHEN appeleLe IS NOT NULL THEN TIMESTAMPDIFF(MINUTE, arriveeLe, appeleLe) END) AS attente "
                + "FROM salle_attente WHERE jour BETWEEN ? AND ?" + medecin(idMedecin),
                (ResultSet rs) -> {
                    Statistiques.SalleAttente x = new Statistiques.SalleAttente();
                    x.setPassages(rs.getInt("passages"));
                    x.setSansRendezVous(rs.getInt("sansRdv"));
                    x.setUrgences(rs.getInt("urgences"));
                    x.setPartis(rs.getInt("partis"));
                    double attente = rs.getDouble("attente");
                    x.setAttenteMoyenne(rs.wasNull() ? null : attente);
                    return x;
                }, p.toArray());
        return s == null ? new Statistiques.SalleAttente() : s;
    }

    private static String medecin(Integer idMedecin) {
        return idMedecin == null ? "" : " AND idMedecin=?";
    }

    private static Object[] parametres(LocalDate debut, LocalDate fin, Integer idMedecin) {
        return idMedecin == null ? new Object[] {debut, fin} : new Object[] {debut, fin, idMedecin};
    }

    private int nombre(String sql, Object... params) {
        Long n = jdbc.queryOne(sql, rs -> rs.getLong("n"), params);
        return n == null ? 0 : n.intValue();
    }

    /** Lignes (clé, nombre) ; la clé est un jour si {@code jour}, sinon un entier. */
    private List<Object[]> compter(String sql, LocalDate debut, LocalDate fin, Integer idMedecin, boolean jour) {
        return jdbc.query(sql, (ResultSet rs) -> new Object[] {
            jour ? Mappers.jour(rs, "cle") : Integer.valueOf(rs.getInt("cle")), Integer.valueOf(rs.getInt("n"))},
                parametres(debut, fin, idMedecin));
    }
}
