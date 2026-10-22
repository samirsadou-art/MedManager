package My_application.dao.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

import My_application.dao.DAOFactory;
import My_application.dao.DataAccessException;
import My_application.entities.DossierPatient;
import My_application.entities.EntreeJournal;
import My_application.entities.MessageInterne;
import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.entities.Statistiques;
import My_application.entities.User;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.medicament;
import My_application.entities.patient;
import My_application.entities.symptome;
import My_application.fake.Donnees;
import My_application.service.AuthentificationService;
import My_application.service.JournalAcces;
import My_application.service.JournalAccesEnBase;
import My_application.service.MembresService;
import My_application.service.MessagerieService;
import My_application.service.PatientService;
import My_application.service.MetierException;
import My_application.service.Session;
import My_application.service.CatalogueService;
import My_application.service.DossierService;
import My_application.service.RendezVousService;
import My_application.service.SalleAttenteService;
import My_application.service.StatistiquesService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            st.execute("INSERT INTO medecin (nomMedecin, prenomMedecin, titre) VALUES ('SAHAB', 'Said', 'Dr')");
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
        assertTrue(migrations.migrer() >= 1);
        assertEquals(migrations.etat().getVersionCode(), migrations.etat().getVersionBase());
        // V2 a bien été appliquée par-dessus la base existante.
        Session session = new Session(daos, 1, idMedecin);
        assertTrue(new AuthentificationService(daos, session, new JournalAccesEnBase(daos.getJournalDao(), session))
                .premiereUtilisation());
    }

    @Test
    public void uneBaseDejaMigreeALaMainNeRejouePasV2() throws Exception {
        baseAncienneEnVersion("2");
        Migrations migrations = new Migrations(url, user, password);
        // Adoptée en version 2 : seules les migrations suivantes sont jouées.
        assertEquals(nombreDeMigrationsApres(2), migrations.migrer());
        assertEquals(migrations.etat().getVersionCode(), migrations.etat().getVersionBase());
        assertFalse(migrations.etat().estEnRetard());
    }

    private static int nombreDeMigrationsApres(int version) {
        int n = 0;
        for (org.flywaydb.core.api.MigrationInfo m : flywayDeTest().load().info().all()) {
            if (m.getVersion() != null && Integer.parseInt(m.getVersion().getVersion()) > version) {
                n++;
            }
        }
        return n;
    }

    /**
     * Base réelle en version 2 (comme celle du cabinet) avec des valeurs
     * sentinelles : les migrations suivantes les convertissent sans perte.
     */
    @Test
    public void lesDonneesDUneBaseEnVersion2SontConverties() throws Exception {
        Flyway flyway = flywayDeTest().target("2").load();
        flyway.clean();
        flyway.migrate();
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("INSERT INTO medecin (idMedcin, nomMedecin, prenomMedcin) VALUES (1, 'SAHAB', 'Said')");
            st.execute("INSERT INTO utilisateur (idUtilisateur, nom, prenom) VALUES (1, 'SAHAB', 'Said')");
            st.execute("INSERT INTO patient (idPatient, nomPatient, prenomPatient, date_Nais_Patient, sexePatient) "
                    + "VALUES (1, 'A', 'a', '1980-01-01', 1)");
            st.execute("INSERT INTO consultation (idConsultation, dateConsultation, heureConsultation, idMedecin, "
                    + "idPatient) VALUES (1, '2026-10-01', '09:00', 1, 1)");
            st.execute("INSERT INTO consultation (idConsultation, dateConsultation, heureConsultation, idMedecin, "
                    + "idPatient, temperature, pulsation, pression, respiration, poids) "
                    + "VALUES (2, '2026-10-02', '09:00', 1, 1, 37.5, 80, '12/8', 16, 70)");
            st.execute("INSERT INTO consultation (idConsultation, dateConsultation, heureConsultation, idMedecin, "
                    + "idPatient, pression) VALUES (3, '2026-10-03', '09:00', 1, 1, '135/85')");
            st.execute("INSERT INTO consultation (idConsultation, dateConsultation, heureConsultation, idMedecin, "
                    + "idPatient, pression) VALUES (4, '2026-10-04', '09:00', 1, 1, '8/12')");
            st.execute("INSERT INTO rondezvous (idUser, idPatient, date, heure) VALUES (1, 1, '2026-10-20', '10:00')");
        }
        flywayDeTest().load().migrate();
        try (Statement st = base.getConnection().createStatement()) {
            java.sql.ResultSet rs = st.executeQuery("SELECT temperature, pulsation, pressionSystolique, "
                    + "pressionDiastolique, respiration, poids FROM consultation ORDER BY idConsultation");
            assertTrue(rs.next());
            for (int i = 1; i <= 6; i++) {
                rs.getObject(i);
                assertTrue("colonne " + i + " : -1 devenu NULL", rs.wasNull());
            }
            assertTrue(rs.next());
            assertEquals(37.5, rs.getDouble(1), 0);
            assertEquals("12/8 en cmHg devient 120/80 mmHg", 120, rs.getInt(3));
            assertEquals(80, rs.getInt(4));
            assertTrue(rs.next());
            assertEquals("déjà en mmHg", 135, rs.getInt(3));
            assertEquals(85, rs.getInt(4));
            assertTrue(rs.next());
            rs.getObject(3);
            assertTrue("8/12 inversée : non convertie", rs.wasNull());
            rs.close();
            rs = st.executeQuery("SELECT idConsultation, pression FROM consultation_pression_ancienne");
            assertTrue(rs.next());
            assertEquals(4, rs.getInt(1));
            assertEquals("la saisie d'origine est gardée", "8/12", rs.getString(2));
            assertFalse(rs.next());
            rs.close();
            try {
                st.execute("UPDATE consultation SET pressionSystolique = 80, pressionDiastolique = 120 "
                        + "WHERE idConsultation = 2");
                fail("systolique < diastolique doit être refusée");
            } catch (SQLException attendu) {
                // CHECK ck_consultation_pression
            }
            try {
                st.execute("UPDATE consultation SET pressionDiastolique = NULL WHERE idConsultation = 2");
                fail("l'une ne va pas sans l'autre");
            } catch (SQLException attendu) {
                // CHECK ck_consultation_pression
            }
            rs = st.executeQuery("SELECT idRendezVous, idMedecin, etat, prisLe FROM rendezvous");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
            assertEquals("seul médecin du cabinet", 1, rs.getInt(2));
            assertEquals(Rendez_Vous.ETAT_PREVU, rs.getInt(3));
            assertEquals("sans date de prise : celle du rendez-vous",
                    java.sql.Timestamp.valueOf("2026-10-20 10:00:00"), rs.getTimestamp(4));
            rs.close();
            try {
                st.execute("UPDATE consultation SET temperature = -1 WHERE idConsultation = 2");
                fail("la valeur sentinelle doit être refusée");
            } catch (SQLException attendu) {
                // CHECK ck_consultation_temperature
            }
        }
        assertEquals("Said", daos.getMedecinDao().findById(1).getPrenom());
        assertEquals(1, daos.getMedecinDao().findAll().size());
        assertEquals("le numéro de dossier reprend idPatient (V15)", "1",
                daos.getPatientDao().findById(1).getNumeroDossier());
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
        patient p = Donnees.patient(0, nom);
        p.setNumeroDossier(daos.getPatientDao().prochainNumeroDossier());
        return daos.getPatientDao().insert(p);
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
        rdv.annuler(ra);
        List<Rendez_Vous> restants = rdv.duJour(jour);
        assertEquals(1, restants.size());
        assertEquals("B", restants.get(0).getPatient().getNom());
    }

    @Test
    public void unCreneauAnnuleEstRepriseEtUnRendezVousSeDeplace() throws Exception {
        patient a = creerPatient("A");
        patient b = creerPatient("B");
        RendezVousService rdv = new RendezVousService(daos);
        LocalDate jour = LocalDate.now().plusDays(2);
        Rendez_Vous ra = rdv.proposer(a, Donnees.utilisateur(1), jour, 9);
        ra.setMotif("Contrôle");
        rdv.confirmer(ra);
        assertNotNull(ra.getIdRendezVous());
        rdv.annuler(ra);
        // La clé unique (V12) ne compte plus le rendez-vous annulé : 9 h 00 est repris.
        Rendez_Vous rb = rdv.proposer(b, Donnees.utilisateur(1), jour, 9);
        assertEquals(ra.getHeure(), rb.getHeure());
        rdv.confirmer(rb);
        // Déplacement de B à 10 h, puis A reprend 9 h.
        rdv.deplacer(rb, rdv.proposerDeplacement(rb, jour, 10, null));
        Rendez_Vous ra2 = rdv.proposer(a, Donnees.utilisateur(1), jour, 9);
        rdv.confirmer(ra2);
        List<Rendez_Vous> lus = rdv.duJour(jour);
        assertEquals(2, lus.size());
        assertEquals("A", lus.get(0).getPatient().getNom());
        assertEquals("B", lus.get(1).getPatient().getNom());
        assertEquals(10, lus.get(1).getHeure().getHour());
        // L'historique garde l'annulé, avec son motif.
        List<Rendez_Vous> deA = rdv.rechercher("A");
        assertEquals(2, deA.size());
        boolean annuleTrouve = false;
        for (Rendez_Vous r : deA) {
            if (r.getEtat() == Rendez_Vous.ETAT_ANNULE) {
                annuleTrouve = true;
                assertEquals("Contrôle", r.getMotif());
            }
        }
        assertTrue(annuleTrouve);
        // Un rendez-vous passé non pointé peut être marqué absent.
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("INSERT INTO rendezvous (idUser, idPatient, idMedecin, date, heure) VALUES (1, "
                    + b.getIdentPat() + ", 1, '" + LocalDate.now().minusDays(1) + "', '09:00:00')");
        }
        Rendez_Vous hier = rdv.duJour(LocalDate.now().minusDays(1)).get(0);
        rdv.marquerAbsent(hier);
        assertEquals(Rendez_Vous.ETAT_ABSENT, rdv.duJour(LocalDate.now().minusDays(1)).get(0).getEtat());
    }

    @Test
    public void salleDAttenteDeLArriveeALaFinDeConsultation() throws Exception {
        patient a = creerPatient("A");
        patient b = creerPatient("B");
        RendezVousService rdv = new RendezVousService(daos);
        SalleAttenteService salle = new SalleAttenteService(daos, rdv);
        long avant = salle.dernierEvenement();
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("INSERT INTO rendezvous (idUser, idPatient, idMedecin, date, heure) VALUES (1, "
                    + a.getIdentPat() + ", 1, '" + LocalDate.now() + "', '00:15:00')");
        }
        Rendez_Vous ra = rdv.duJour(LocalDate.now()).get(0);
        salle.arriveeSansRendezVous(b, null);
        salle.arrivee(ra);
        assertTrue("chaque arrivée publie un événement", salle.dernierEvenement() >= avant + 2);
        List<Passage> file = salle.file(1);
        assertEquals("le rendez-vous passe avant l'arrivée sans rendez-vous", "A",
                file.get(0).getPatient().getNom());
        assertEquals(15, file.get(0).getHeurePrevue().getMinute());
        assertTrue(file.get(1).isSansRendezVous());
        // La clé unique (V13) refuse un second passage du même patient.
        try {
            salle.arriveeSansRendezVous(b, 1);
            fail();
        } catch (MetierException attendue) {
            assertTrue(attendue.getMessage().contains("déjà dans la salle"));
        }
        assertEquals(Rendez_Vous.ETAT_ARRIVE, rdv.duJour(LocalDate.now()).get(0).getEtat());

        Passage appele = salle.appelerSuivant(1);
        assertEquals("A", appele.getPatient().getNom());
        assertEquals(Rendez_Vous.ETAT_EN_CONSULTATION, rdv.duJour(LocalDate.now()).get(0).getEtat());
        DossierService dossiers = new DossierService(daos);
        DossierPatient d = dossiers.ouvrirDossier(appele.getPatient(), daos.getMedecinDao().findById(idMedecin));
        dossiers.nouvelleConsultation(d);
        dossiers.enregistrer(d);
        assertEquals(Rendez_Vous.ETAT_TERMINE, rdv.duJour(LocalDate.now()).get(0).getEtat());
        assertEquals(1, salle.file(1).size());

        // Sorti, A peut revenir dans la journée (present vaut NULL une fois terminé).
        salle.arriveeSansRendezVous(a, 1);
        salle.partiSansConsultation(salle.file(1).get(0));
        SalleAttenteService.Bilan bilan = salle.bilan();
        assertEquals(1, bilan.getVus());
        assertEquals("B, arrivé le premier, est parti", 1, bilan.getPartis());
        assertEquals(1, bilan.getEnAttente());
        assertEquals(2, bilan.getSansRendezVous());
        assertEquals(0, salle.purgerEvenements());
    }

    @Test
    public void messagerieSurUneVraieBase() throws Exception {
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("UPDATE utilisateur SET login='sahab' WHERE idUtilisateur=1");
            st.execute("INSERT INTO utilisateur (nom, prenom, qualite, login, role) "
                    + "VALUES ('BELKACEM', 'Lila', 'Secrétaire', 'lila', 'SECRETAIRE')");
        }
        patient p = creerPatient("CITE");
        MessagerieService medecin = new MessagerieService(daos, () -> 1);
        MessagerieService secretaire = new MessagerieService(daos, () -> 2);
        secretaire.envoyer(1, "Les résultats de ce patient sont arrivés.", p);
        secretaire.envoyer(null, "Réunion à 13 h", null);
        medecin.envoyer(null, "D'accord", null);

        List<MessagerieService.Conversation> fils = medecin.conversations();
        assertEquals(2, fils.size());
        assertEquals("un message à tous non lu", 1, fils.get(0).getNonLus());
        assertEquals("un message privé non lu", 1, fils.get(1).getNonLus());
        List<MessageInterne> prive = medecin.lire(2);
        assertEquals("CITE", prive.get(0).getPatient().getNom());
        assertEquals("BELKACEM Lila", prive.get(0).getNomExpediteur());
        assertTrue("accusé de lecture", secretaire.lire(1).get(0).isLuParDestinataire());
        assertEquals(1, MessagerieService.total(medecin.conversations()));
        assertEquals(2, medecin.lire(null).size());
        assertEquals(0, MessagerieService.total(medecin.conversations()));
        assertEquals("lire deux fois ne duplique rien", 0,
                daos.getMessageDao().marquerLus(1, null, LocalDateTime.now()));

        secretaire.signalerPresence("ACCUEIL");
        assertTrue(medecin.conversations().get(1).isEnLigne());
        secretaire.signalerPresence("ACCUEIL");
        secretaire.quitter();
        assertFalse(medecin.conversations().get(1).isEnLigne());

        try (Statement st = base.getConnection().createStatement()) {
            st.execute("UPDATE message_interne SET envoyeLe = NOW() - INTERVAL 13 MONTH WHERE idDestinataire = 1");
        }
        assertEquals("le message privé et son accusé de lecture sont supprimés", 1, medecin.purger());
        assertTrue(medecin.lire(2).isEmpty());
        assertTrue(medecin.dernierMessage() >= 3);
    }

    @Test
    public void deuxMedecinsPeuventRecevoirAuMemeCreneau() throws Exception {
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("INSERT INTO medecin (nomMedecin, prenomMedecin, titre) VALUES ('AMRANI', 'Lina', 'Dr')");
        }
        patient a = creerPatient("A");
        patient b = creerPatient("B");
        LocalDate jour = LocalDate.now().plusDays(2);
        RendezVousService medecin1 = new RendezVousService(daos, JournalAcces.AUCUN, () -> 1);
        RendezVousService medecin2 = new RendezVousService(daos, JournalAcces.AUCUN, () -> 2);
        Rendez_Vous ra = medecin1.proposer(a, Donnees.utilisateur(1), jour, 9);
        medecin1.confirmer(ra);
        Rendez_Vous rb = medecin2.proposer(b, Donnees.utilisateur(1), jour, 9);
        medecin2.confirmer(rb);
        assertEquals(ra.getHeure(), rb.getHeure());
        List<Rendez_Vous> lus = medecin1.duJour(jour);
        assertEquals(2, lus.size());
        assertEquals(Integer.valueOf(1), lus.get(0).getIdMedecin());
        // Même médecin, même créneau : refusé par la contrainte unique.
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("INSERT INTO rendezvous (idUser, idPatient, idMedecin, date, heure) VALUES (1, "
                    + b.getIdentPat() + ", 1, '" + jour + "', '" + ra.getHeure() + "')");
            fail("créneau déjà pris chez ce médecin");
        } catch (SQLException attendu) {
            // uk_rendezvous_creneau
        }
    }

    @Test
    public void inscriptionAvecTelephoneEtDetectionDesDoublons() {
        PatientService service = new PatientService(daos);
        patient p = Donnees.patient(0, "ziani", "Nadia", LocalDate.of(1992, 2, 29), false);
        p.setTelephone("0555 12 34 56");
        service.creer(p);
        patient memeTel = Donnees.patient(0, "AUTRE", "Personne", LocalDate.of(2000, 1, 1), true);
        memeTel.setTelephone("+213 555 12 34 56");
        assertEquals(1, service.doublonsPossibles(memeTel).size());
        assertEquals(p.getIdentPat(), service.rechercher("0555 12").get(0).getIdentPat());
        p.setVille("Fréha");
        service.modifier(p);
        assertEquals("Fréha", daos.getPatientDao().findById(p.getIdentPat()).getVille());
    }

    @Test
    public void numeroDeDossierSepareDeLIdentifiantTechnique() throws Exception {
        PatientService service = new PatientService(daos);
        patient papier = Donnees.patient(0, "ANCIEN", "Dossier", LocalDate.of(1960, 3, 1), true);
        papier.setTelephone("0555000001");
        papier.setNumeroDossier("500");
        service.creer(papier);
        assertEquals("l'auto-incrément ne saute plus au numéro saisi", 1, papier.getIdentPat());

        patient annee = Donnees.patient(0, "ANNEE", "Dossier", LocalDate.of(1970, 3, 1), false);
        annee.setTelephone("0555000002");
        annee.setNumeroDossier(" 2019/37 ");
        service.creer(annee);
        assertEquals("2019/37", annee.getNumeroDossier());

        patient auto = Donnees.patient(0, "AUTO", "Dossier", LocalDate.of(1980, 3, 1), true);
        auto.setTelephone("0555000003");
        service.creer(auto);
        assertEquals("plus grand numéro numérique + 1", "501", auto.getNumeroDossier());
        assertEquals(3, auto.getIdentPat());
        assertEquals("501", daos.getPatientDao().findById(3).getNumeroDossier());

        patient minuscule = Donnees.patient(0, "MINUSCULE", "Dossier", LocalDate.of(1990, 3, 1), true);
        minuscule.setTelephone("0555000004");
        minuscule.setNumeroDossier("a-12");
        service.creer(minuscule);
        assertEquals("A-12", daos.getPatientDao().findByNumeroDossier("a-12").getNumeroDossier());

        auto.setNumeroDossier("2019/37");
        try {
            service.modifier(auto);
            fail("numéro déjà pris");
        } catch (MetierException attendu) {
            assertTrue(attendu.getMessage().contains("déjà utilisé"));
        }
        auto.setNumeroDossier("2019/38");
        service.modifier(auto);
        assertEquals(auto.getIdentPat(), service.rechercher("2019/38").get(0).getIdentPat());
        assertEquals(papier.getIdentPat(), service.rechercher("500").get(0).getIdentPat());

        try (Statement st = base.getConnection().createStatement()) {
            st.execute("INSERT INTO patient (numeroDossier, nomPatient, prenomPatient, date_Nais_Patient, "
                    + "sexePatient) VALUES ('12#', 'X', 'Y', '1990-01-01', 1)");
            fail("CHECK ck_patient_numero_dossier");
        } catch (SQLException attendu) {
            // caractère refusé
        }
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("INSERT INTO patient (numeroDossier, nomPatient, prenomPatient, date_Nais_Patient, "
                    + "sexePatient) VALUES ('a-12', 'X', 'Y', '1990-01-01', 1)");
            fail("unicité insensible à la casse");
        } catch (SQLException attendu) {
            // uk_patient_numero_dossier
        }
    }

    @Test
    public void lesComptesExistantsDeviennentMedecinsEtLesRolesSontEnregistres() throws Exception {
        // Reprise V10 : le compte inséré avant la migration garde l'accès au dossier.
        assertEquals(Role.MEDECIN, daos.getUtilisateurDao().findById(1).getRole());
        MembresService membres = new MembresService(daos, JournalAcces.AUCUN,
                My_application.service.ControleAcces.AUCUN);
        User secretaire = membres.ajouter("benali", "amina", Role.SECRETAIRE, null);
        User admin = membres.ajouter("admin", "cabinet", Role.ADMIN, null);
        assertEquals(Role.SECRETAIRE, daos.getUtilisateurDao().findById(secretaire.getIdent()).getRole());
        membres.definirRole(1, Role.MEDECIN, idMedecin);
        assertEquals(Integer.valueOf(idMedecin), daos.getUtilisateurDao().findById(1).getIdMedecin());
        daos.getUtilisateurDao().noterConnexion(admin.getIdent(), LocalDateTime.of(2026, 10, 22, 8, 0));
        try (Statement st = base.getConnection().createStatement()) {
            java.sql.ResultSet rs = st.executeQuery("SELECT derniereConnexion FROM utilisateur WHERE idUtilisateur="
                    + admin.getIdent());
            assertTrue(rs.next());
            assertEquals(LocalDateTime.of(2026, 10, 22, 8, 0), rs.getTimestamp(1).toLocalDateTime());
            rs.close();
            st.execute("UPDATE utilisateur SET role='CHEF' WHERE idUtilisateur=1");
            fail("rôle hors liste refusé par ck_utilisateur_role");
        } catch (SQLException attendu) {
            // ck_utilisateur_role
        }
    }

    @Test
    public void compteAvecMotDePasseProvisoireEtMedecinQuiQuitteLeCabinet() throws Exception {
        MembresService membres = new MembresService(daos, JournalAcces.AUCUN,
                My_application.service.ControleAcces.AUCUN);
        medecin fiche = new medecin();
        fiche.setNom("amrani");
        fiche.setPrenom("lina");
        fiche.setOrdmedcin("15/987");
        User lina = membres.creerCompte("amrani", "lina", Role.MEDECIN, null, fiche, "dr.amrani",
                "Provisoire1".toCharArray(), "Provisoire1".toCharArray());
        medecin lu = daos.getMedecinDao().findById(lina.getIdMedecin());
        assertEquals("AMRANI", lu.getNom());
        assertTrue(lu.isActif());
        assertTrue(daos.getUtilisateurDao().findById(lina.getIdent()).isDoitChanger());

        // Même numéro d'ordre : refusé par uk_medecin_ordre, sans compte à moitié créé.
        int avant = daos.getUtilisateurDao().findAll().size();
        medecin doublon = new medecin();
        doublon.setNom("autre");
        doublon.setPrenom("medecin");
        doublon.setOrdmedcin("15/987");
        try {
            membres.creerCompte("autre", "medecin", Role.MEDECIN, null, doublon, "autre",
                    "Provisoire1".toCharArray(), "Provisoire1".toCharArray());
            fail();
        } catch (MetierException attendu) {
            assertTrue(attendu.getMessage().contains("numéro d'ordre"));
        }
        assertEquals(avant, daos.getUtilisateurDao().findAll().size());

        Session session = new Session(daos, 1, idMedecin);
        AuthentificationService auth = new AuthentificationService(daos, session, JournalAcces.AUCUN);
        assertTrue(auth.connecter("dr.amrani", "Provisoire1".toCharArray()).isDoitChanger());
        assertTrue(!session.estOuverte());
        auth.remplacerMotDePasseProvisoire("Choisi2026".toCharArray(), "Choisi2026".toCharArray());
        assertTrue(session.estOuverte());
        User relu = daos.getUtilisateurDao().findById(lina.getIdent());
        assertTrue(!relu.isDoitChanger());
        assertNotNull(relu.getDerniereConnexion());

        lu.setSpecialite("Pédiatrie");
        membres.modifierMedecin(lu);
        assertEquals("Pédiatrie", daos.getMedecinDao().findById(lu.getIdmedecin()).getSpecialite());

        patient p = creerPatient("RDV");
        RendezVousService rdv = new RendezVousService(daos, JournalAcces.AUCUN, () -> null);
        Rendez_Vous r = rdv.proposer(p, Donnees.utilisateur(1), LocalDate.now().plusDays(3), 10, lu.getIdmedecin());
        rdv.confirmer(r);
        assertEquals(1, daos.getRendezVousDao().compterPrevus(lu.getIdmedecin(), LocalDate.now()));
        membres.definirActif(lina.getIdent(), false);
        try {
            membres.definirMedecinActif(lu.getIdmedecin(), false);
            fail();
        } catch (MetierException attendu) {
            assertTrue(attendu.getMessage().contains("rendez-vous à venir"));
        }
        assertTrue(daos.getRendezVousDao().changerEtat(r.getIdRendezVous(), Rendez_Vous.ETAT_PREVU,
                Rendez_Vous.ETAT_ANNULE));
        membres.definirMedecinActif(lu.getIdmedecin(), false);
        assertTrue(!daos.getMedecinDao().findById(lu.getIdmedecin()).isActif());
        assertEquals(1, rdv.medecins().size());
        assertEquals(2, rdv.tousLesMedecins().size());
    }

    @Test
    public void statistiquesCalculeesEnSql() throws Exception {
        patient a = creerPatient("A");
        patient b = creerPatient("B");
        LocalDate jour = LocalDate.of(2026, 10, 5);
        try (Statement st = base.getConnection().createStatement()) {
            st.execute("INSERT INTO medecin (nomMedecin, prenomMedecin, titre) VALUES ('AMRANI', 'Lina', 'Dr')");
            String c = "INSERT INTO consultation (dateConsultation, heureConsultation, idMedecin, idPatient) VALUES ";
            st.execute(c + "('2026-09-20', '09:00', 1, " + a.getIdentPat() + ")");
            st.execute(c + "('2026-10-05', '09:00', 1, " + a.getIdentPat() + ")");
            st.execute(c + "('2026-10-05', '10:00', 2, " + b.getIdentPat() + ")");
            st.execute(c + "('2026-10-07', '10:00', 1, " + b.getIdentPat() + ")");
            String r = "INSERT INTO rendezvous (idUser, idPatient, idMedecin, date, heure, etat) VALUES (1, ";
            st.execute(r + a.getIdentPat() + ", 1, '2026-10-05', '09:00', 3)");
            st.execute(r + b.getIdentPat() + ", 1, '2026-10-06', '09:00', 5)");
            st.execute(r + b.getIdentPat() + ", 2, '2026-10-06', '09:00', 4)");
            String p = "INSERT INTO salle_attente (idPatient, idMedecin, idRendezVous, jour, arriveeLe, appeleLe, "
                    + "priorite, etat) VALUES (";
            st.execute(p + a.getIdentPat() + ", 1, 1, '2026-10-05', '2026-10-05 08:50', '2026-10-05 09:10', 0, 2)");
            st.execute(p + b.getIdentPat() + ", 2, NULL, '2026-10-05', '2026-10-05 09:00', '2026-10-05 09:40', 1, 2)");
            st.execute(p + b.getIdentPat() + ", 1, NULL, '2026-10-07', '2026-10-07 09:00', NULL, 0, 3)");
        }
        StatistiquesService stats = new StatistiquesService(daos, My_application.service.ControleAcces.AUCUN);
        Statistiques s = stats.calculer(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null);
        assertEquals(3, s.getConsultations());
        assertEquals(Integer.valueOf(2), s.getConsultationsParJour().get(jour));
        assertEquals(Integer.valueOf(2), s.getConsultationsParMedecin().get(1));
        assertEquals(2, s.getPatientsDistincts());
        assertEquals("A a déjà consulté en septembre", 1, s.getNouveauxPatients());
        assertEquals(1, s.rendezVous(Rendez_Vous.ETAT_ABSENT));
        assertEquals(50.0, s.getTauxAbsence(), 0.001);
        assertEquals(3, s.getSalle().getPassages());
        assertEquals(2, s.getSalle().getSansRendezVous());
        assertEquals(1, s.getSalle().getUrgences());
        assertEquals(1, s.getSalle().getPartis());
        assertEquals(30.0, s.getSalle().getAttenteMoyenne(), 0.001);

        Statistiques amrani = stats.calculer(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 2);
        assertEquals(1, amrani.getConsultations());
        assertEquals(1, amrani.getNouveauxPatients());
        assertEquals(1, amrani.getRendezVous());
        assertEquals(40.0, amrani.getSalle().getAttenteMoyenne(), 0.001);
        assertNull(stats.calculer(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2), null).getSalle()
                .getAttenteMoyenne());
    }
}
