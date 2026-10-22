package My_application.dao.jdbc;

import My_application.dao.PresenceDao;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JdbcPresenceDao implements PresenceDao {

    private final JdbcHelper jdbc;

    public JdbcPresenceDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void signaler(int idUtilisateur, String poste) {
        jdbc.update("INSERT INTO utilisateur_presence (idUtilisateur, poste, vuLe) VALUES (?, ?, NOW()) "
                + "ON DUPLICATE KEY UPDATE poste = VALUES(poste), vuLe = NOW()", idUtilisateur, poste);
    }

    @Override
    public void quitter(int idUtilisateur) {
        jdbc.update("DELETE FROM utilisateur_presence WHERE idUtilisateur=?", idUtilisateur);
    }

    @Override
    public Set<Integer> enLigne(int secondes) {
        List<Integer> ids = jdbc.query("SELECT idUtilisateur FROM utilisateur_presence "
                + "WHERE vuLe >= NOW() - INTERVAL ? SECOND", rs -> rs.getInt("idUtilisateur"), secondes);
        return new HashSet<>(ids);
    }
}
