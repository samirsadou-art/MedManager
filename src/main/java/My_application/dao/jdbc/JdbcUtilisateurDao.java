package My_application.dao.jdbc;

import My_application.dao.UtilisateurDao;
import My_application.entities.Compte;
import My_application.entities.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcUtilisateurDao implements UtilisateurDao {

    private static final String COLONNES = "idUtilisateur, nom, prenom, qualite, login, actif, idMedecin";

    private static final RowMapper<Compte> COMPTE = new RowMapper<Compte>() {
        @Override
        public Compte map(ResultSet rs) throws SQLException {
            return new Compte(Mappers.UTILISATEUR.map(rs), rs.getString("motDePasse"));
        }
    };

    private final JdbcHelper jdbc;

    public JdbcUtilisateurDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public User findById(int id) {
        return jdbc.queryOne("SELECT " + COLONNES + " FROM utilisateur WHERE idUtilisateur=?",
                Mappers.UTILISATEUR, id);
    }

    @Override
    public List<User> findAll() {
        return jdbc.query("SELECT " + COLONNES + " FROM utilisateur ORDER BY nom, prenom", Mappers.UTILISATEUR);
    }

    @Override
    public Compte findCompte(String login) {
        return jdbc.queryOne("SELECT " + COLONNES + ", motDePasse FROM utilisateur WHERE login=?", COMPTE, login);
    }

    @Override
    public boolean existeUnCompte() {
        return jdbc.queryOne("SELECT idUtilisateur FROM utilisateur "
                + "WHERE login IS NOT NULL AND motDePasse IS NOT NULL LIMIT 1",
                new RowMapper<Integer>() {
                    @Override
                    public Integer map(ResultSet rs) throws SQLException {
                        return rs.getInt(1);
                    }
                }) != null;
    }

    @Override
    public void definirIdentifiants(int idUtilisateur, String login, String empreinte) {
        jdbc.update("UPDATE utilisateur SET login=?, motDePasse=? WHERE idUtilisateur=?",
                login, empreinte, idUtilisateur);
    }

    @Override
    public void definirActif(int idUtilisateur, boolean actif) {
        jdbc.update("UPDATE utilisateur SET actif=? WHERE idUtilisateur=?", actif, idUtilisateur);
    }
}
