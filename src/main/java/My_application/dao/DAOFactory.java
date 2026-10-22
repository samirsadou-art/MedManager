package My_application.dao;

/**
 * Fabrique abstraite de DAO (patron Abstract Factory).
 * <p>
 * Les services ne connaissent que ces interfaces : on peut remplacer
 * l'implémentation JDBC/MySQL par une autre (base de test en mémoire,
 * autre SGBD) sans toucher aux services ni aux vues.
 */
public interface DAOFactory {

    PatientDao getPatientDao();

    MedecinDao getMedecinDao();

    UtilisateurDao getUtilisateurDao();

    ConsultationDao getConsultationDao();

    SymptomeDao getSymptomeDao();

    PathologieDao getPathologieDao();

    MedicamentDao getMedicamentDao();

    PrescriptionDao getPrescriptionDao();

    AnalyseDao getAnalyseDao();

    DemandeAnalyseDao getDemandeAnalyseDao();

    RendezVousDao getRendezVousDao();

    JournalDao getJournalDao();

    SalleAttenteDao getSalleAttenteDao();

    EvenementDao getEvenementDao();

    MessageDao getMessageDao();

    PresenceDao getPresenceDao();

    StatistiquesDao getStatistiquesDao();

    /**
     * Exécute un traitement de façon atomique (tout ou rien) sur les DAO de
     * cette fabrique.
     */
    <T> T enTransaction(UniteDeTravail<T> travail);
}
