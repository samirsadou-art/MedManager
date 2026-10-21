package My_application.dao.jdbc;

import My_application.dao.AnalyseDao;
import My_application.dao.ConsultationDao;
import My_application.dao.DAOFactory;
import My_application.dao.DemandeAnalyseDao;
import My_application.dao.JournalDao;
import My_application.dao.MedecinDao;
import My_application.dao.MedicamentDao;
import My_application.dao.PathologieDao;
import My_application.dao.PatientDao;
import My_application.dao.PrescriptionDao;
import My_application.dao.RendezVousDao;
import My_application.dao.SymptomeDao;
import My_application.dao.UniteDeTravail;
import My_application.dao.UtilisateurDao;

/**
 * Fabrique concrète : DAO JDBC partageant le même {@link ConnectionProvider},
 * à travers le {@link TransactionManager} (et donc la même transaction).
 */
public class JdbcDAOFactory implements DAOFactory {

    private final TransactionManager transactions;
    private final PatientDao patientDao;
    private final MedecinDao medecinDao;
    private final UtilisateurDao utilisateurDao;
    private final ConsultationDao consultationDao;
    private final SymptomeDao symptomeDao;
    private final PathologieDao pathologieDao;
    private final MedicamentDao medicamentDao;
    private final PrescriptionDao prescriptionDao;
    private final AnalyseDao analyseDao;
    private final DemandeAnalyseDao demandeAnalyseDao;
    private final RendezVousDao rendezVousDao;
    private final JournalDao journalDao;

    public JdbcDAOFactory(ConnectionProvider provider) {
        // Les DAO passent par le gestionnaire de transactions : pendant une
        // transaction, ils reçoivent sa connexion et partagent son verrou.
        this.transactions = new TransactionManager(provider);
        JdbcHelper jdbc = new JdbcHelper(transactions, transactions.verrou());
        this.patientDao = new JdbcPatientDao(jdbc);
        this.medecinDao = new JdbcMedecinDao(jdbc);
        this.utilisateurDao = new JdbcUtilisateurDao(jdbc);
        this.consultationDao = new JdbcConsultationDao(jdbc);
        this.symptomeDao = new JdbcSymptomeDao(jdbc);
        this.pathologieDao = new JdbcPathologieDao(jdbc);
        this.medicamentDao = new JdbcMedicamentDao(jdbc);
        this.prescriptionDao = new JdbcPrescriptionDao(jdbc);
        this.analyseDao = new JdbcAnalyseDao(jdbc);
        this.demandeAnalyseDao = new JdbcDemandeAnalyseDao(jdbc);
        this.rendezVousDao = new JdbcRendezVousDao(jdbc);
        this.journalDao = new JdbcJournalDao(jdbc);
    }

    @Override
    public <T> T enTransaction(UniteDeTravail<T> travail) {
        return transactions.executer(travail);
    }

    @Override
    public PatientDao getPatientDao() {
        return patientDao;
    }

    @Override
    public MedecinDao getMedecinDao() {
        return medecinDao;
    }

    @Override
    public UtilisateurDao getUtilisateurDao() {
        return utilisateurDao;
    }

    @Override
    public ConsultationDao getConsultationDao() {
        return consultationDao;
    }

    @Override
    public SymptomeDao getSymptomeDao() {
        return symptomeDao;
    }

    @Override
    public PathologieDao getPathologieDao() {
        return pathologieDao;
    }

    @Override
    public MedicamentDao getMedicamentDao() {
        return medicamentDao;
    }

    @Override
    public PrescriptionDao getPrescriptionDao() {
        return prescriptionDao;
    }

    @Override
    public AnalyseDao getAnalyseDao() {
        return analyseDao;
    }

    @Override
    public DemandeAnalyseDao getDemandeAnalyseDao() {
        return demandeAnalyseDao;
    }

    @Override
    public RendezVousDao getRendezVousDao() {
        return rendezVousDao;
    }

    @Override
    public JournalDao getJournalDao() {
        return journalDao;
    }
}
