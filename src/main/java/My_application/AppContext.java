package My_application;

import My_application.dao.DAOFactory;
import My_application.dao.jdbc.Database;
import My_application.dao.jdbc.JdbcDAOFactory;
import My_application.report.RapportService;
import My_application.service.AuthentificationService;
import My_application.service.CatalogueService;
import My_application.service.DossierService;
import My_application.service.JournalAcces;
import My_application.service.JournalAccesEnBase;
import My_application.service.PatientService;
import My_application.service.RendezVousService;
import My_application.service.Session;

/**
 * Point d'assemblage de l'application (« composition root »).
 * <p>
 * C'est le seul endroit qui choisit les implémentations concrètes : la base
 * MySQL, la fabrique de DAO JDBC, les services. Les vues reçoivent leurs
 * contrôleurs, qui reçoivent leurs services : aucune classe ne fait plus
 * {@code connexion.getConnexion()} elle-même.
 * <p>
 * Les tests appellent {@link #initialiser(DAOFactory)} avec une fabrique de
 * DAO en mémoire.
 */
public final class AppContext {

    /** Identifiants par défaut (ceux codés en dur dans la version d'origine). */
    public static final int ID_UTILISATEUR_DEFAUT = 1;
    public static final int ID_MEDECIN_DEFAUT = 1;

    private static AppContext instance;

    private final DAOFactory daos;
    private final PatientService patientService;
    private final DossierService dossierService;
    private final CatalogueService catalogueService;
    private final RendezVousService rendezVousService;
    private final RapportService rapportService;
    private final Session session;
    private final JournalAcces journal;
    private final AuthentificationService authentificationService;
    private AutoCloseable ressource;

    private AppContext(DAOFactory daos) {
        this.daos = daos;
        this.session = new Session(daos,
                Integer.getInteger("gestimed.utilisateur", ID_UTILISATEUR_DEFAUT),
                Integer.getInteger("gestimed.medecin", ID_MEDECIN_DEFAUT));
        this.journal = new JournalAccesEnBase(daos.getJournalDao(), session);
        this.authentificationService = new AuthentificationService(daos, session, journal);
        this.patientService = new PatientService(daos, journal);
        this.dossierService = new DossierService(daos, journal);
        this.catalogueService = new CatalogueService(daos);
        this.rendezVousService = new RendezVousService(daos, journal);
        this.rapportService = new RapportService();
    }

    public static synchronized AppContext get() {
        if (instance == null) {
            Database base = Database.getInstance();
            instance = new AppContext(new JdbcDAOFactory(base));
            instance.ressource = base;
        }
        return instance;
    }

    /** Remplace l'assemblage (tests, autre base de données). */
    public static synchronized AppContext initialiser(DAOFactory daos) {
        instance = new AppContext(daos);
        return instance;
    }

    /** À appeler à la fermeture de l'application : libère la connexion. */
    public static synchronized void fermer() {
        if (instance != null && instance.ressource != null) {
            try {
                instance.ressource.close();
            } catch (Exception ignored) {
                // fermeture de l'application : rien d'autre à faire
            }
        }
    }

    public DAOFactory getDaos() {
        return daos;
    }

    public PatientService getPatientService() {
        return patientService;
    }

    public DossierService getDossierService() {
        return dossierService;
    }

    public CatalogueService getCatalogueService() {
        return catalogueService;
    }

    public RendezVousService getRendezVousService() {
        return rendezVousService;
    }

    public RapportService getRapportService() {
        return rapportService;
    }

    public Session getSession() {
        return session;
    }

    public AuthentificationService getAuthentificationService() {
        return authentificationService;
    }

    public JournalAcces getJournal() {
        return journal;
    }
}
