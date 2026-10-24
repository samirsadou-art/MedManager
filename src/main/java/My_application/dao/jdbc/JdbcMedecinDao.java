package My_application.dao.jdbc;

import My_application.dao.MedecinDao;
import My_application.entities.medecin;
import java.util.List;

public class JdbcMedecinDao implements MedecinDao {

    private final JdbcHelper jdbc;

    public JdbcMedecinDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public medecin findById(int id) {
        return jdbc.queryOne("SELECT * FROM medecin WHERE idMedecin=?", Mappers.MEDECIN, id);
    }

    @Override
    public List<medecin> findAll() {
        return jdbc.query("SELECT * FROM medecin ORDER BY nomMedecin, prenomMedecin", Mappers.MEDECIN);
    }

    @Override
    public medecin insert(medecin m) {
        int id = jdbc.insertAndGetKey("INSERT INTO medecin (ordreMedecin, nomMedecin, prenomMedecin, titre, specialite, "
                + "adresse, telephone1, telephone2, email, nomCabinet, actif) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                m.getOrdmedcin(), m.getNom(), m.getPrenom(), m.getTitre(), m.getSpecialite(), m.getAdresse(),
                m.getTelephone1(), m.getTelephone2(), m.getEmail(), m.getNom_cabinet(), m.isActif());
        m.setIdmedecin(id);
        return m;
    }

    @Override
    public void modifier(medecin m) {
        jdbc.update("UPDATE medecin SET ordreMedecin=?, nomMedecin=?, prenomMedecin=?, titre=?, specialite=?, "
                + "adresse=?, telephone1=?, telephone2=?, email=?, nomCabinet=? WHERE idMedecin=?",
                m.getOrdmedcin(), m.getNom(), m.getPrenom(), m.getTitre(), m.getSpecialite(), m.getAdresse(),
                m.getTelephone1(), m.getTelephone2(), m.getEmail(), m.getNom_cabinet(), m.getIdmedecin());
    }

    @Override
    public void definirActif(int idMedecin, boolean actif) {
        jdbc.update("UPDATE medecin SET actif=? WHERE idMedecin=?", actif, idMedecin);
    }
}
