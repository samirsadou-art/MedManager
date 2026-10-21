package My_application.service;

import My_application.dao.JournalDao;
import My_application.entities.EntreeJournal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Journal des accès enregistré dans la table {@code journal_acces}. */
public class JournalAccesEnBase implements JournalAcces {

    static final int LONGUEUR_DETAIL = 255;

    private final JournalDao dao;
    private final Session session;
    private final Clock horloge;
    private final String poste;

    public JournalAccesEnBase(JournalDao dao, Session session) {
        this(dao, session, Clock.systemDefaultZone(), nomDuPoste());
    }

    public JournalAccesEnBase(JournalDao dao, Session session, Clock horloge, String poste) {
        this.dao = dao;
        this.session = session;
        this.horloge = horloge;
        this.poste = poste;
    }

    @Override
    public void noter(Action action, Integer idPatient, String detail) {
        String d = detail != null && detail.length() > LONGUEUR_DETAIL ? detail.substring(0, LONGUEUR_DETAIL) : detail;
        dao.ajouter(new EntreeJournal(LocalDateTime.now(horloge).truncatedTo(ChronoUnit.SECONDS),
                session.getIdUtilisateurConnecte(), action.name(), idPatient, d, poste));
    }

    private static String nomDuPoste() {
        try {
            String nom = InetAddress.getLocalHost().getHostName();
            return nom.length() > 100 ? nom.substring(0, 100) : nom;
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
