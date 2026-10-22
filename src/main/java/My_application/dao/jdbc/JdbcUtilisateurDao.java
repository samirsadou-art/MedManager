package My_application.dao.jdbc;

import My_application.dao.UtilisateurDao;
import My_application.entities.Compte;
import My_application.entities.Role;
import My_application.entities.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class JdbcUtilisateurDao implements UtilisateurDao {

    private static final String COLONNES = "idUtilisateur, nom, prenom, qualite, role, login, actif, idMedecin, doitChanger, "
            + "derniereConnexion";

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
        jdbc.update("UPDATE utilisateur SET login=?, motDePasse=?, doitChanger=0 WHERE idUtilisateur=?",
                login, empreinte, idUtilisateur);
    }

    @Override
    public void definirMotDePasse(int idUtilisateur, String empreinte, boolean doitChanger) {
        jdbc.update("UPDATE utilisateur SET motDePasse=?, doitChanger=? WHERE idUtilisateur=?",
                empreinte, doitChanger, idUtilisateur);
    }

    @Override
    public void modifier(User u) {
        jdbc.update("UPDATE utilisateur SET nom=?, prenom=?, qualite=? WHERE idUtilisateur=?",
                u.getNom(), u.getPrenom(), u.getQuailite(), u.getIdent());
    }

    @Override
    public void definirActif(int idUtilisateur, boolean actif) {
        jdbc.update("UPDATE utilisateur SET actif=? WHERE idUtilisateur=?", actif, idUtilisateur);
    }

    @Override
    public User insert(User u) {
        int id = jdbc.insertAndGetKey("INSERT INTO utilisateur (nom, prenom, qualite, role, actif, idMedecin) "
                + "VALUES (?,?,?,?,?,?)", u.getNom(), u.getPrenom(), u.getQuailite(), u.getRole().name(),
                u.isActif(), u.getIdMedecin());
        u.setIdent(id);
        return u;
    }

    @Override
    public void definirRole(int idUtilisateur, Role role, Integer idMedecin) {
        jdbc.update("UPDATE utilisateur SET role=?, idMedecin=? WHERE idUtilisateur=?",
                role.name(), idMedecin, idUtilisateur);
    }

    @Override
    public void noterConnexion(int idUtilisateur, LocalDateTime quand) {
        jdbc.update("UPDATE utilisateur SET derniereConnexion=? WHERE idUtilisateur=?", quand, idUtilisateur);
    }
}
