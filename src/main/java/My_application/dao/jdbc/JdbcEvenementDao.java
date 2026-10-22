package My_application.dao.jdbc;

import My_application.dao.EvenementDao;
import java.time.LocalDateTime;

public class JdbcEvenementDao implements EvenementDao {

    private final JdbcHelper jdbc;

    public JdbcEvenementDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void publier(String type, Integer idObjet) {
        jdbc.update("INSERT INTO evenement (type, idObjet) VALUES (?,?)", type, idObjet);
    }

    @Override
    public long dernier() {
        Long n = jdbc.queryOne("SELECT COALESCE(MAX(idEvenement), 0) AS dernier FROM evenement",
                rs -> rs.getLong("dernier"));
        return n == null ? 0 : n;
    }

    @Override
    public int purgerAvant(LocalDateTime limite) {
        return jdbc.update("DELETE FROM evenement WHERE creeLe < ?", limite);
    }
}
