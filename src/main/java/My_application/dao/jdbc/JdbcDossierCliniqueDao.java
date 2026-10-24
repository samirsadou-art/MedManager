package My_application.dao.jdbc;

import My_application.dao.DossierCliniqueDao;
import My_application.entities.Antecedent;
import My_application.entities.NoteDossier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class JdbcDossierCliniqueDao implements DossierCliniqueDao {

    private final JdbcHelper jdbc;

    public JdbcDossierCliniqueDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    private static LocalDateTime moment(ResultSet rs, String colonne) throws SQLException {
        Timestamp t = rs.getTimestamp(colonne);
        return t == null ? null : t.toLocalDateTime();
    }

    private static final RowMapper<Antecedent> ANTECEDENT = (ResultSet rs) -> {
        Antecedent a = new Antecedent();
        a.setIdAntecedent(rs.getInt("idAntecedent"));
        a.setIdPatient(rs.getInt("idPatient"));
        a.setCategorie(Antecedent.Categorie.valueOf(rs.getString("categorie")));
        a.setLibelle(rs.getString("libelle"));
        a.setComplement(rs.getString("complement"));
        a.setIdMedecin((Integer) rs.getObject("idMedecin"));
        a.setSaisiLe(moment(rs, "saisiLe"));
        return a;
    };

    private static final RowMapper<NoteDossier> NOTE = (ResultSet rs) -> {
        NoteDossier n = new NoteDossier();
        n.setIdNote(rs.getInt("idNote"));
        n.setIdPatient(rs.getInt("idPatient"));
        n.setTexte(rs.getString("texte"));
        n.setIdMedecin((Integer) rs.getObject("idMedecin"));
        n.setEcriteLe(moment(rs, "ecriteLe"));
        return n;
    };

    @Override
    public List<Antecedent> findAntecedents(int idPatient) {
        return jdbc.query("SELECT idAntecedent, idPatient, categorie, libelle, complement, idMedecin, saisiLe "
                + "FROM antecedent WHERE idPatient=? ORDER BY categorie, idAntecedent", ANTECEDENT, idPatient);
    }

    @Override
    public void insert(Antecedent a) {
        int id = jdbc.insertAndGetKey("INSERT INTO antecedent (idPatient, categorie, libelle, complement, "
                + "idMedecin, saisiLe) VALUES (?,?,?,?,?,?)", a.getIdPatient(), a.getCategorie().name(),
                a.getLibelle(), a.getComplement(), a.getIdMedecin(), a.getSaisiLe());
        a.setIdAntecedent(id);
    }

    @Override
    public boolean supprimerAntecedent(int idAntecedent) {
        return jdbc.update("DELETE FROM antecedent WHERE idAntecedent=?", idAntecedent) > 0;
    }

    @Override
    public List<NoteDossier> findNotes(int idPatient) {
        return jdbc.query("SELECT idNote, idPatient, texte, idMedecin, ecriteLe FROM note_dossier "
                + "WHERE idPatient=? ORDER BY ecriteLe DESC, idNote DESC", NOTE, idPatient);
    }

    @Override
    public void insert(NoteDossier n) {
        int id = jdbc.insertAndGetKey("INSERT INTO note_dossier (idPatient, texte, idMedecin, ecriteLe) "
                + "VALUES (?,?,?,?)", n.getIdPatient(), n.getTexte(), n.getIdMedecin(), n.getEcriteLe());
        n.setIdNote(id);
    }

    @Override
    public boolean supprimerNote(int idNote) {
        return jdbc.update("DELETE FROM note_dossier WHERE idNote=?", idNote) > 0;
    }
}
