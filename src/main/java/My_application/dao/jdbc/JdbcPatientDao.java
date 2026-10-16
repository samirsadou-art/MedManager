package My_application.dao.jdbc;

import My_application.dao.PatientDao;
import My_application.entities.patient;
import java.util.List;

public class JdbcPatientDao implements PatientDao {

    static final String COLONNES_ECRITURE = "nomPatient, prenomPatient, date_Nais_Patient, sexePatient, "
            + "num_sec_soc_Patient, paysPatient, wilayaPatient, villePatient, telPatient, mailPatient, "
            + "localitePatient, civilePatient";

    private final JdbcHelper jdbc;

    public JdbcPatientDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public patient insert(patient p) {
        if (p.getIdentPat() > 0) {
            // Numéro de dossier saisi par l'utilisateur.
            jdbc.update("INSERT INTO patient (idPatient, " + COLONNES_ECRITURE + ") "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)", concat(p.getIdentPat(), valeurs(p)));
        } else {
            int id = jdbc.insertAndGetKey("INSERT INTO patient (" + COLONNES_ECRITURE + ") "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", valeurs(p));
            p.setIdentPat(id);
        }
        return p;
    }

    @Override
    public void update(patient p) {
        jdbc.update("UPDATE patient SET nomPatient=?, prenomPatient=?, date_Nais_Patient=?, sexePatient=?, "
                + "num_sec_soc_Patient=?, paysPatient=?, wilayaPatient=?, villePatient=?, telPatient=?, "
                + "mailPatient=?, localitePatient=?, civilePatient=? WHERE idPatient=?",
                concatFin(valeurs(p), p.getIdentPat()));
    }

    @Override
    public void delete(Integer id) {
        jdbc.update("DELETE FROM patient WHERE idPatient=?", id);
    }

    @Override
    public patient findById(Integer id) {
        return jdbc.queryOne("SELECT * FROM patient WHERE idPatient=?", Mappers.PATIENT, id);
    }

    @Override
    public List<patient> findAll() {
        return jdbc.query("SELECT * FROM patient ORDER BY nomPatient, prenomPatient", Mappers.PATIENT);
    }

    @Override
    public List<patient> findByNomCommencantPar(String prefixe) {
        if (prefixe == null || prefixe.trim().isEmpty()) {
            return findAll();
        }
        return jdbc.query("SELECT * FROM patient WHERE nomPatient LIKE ? ORDER BY nomPatient, prenomPatient",
                Mappers.PATIENT, JdbcHelper.commencePar(prefixe.trim()));
    }

    private static Object[] valeurs(patient p) {
        return new Object[]{p.getNom(), p.getPrenom(), p.getDat_nais(), p.isMasculin(), p.getNum_soc(),
            p.getPays(), p.getWilaya(), p.getVille(), p.getTelephone(), p.getEmail(), p.getLocalite(),
            p.getSituatMarit()};
    }

    private static Object[] concat(Object premier, Object[] reste) {
        Object[] r = new Object[reste.length + 1];
        r[0] = premier;
        System.arraycopy(reste, 0, r, 1, reste.length);
        return r;
    }

    private static Object[] concatFin(Object[] debut, Object dernier) {
        Object[] r = new Object[debut.length + 1];
        System.arraycopy(debut, 0, r, 0, debut.length);
        r[debut.length] = dernier;
        return r;
    }
}
