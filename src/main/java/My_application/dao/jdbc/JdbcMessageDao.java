package My_application.dao.jdbc;

import My_application.dao.MessageDao;
import My_application.entities.MessageInterne;
import My_application.entities.patient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcMessageDao implements MessageDao {

    private final JdbcHelper jdbc;

    public JdbcMessageDao(JdbcHelper jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<MessageInterne> MAPPER = new RowMapper<MessageInterne>() {
        @Override
        public MessageInterne map(ResultSet rs) throws SQLException {
            MessageInterne m = new MessageInterne();
            m.setIdMessage(rs.getInt("idMessage"));
            m.setIdExpediteur((Integer) rs.getObject("idExpediteur"));
            m.setIdDestinataire((Integer) rs.getObject("idDestinataire"));
            m.setTexte(rs.getString("texte"));
            Timestamp envoye = rs.getTimestamp("envoyeLe");
            m.setEnvoyeLe(envoye == null ? null : envoye.toLocalDateTime());
            if (m.getIdExpediteur() != null) {
                m.setNomExpediteur(rs.getString("nom") + " " + rs.getString("prenom"));
            }
            Integer idPatient = (Integer) rs.getObject("idPatient");
            if (idPatient != null) {
                patient p = new patient();
                p.setIdentPat(idPatient);
                p.setNom(rs.getString("nomPatient"));
                p.setPrenom(rs.getString("prenomPatient"));
                m.setPatient(p);
            }
            m.setLuParDestinataire(rs.getInt("lu") > 0);
            return m;
        }
    };

    @Override
    public void insert(MessageInterne m) {
        Integer idPatient = m.getPatient() == null ? null : m.getPatient().getIdentPat();
        m.setIdMessage(jdbc.insertAndGetKey("INSERT INTO message_interne (idExpediteur, idDestinataire, texte, "
                + "idPatient, envoyeLe) VALUES (?,?,?,?,?)",
                m.getIdExpediteur(), m.getIdDestinataire(), m.getTexte(), idPatient, m.getEnvoyeLe()));
    }

    /** Messages du fil : « Tout le cabinet », ou les deux sens d'une conversation privée. */
    private static String filtre(Integer autre) {
        return autre == null ? "m.idDestinataire IS NULL"
                : "((m.idExpediteur=? AND m.idDestinataire=?) OR (m.idExpediteur=? AND m.idDestinataire=?))";
    }

    private static Object[] parametres(int moi, Integer autre, Object... suite) {
        List<Object> l = new ArrayList<>();
        if (autre != null) {
            Collections.addAll(l, moi, autre, autre, moi);
        }
        Collections.addAll(l, suite);
        return l.toArray();
    }

    @Override
    public List<MessageInterne> conversation(int moi, Integer autre, int limite) {
        List<MessageInterne> l = jdbc.query("SELECT m.idMessage, m.idExpediteur, m.idDestinataire, m.texte, "
                + "m.idPatient, m.envoyeLe, u.nom, u.prenom, p.nomPatient, p.prenomPatient, "
                + "(SELECT COUNT(*) FROM message_lu l WHERE l.idMessage = m.idMessage "
                + "AND l.idUtilisateur = m.idDestinataire) AS lu "
                + "FROM message_interne m LEFT JOIN utilisateur u ON u.idUtilisateur = m.idExpediteur "
                + "LEFT JOIN patient p ON p.idPatient = m.idPatient "
                + "WHERE " + filtre(autre) + " ORDER BY m.idMessage DESC LIMIT ?",
                MAPPER, parametres(moi, autre, limite));
        Collections.reverse(l);
        return l;
    }

    /** Messages reçus par {@code moi} (privés ou pour tous) qu'il n'a pas écrits. */
    private static final String RECUS = "(m.idDestinataire=? OR m.idDestinataire IS NULL) "
            + "AND (m.idExpediteur IS NULL OR m.idExpediteur<>?)";

    @Override
    public Map<Integer, Integer> nonLus(int moi) {
        final Map<Integer, Integer> n = new HashMap<>();
        jdbc.query("SELECT CASE WHEN m.idDestinataire IS NULL THEN " + TOUT_LE_CABINET
                + " ELSE m.idExpediteur END AS fil, COUNT(*) AS n "
                + "FROM message_interne m LEFT JOIN message_lu l "
                + "ON l.idMessage = m.idMessage AND l.idUtilisateur = ? "
                + "WHERE l.idMessage IS NULL AND " + RECUS + " GROUP BY fil",
                new RowMapper<Void>() {
                    @Override
                    public Void map(ResultSet rs) throws SQLException {
                        n.put(rs.getInt("fil"), rs.getInt("n"));
                        return null;
                    }
                }, moi, moi, moi);
        return n;
    }

    @Override
    public int marquerLus(int moi, Integer autre, LocalDateTime quand) {
        String fil = autre == null ? "m.idDestinataire IS NULL" : "m.idExpediteur=? AND m.idDestinataire=?";
        List<Object> p = new ArrayList<>();
        Collections.addAll(p, moi, quand, moi);
        if (autre != null) {
            Collections.addAll(p, autre, moi);
        }
        p.add(moi);
        return jdbc.update("INSERT INTO message_lu (idMessage, idUtilisateur, luLe) "
                + "SELECT m.idMessage, ?, ? FROM message_interne m LEFT JOIN message_lu l "
                + "ON l.idMessage = m.idMessage AND l.idUtilisateur = ? "
                + "WHERE l.idMessage IS NULL AND " + fil
                + " AND (m.idExpediteur IS NULL OR m.idExpediteur<>?)", p.toArray());
    }

    @Override
    public long dernier() {
        Long n = jdbc.queryOne("SELECT COALESCE(MAX(idMessage), 0) AS dernier FROM message_interne",
                rs -> rs.getLong("dernier"));
        return n == null ? 0 : n;
    }

    @Override
    public int purgerAvant(LocalDateTime limite) {
        return jdbc.update("DELETE FROM message_interne WHERE envoyeLe < ?", limite);
    }
}
