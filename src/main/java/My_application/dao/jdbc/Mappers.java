package My_application.dao.jdbc;

import My_application.entities.User;
import My_application.entities.medecin;
import My_application.entities.patient;
import java.lang.ref.WeakReference;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Correspondances ResultSet → entités partagées entre plusieurs DAO
 * (un patient est lu par PatientDao, RendezVousDao et ConsultationDao).
 */
final class Mappers {

    private Mappers() {
    }

    /** Colonnes minimales d'un patient (présentes dans toutes les requêtes). */
    static final RowMapper<patient> PATIENT = new RowMapper<patient>() {
        @Override
        public patient map(ResultSet rs) throws SQLException {
            patient p = new patient();
            p.setIdentPat(rs.getInt("idPatient"));
            p.setNom(rs.getString("nomPatient"));
            p.setPrenom(rs.getString("prenomPatient"));
            p.setDat_nais(jour(rs, "date_Nais_Patient"));
            p.setSexe(rs.getBoolean("sexePatient"));
            Set<String> colonnes = colonnes(rs);
            p.setNum_soc(chaine(rs, colonnes, "num_sec_soc_Patient"));
            p.setPays(chaine(rs, colonnes, "paysPatient"));
            p.setWilaya(chaine(rs, colonnes, "wilayaPatient"));
            p.setVille(chaine(rs, colonnes, "villePatient"));
            p.setTelephone(chaine(rs, colonnes, "telPatient"));
            p.setEmail(chaine(rs, colonnes, "mailPatient"));
            p.setLocalite(chaine(rs, colonnes, "localitePatient"));
            p.setSituatMarit(chaine(rs, colonnes, "civilePatient"));
            return p;
        }
    };

    static final RowMapper<medecin> MEDECIN = new RowMapper<medecin>() {
        @Override
        public medecin map(ResultSet rs) throws SQLException {
            medecin m = new medecin();
            m.setIdmedecin(rs.getInt("idMedcin"));
            m.setOrdmedcin(rs.getString("ordreMedecin"));
            m.setNom(rs.getString("nomMedecin"));
            m.setPrenom(rs.getString("prenomMedcin"));
            m.setTitre(rs.getString("titre"));
            m.setSpecialite(rs.getString("specialite"));
            m.setAdresse(rs.getString("adresse"));
            m.setTelephone1(rs.getString("telephone1"));
            m.setTelephone2(rs.getString("telephone2"));
            m.setEmail(rs.getString("email"));
            m.setNom_cabinet(rs.getString("nomCabinet"));
            return m;
        }
    };

    static final RowMapper<User> UTILISATEUR = new RowMapper<User>() {
        @Override
        public User map(ResultSet rs) throws SQLException {
            User u = new User(rs.getInt("idUtilisateur"), rs.getString("nom"),
                    rs.getString("prenom"), rs.getString("qualite"));
            u.setLogin(rs.getString("login"));
            u.setActif(rs.getBoolean("actif"));
            u.setIdMedecin((Integer) rs.getObject("idMedecin"));
            return u;
        }
    };

    /** Colonnes du dernier ResultSet lu par ce fil : les métadonnées ne sont lues qu'une fois par requête. */
    private static final class ColonnesLues {
        final WeakReference<ResultSet> resultSet;
        final Set<String> noms;

        ColonnesLues(ResultSet rs, Set<String> noms) {
            this.resultSet = new WeakReference<>(rs);
            this.noms = noms;
        }
    }

    private static final ThreadLocal<ColonnesLues> DERNIERES_COLONNES = new ThreadLocal<>();

    /** Noms des colonnes du ResultSet (en minuscules) pour les colonnes optionnelles. */
    static Set<String> colonnes(ResultSet rs) throws SQLException {
        ColonnesLues dejaLues = DERNIERES_COLONNES.get();
        if (dejaLues != null && dejaLues.resultSet.get() == rs) {
            return dejaLues.noms;
        }
        ResultSetMetaData md = rs.getMetaData();
        Set<String> noms = new HashSet<>();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            noms.add(md.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }
        DERNIERES_COLONNES.set(new ColonnesLues(rs, noms));
        return noms;
    }

    /** Colonne DATE en {@link LocalDate} (null si NULL). Le domaine n'utilise plus java.sql.Date. */
    static LocalDate jour(ResultSet rs, String colonne) throws SQLException {
        Date d = rs.getDate(colonne);
        return d == null ? null : d.toLocalDate();
    }

    /** Colonne TIME en {@link LocalTime} (null si NULL). */
    static LocalTime heure(ResultSet rs, String colonne) throws SQLException {
        Time t = rs.getTime(colonne);
        return t == null ? null : t.toLocalTime();
    }

    static String chaine(ResultSet rs, Set<String> colonnes, String nom) throws SQLException {
        return colonnes.contains(nom.toLowerCase(Locale.ROOT)) ? rs.getString(nom) : null;
    }
}
