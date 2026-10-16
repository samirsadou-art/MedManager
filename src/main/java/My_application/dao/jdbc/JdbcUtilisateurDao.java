package My_application.dao.jdbc;

import My_application.dao.UtilisateurDao;
import My_application.entities.User;

public class JdbcUtilisateurDao implements UtilisateurDao {

    private final JdbcHelper jdbc;

    public JdbcUtilisateurDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public User findById(int id) {
        return jdbc.queryOne("SELECT idUtilisateur, nom, prenom, qualite FROM utilisateur WHERE idUtilisateur=?",
                Mappers.UTILISATEUR, id);
    }
}
