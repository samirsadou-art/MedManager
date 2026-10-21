package My_application.dao.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

import My_application.dao.DAOFactory;
import My_application.dao.DataAccessException;
import My_application.entities.DossierPatient;
import My_application.entities.EntreeJournal;
import My_application.entities.Rendez_Vous;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.medicament;
import My_application.entities.patient;
import My_application.entities.symptome;
import My_application.fake.Donnees;
import My_application.service.AuthentificationService;
import My_application.service.JournalAccesEnBase;
import My_application.service.MetierException;
import My_application.service.Session;
import My_application.service.CatalogueService;
import My_application.service.DossierService;
import My_application.service.RendezVousService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test d'intégration sur un VRAI serveur MySQL.
 * <p>
 * Ignoré automatiquement (et non en échec) si aucune base de test n'est
 * configurée. Pour l'exécuter, créer une base dédiée puis lancer les tests avec :
 * <pre>
 *   -Dgestimed.test.db.url=jdbc:mysql://localhost:3306/medicale_test
 *   -Dgestimed.test.db.user=root -Dgestimed.test.db.password=...
 * </pre>
 * Par sécurité, le nom de la base doit se terminer par « _test » : avant
 * chaque test, Flyway y SUPPRIME toutes les tables ({@code clean}) puis
 * rejoue les migrations de {@code db/migration}, ce qui vérifie aussi ces
 * scripts sur un vrai serveur.
 */
public class MySqlIntegrationTest {

    private static Database base;
    private static String url;
    private static String user;
    private static String password;
    private static String echecSur;
    private DAOFactory daos;
    private int idMedecin;

    @BeforeClass
    public static void connecter() throws Exception {
        url = System.getProperty("gestimed.test.db.url");
        assumeTrue("Base de test non configurée : test d'intégration ignoré", url != null);
        assumeTrue("Le nom de la base de test doit se terminer par _test", url.matches(".*_test(\\?.*)?$"));
        user = System.getProperty("gestimed.test.db.user", "root");
        password = System.getProperty("gestimed.test.db.password", "");
        base = new Database(new DatabaseConfig(url, user, password,
                System.getProperty("gestimed.test.db.driver", "com.mysql.cj.jdbc.Driver")));
        try {
            base.getConnection();
        } catch (SQLException | DataAccessException e) {
            assumeNoException("Serveur MySQL injoignable", e);
        }
    }

    @AfterClass
    public static void fermer() {
        if (base != null) {
            base.close();
        }
    }

    @Before
    public void recreerLeSchema() throws Exception {
        Flyway flyway = flywayDeTest().load();
        flyway.clean();
        flyway.migrate();
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("INSERT INTO utilisateur (nom, prenom, qualite) VALUES ('SAHAB', 'Said', 'Médecin')");
            st.execute("INSERT INTO medecin (nomMedecin, prenomMedcin, titre) VALUES ('SAHAB', 'Said', 'Dr')");
            st.execute("INSERT INTO medicaments (nomMedicament, forme, type) VALUES ('DOLIPRANE 1G', 'comprimé', 'P')");
        }
        echecSur = null;
        daos = new JdbcDAOFactory(fournisseurAvecPanne());
        idMedecin = 1;
    }

    /** Seule configuration où clean est permis : base *_test vérifiée dans connecter(). */
    private static FluentConfiguration flywayDeTest() {
        return Migrations.configuration(url, user, password).cleanDisabled(false);
    }

    /** Simule une base créée avant Flyway, à la version donnée, sans historique. */
    private void baseAncienneEnVersion(String version) throws SQLException {
        Flyway flyway = flywayDeTest().target(version).load();
        flyway.clean();
        flyway.migrate();
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("DROP TABLE " + Migrations.TABLE_HISTORIQUE);
        }
    }

    @Test
    public void lesMigrationsAmenentUneBaseNeuveALaDerniereVersion() {
        Migrations.Etat etat = new Migrations(url, user, password).etat();
        assertTrue(etat.estSuivie());
        assertFalse(etat.estEnRetard());
        assertEquals(etat.getVersionCode(), etat.getVersionBase());
    }

    @Test
    public void uneBaseAncienneSansHistoriqueEstAdopteePuisMiseAJour() throws Exception {
        baseAncienneEnVersion("1");
        Migrations migrations = new Migrations(url, user, password);
        assertFalse(migrations.etat().estSuivie());
        assertEquals(1, migrations.migrer());
        assertEquals("2", migrations.etat().getVersionBase());
        // V2 a bien été appliquée par-dessus la base existante.
        Session session = new Session(daos, 1, idMedecin);
        assertTrue(new AuthentificationService(daos, session, new JournalAccesEnBase(daos.getJournalDao(), session))
                .premiereUtilisation());
    }

    @Test
    public void uneBaseDejaMigreeALaMainNeRejouePasV2() throws Exception {
        baseAncienneEnVersion("2");
        Migrations migrations = new Migrations(url, user, password);
        assertEquals(0, migrations.migrer());
        assertEquals("2", migrations.etat().getVersionBase());
        assertFalse(migrations.etat().estEnRetard());
    }

    /** Connexion réelle, mais qui peut simuler une panne sur une table donnée. */
    private static ConnectionProvider fournisseurAvecPanne() {
        return new ConnectionProvider() {
            @Override
            public Connection getConnection() throws SQLException {
                final Connection reelle = base.getConnection();
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class}, new InvocationHandler() {
                            @Override
                            public Object invoke(Object p, Method m, Object[] a) throws Throwable {
                                if (m.getName().equals("prepareStatement") && echecSur != null
                                        && ((String) a[0]).contains(echecSur)) {
                                    throw new SQLException("Panne simulée");
                                }
                                try {
                                    return m.invoke(reelle, a);
                                } catch (InvocationTargetException e) {
                                    throw e.getCause();
                                }
                            }
                        });
            }
        };
    }

    private patient creerPatient(String nom) {
        return daos.getPatientDao().insert(Donnees.patient(0, nom));
    }

    @Test
    public void premierCompteConnexionEtJournalSurUneVraieBase() {
        Session session = new Session(daos, 1, idMedecin);
        AuthentificationService auth = new AuthentificationService(daos, session,
                new JournalAccesEnBase(daos.getJournalDao(), session));
        assertTrue(auth.premiereUtilisation());
        auth.creerPremierCompte("medecin", "Cabinet2026".toCharArray(), "Cabinet2026".toCharArray());
        assertTrue(!auth.premiereUtilisation());
        try {
            auth.connecter("medecin", "Mauvais2026".toCharArray());
            fail();
        } catch (MetierException attendu) {
            // refus attendu, tracé sans utilisateur
        }
        assertEquals(1, auth.connecter("MEDECIN", "Cabinet2026".toCharArray()).getIdent());
        patient p = creerPatient("TRACE");
        new DossierService(daos, new JournalAccesEnBase(daos.getJournalDao(), session))
                .ouvrirDossier(p, daos.getMedecinDao().findById(idMedecin));
        List<EntreeJournal> acces = daos.getJournalDao().findByPatient(p.getIdentPat());
        assertEquals(1, acces.size());
        assertEquals("OUVERTURE_DOSSIER", acces.get(0).getAction());
        assertEquals(Integer.valueOf(1), acces.get(0).getIdUtilisateur());
    }

    @Test
    public void patientAvecApostropheEnregistreEtRetrouve() {
        patient p = creerPatient("O'NEIL");
        assertTrue(p.getIdentPat() > 0);
        List<patient> r = daos.getPatientDao().findByNomCommencantPar("O'N");
        assertEquals(1, r.size());
        assertEquals(p.getDat_nais(), r.get(0).getDat_nais());
    }

    @Test
    public void consultationCompleteEnregistreeEtRelue() {
        patient p = creerPatient("AMRANI");
        medecin m = daos.getMedecinDao().findById(idMedecin);
        DossierService service = new DossierService(daos);
        DossierPatient d = service.ouvrirDossier(p, m);
        consultation c = service.nouvelleConsultation(d);
        symptome s = new CatalogueService(daos).obtenirOuCreerSymptome("Maux d'estomac");
        c.addSymptome(s);
        medicament med = daos.getMedicamentDao().findByNom("DOLIPRANE 1G");
        med.setUnite(1);
        med.setPosologie(3);
        c.getOrdoPat().ajouter(med);
        service.enregistrer(d);

        DossierPatient relu = service.ouvrirDossier(p, m);
        assertEquals(1, relu.getNombreConsultation());
        assertEquals("Maux d'estomac", relu.getConsultationAt(0).getSymptomes().get(0).getDesignation());
        assertEquals(3, relu.getConsultationAt(0).getOrdoPat().getMedicament(0).getPosologie());
    }

    @Test
    public void rollbackReelSiUnePartieEchoue() {
        patient p = creerPatient("BELKACEM");
        DossierService service = new DossierService(daos);
        DossierPatient d = service.ouvrirDossier(p, daos.getMedecinDao().findById(idMedecin));
        consultation c = service.nouvelleConsultation(d);
        medicament med = daos.getMedicamentDao().findByNom("DOLIPRANE 1G");
        c.getOrdoPat().ajouter(med);
        echecSur = "prescrire";
        try {
            service.enregistrer(d);
            fail();
        } catch (DataAccessException attendu) {
            // ok
        }
        echecSur = null;
        assertEquals("la consultation doit avoir été annulée (tables InnoDB ?)", 0,
                daos.getConsultationDao().findByPatient(p.getIdentPat()).size());
    }

    @Test
    public void annulationDUnRendezVousNeTouchePasAuxAutres() {
        patient a = creerPatient("A");
        patient b = creerPatient("B");
        RendezVousService rdv = new RendezVousService(daos);
        LocalDate jour = LocalDate.now().plusDays(2);
        Rendez_Vous ra = rdv.proposer(a, Donnees.utilisateur(1), jour, 9);
        rdv.confirmer(ra);
        rdv.confirmer(rdv.proposer(b, Donnees.utilisateur(1), jour, 9));
        rdv.annuler(ra, a);
        List<Rendez_Vous> restants = rdv.duJour(jour);
        assertEquals(1, restants.size());
        assertEquals("B", restants.get(0).getPatient().getNom());
    }
}
