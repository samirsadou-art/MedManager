package My_application.dao.jdbc;

import My_application.dao.ConsultationDao;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.pathologie;
import My_application.entities.patient;
import My_application.entities.symptome;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdbcConsultationDao implements ConsultationDao {

    private final JdbcHelper jdbc;

    public JdbcConsultationDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    /** En-tête : le médecin n'est renseigné que par son identifiant. */
    private static final RowMapper<consultation> ENTETE = new RowMapper<consultation>() {
        @Override
        public consultation map(ResultSet rs) throws SQLException {
            consultation c = new consultation(null, null);
            c.setIdConsul(rs.getInt("idConsultation"));
            c.setDate_cons(rs.getDate("dateConsultation"));
            c.setHeure_cons(rs.getTime("heureConsultation"));
            c.setTemperature(rs.getDouble("temperature"));
            c.setPul_Cardiaque(rs.getInt("pulsation"));
            c.setPressionA(rs.getString("pression"));
            c.setTauxRespir(rs.getInt("respiration"));
            c.setPoids(rs.getDouble("poids"));
            medecin m = new medecin();
            m.setIdmedecin(rs.getInt("idMedecin"));
            c.setMedTraitant(m);
            patient p = new patient();
            p.setIdentPat(rs.getInt("idPatient"));
            c.setMalade(p);
            return c;
        }
    };

    @Override
    public consultation insert(consultation c) {
        int id = jdbc.insertAndGetKey("INSERT INTO consultation (dateConsultation, heureConsultation, idMedecin, "
                + "idPatient, temperature, pulsation, pression, respiration, poids) VALUES (?,?,?,?,?,?,?,?,?)",
                c.getDate_cons(), c.getHeure_cons(), c.getMedecin().getIdmedecin(), c.getPatient().getIdentPat(),
                c.getTemperature(), c.getPul_Cardiaque(), c.getPressionA(), c.getTauxRespir(), c.getPoids());
        c.setIdConsul(id);
        return c;
    }

    @Override
    public List<consultation> findByPatient(int idPatient) {
        return jdbc.query("SELECT * FROM consultation WHERE idPatient=? "
                + "ORDER BY dateConsultation, heureConsultation", ENTETE, idPatient);
    }

    @Override
    public List<consultation> findByDate(Date date) {
        // Une seule requête avec jointure au lieu d'une requête patient par ligne.
        return jdbc.query("SELECT c.*, p.nomPatient, p.prenomPatient, p.date_Nais_Patient, p.sexePatient "
                + "FROM consultation c JOIN patient p ON p.idPatient = c.idPatient "
                + "WHERE c.dateConsultation=? ORDER BY c.heureConsultation",
                new RowMapper<consultation>() {
                    @Override
                    public consultation map(ResultSet rs) throws SQLException {
                        consultation c = ENTETE.map(rs);
                        c.setMalade(Mappers.PATIENT.map(rs));
                        return c;
                    }
                }, date);
    }

    @Override
    public void ajouterSymptomes(int idConsultation, List<symptome> symptomes) {
        List<Object[]> lignes = new ArrayList<>();
        for (symptome s : symptomes) {
            lignes.add(new Object[]{idConsultation, s.getId()});
        }
        jdbc.batch("INSERT INTO consultsymptome (idconsultation, idsymptome) VALUES (?,?)", lignes);
    }

    @Override
    public void ajouterPathologies(int idConsultation, List<pathologie> pathologies) {
        List<Object[]> lignes = new ArrayList<>();
        for (pathologie p : pathologies) {
            lignes.add(new Object[]{idConsultation, p.getId()});
        }
        jdbc.batch("INSERT INTO consultpathologie (idconsultation, idpathologie) VALUES (?,?)", lignes);
    }

    @Override
    public List<symptome> findSymptomes(int idConsultation) {
        return jdbc.query("SELECT s.idsymptome, s.designation FROM symptomes s "
                + "JOIN consultsymptome cs ON cs.idsymptome = s.idsymptome WHERE cs.idconsultation=?",
                JdbcSymptomeDao.MAPPER, idConsultation);
    }

    @Override
    public List<pathologie> findPathologies(int idConsultation) {
        return jdbc.query("SELECT p.idpathologie, p.designation FROM pathologies p "
                + "JOIN consultpathologie cp ON cp.idpathologie = p.idpathologie WHERE cp.idconsultation=?",
                JdbcPathologieDao.MAPPER, idConsultation);
    }

    @Override
    public Map<Integer, List<symptome>> findSymptomesParPatient(int idPatient) {
        return jdbc.queryParCle("SELECT cs.idconsultation, s.idsymptome, s.designation FROM consultsymptome cs "
                + "JOIN symptomes s ON s.idsymptome = cs.idsymptome "
                + "JOIN consultation c ON c.idConsultation = cs.idconsultation WHERE c.idPatient=?",
                "idconsultation", JdbcSymptomeDao.MAPPER, idPatient);
    }

    @Override
    public Map<Integer, List<pathologie>> findPathologiesParPatient(int idPatient) {
        return jdbc.queryParCle("SELECT cp.idconsultation, p.idpathologie, p.designation FROM consultpathologie cp "
                + "JOIN pathologies p ON p.idpathologie = cp.idpathologie "
                + "JOIN consultation c ON c.idConsultation = cp.idconsultation WHERE c.idPatient=?",
                "idconsultation", JdbcPathologieDao.MAPPER, idPatient);
    }
}
