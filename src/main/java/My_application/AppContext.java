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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Point d'assemblage de l'application (« composition root »).
 * <p>
 * C'est le seul endroit qui choisit les implémentations concrètes : la base
 * MySQL, la fabrique de DAO JDBC, les services. Il est créé une seule fois,
 * dans {@code PrincipalForm.main} (ou par un outil en ligne de commande), puis
 * passé par constructeur : les fenêtres qui en ouvrent d'autres le reçoivent,
 * les autres ne reçoivent que les services dont elles ont besoin. Il n'y a
 * plus d'instance globale.
 */
public final class AppContext implements AutoCloseable {

    /** Identifiants par défaut (ceux codés en dur dans la version d'origine). */
    public static final int ID_UTILISATEUR_DEFAUT = 1;
    public static final int ID_MEDECIN_DEFAUT = 1;

    private final DAOFactory daos;
    private final PatientService patientService;
    private final DossierService dossierService;
    private final CatalogueService catalogueService;
    private final RendezVousService rendezVousService;
    private final RapportService rapportService;
    private final Session session;
    private final JournalAcces journal;
    private final AuthentificationService authentificationService;
    private final AutoCloseable ressource;

    /** Assemblage sur une fabrique de DAO donnée (autre base, tests). */
    public AppContext(DAOFactory daos) {
        this(daos, null);
    }

    private AppContext(DAOFactory daos, AutoCloseable ressource) {
        this.ressource = ressource;
        this.daos = daos;
        this.session = new Session(daos,
                Integer.getInteger("gestimed.utilisateur", ID_UTILISATEUR_DEFAUT),
                Integer.getInteger("gestimed.medecin", ID_MEDECIN_DEFAUT));
        this.journal = new JournalAccesEnBase(daos.getJournalDao(), session);
        this.authentificationService = new AuthentificationService(daos, session, journal);
        this.patientService = new PatientService(daos, journal);
        this.dossierService = new DossierService(daos, journal);
        this.catalogueService = new CatalogueService(daos);
        this.rendezVousService = new RendezVousService(daos, journal, session::getIdMedecin);
        this.rapportService = new RapportService();
    }

    /** Assemblage de production : base MySQL de {@code gestimed.properties}. */
    public static AppContext mysql() {
        Database base = Database.getInstance();
        return new AppContext(new JdbcDAOFactory(base), base);
    }

    /** À appeler à la fermeture de l'application : libère la connexion. */
    @Override
    public void close() {
        if (ressource != null) {
            try {
                ressource.close();
            } catch (Exception e) {
                // Fermeture de l'application : on le note, rien d'autre à faire.
                Logger.getLogger("GestiMed").log(Level.WARNING, "Fermeture de la connexion impossible", e);
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
