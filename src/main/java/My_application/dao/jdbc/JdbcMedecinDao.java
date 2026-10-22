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
}
