package My_application.dao.jdbc;

import static My_application.fake.FauxJdbc.ligne;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.Compte;
import My_application.entities.EntreeJournal;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.medicament;
import My_application.entities.patient;
import My_application.entities.symptome;
import My_application.fake.Donnees;
import My_application.fake.FauxJdbc;
import java.time.LocalTime;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

/** Vérifie le SQL produit par les DAO JDBC et la conversion des lignes en entités. */
public class JdbcDaoTest {

    private final FauxJdbc base = new FauxJdbc();
    private final JdbcDAOFactory daos = new JdbcDAOFactory(base);

    @After
    public void aucuneRessourceOubliee() {
        assertEquals("PreparedStatement non fermé", 0, base.statementsOuverts);
        assertEquals("ResultSet non fermé", 0, base.resultSetsOuverts);
    }

    @Test
    public void rechercheDePatientsParPrefixeParametre() {
        base.quand("FROM patient WHERE nomPatient LIKE", ligne("idPatient", 7, "nomPatient", "O'NEIL",
                "prenomPatient", "Sean", "date_Nais_Patient", java.sql.Date.valueOf("1990-01-02"), "sexePatient", 1,
                "telPatient", "0555"));
        List<patient> r = daos.getPatientDao().findByNomCommencantPar("O'N");
        assertEquals(1, r.size());
        patient p = r.get(0);
        assertEquals(7, p.getIdentPat());
        assertEquals("O'NEIL", p.getNom());
        assertTrue(p.isMasculin());
        assertEquals("0555", p.getTelephone());
        assertNull("colonne absente du résultat : pas d'erreur", p.getEmail());
        assertEquals("O'N%", base.derniere().param(1));
    }

    @Test
    public void ordonnancesDuPatientChargeesEnUneRequeteEtRegroupeesParConsultation() {
        base.quand("FROM prescrire p JOIN medicaments m",
                ligne("idConsultation", 5, "idMedicament", 10, "nomMedicament", "DOLIPRANE", "unite", 1,
                        "posologie", 3, "duree", 5, "mode", 2),
                ligne("idConsultation", 8, "idMedicament", 11, "nomMedicament", "AUGMENTIN", "unite", 1,
                        "posologie", 2, "duree", 7, "mode", 1),
                ligne("idConsultation", 5, "idMedicament", 12, "nomMedicament", "SPASFON", "unite", 2,
                        "posologie", 1, "duree", 3, "mode", 1));
        Map<Integer, List<medicament>> r = daos.getPrescriptionDao().findByPatient(3);
        assertEquals(1, base.executions.size());
        assertTrue(base.derniere().sql.contains("WHERE c.idPatient=?"));
        assertEquals(3, base.derniere().param(1));
        assertEquals(2, r.get(5).size());
        assertEquals("SPASFON", r.get(5).get(1).getDesignmed());
        assertEquals("AUGMENTIN", r.get(8).get(0).getDesignmed());
    }

    @Test
    public void symptomesDuPatientRegroupesParConsultation() {
        base.quand("FROM consultsymptome cs",
                ligne("idconsultation", 5, "idsymptome", 1, "designation", "Toux"),
                ligne("idconsultation", 6, "idsymptome", 2, "designation", "Fièvre"));
        Map<Integer, List<symptome>> r = daos.getConsultationDao().findSymptomesParPatient(3);
        assertEquals("Toux", r.get(5).get(0).getDesignation());
        assertEquals("Fièvre", r.get(6).get(0).getDesignation());
        assertNull(r.get(7));
    }

    @Test
    public void insertionPatientSansNumeroUtiliseLaCleGeneree() {
        patient p = Donnees.patient(0, "NOUVEAU");
        daos.getPatientDao().insert(p);
        assertTrue(p.getIdentPat() > 0);
        assertFalse(base.derniere().sql.contains("idPatient"));
    }

    @Test
    public void insertionPatientAvecNumeroSaisi() {
        daos.getPatientDao().insert(Donnees.patient(42, "DUPONT"));
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.startsWith("INSERT INTO patient (idPatient,"));
        assertEquals(42, e.param(1));
        assertEquals("DUPONT", e.param(2));
    }

    @Test
    public void insertionConsultationRecupereSonIdentifiantSansRequeteSupplementaire() {
        consultation c = new consultation(Donnees.patient(3, "X"), Donnees.medecin(1));
        c.setDate_cons(LocalDate.parse("2026-10-08"));
        c.setHeure_cons(LocalTime.parse("10:30:00"));
        daos.getConsultationDao().insert(c);
        assertTrue(c.getIdConsul() > 0);
        assertEquals(1, base.executions.size()); // plus de « select idConsultation where date=... »
        assertEquals(3, base.derniere().param(4));
        // Le domaine est en java.time ; JdbcHelper lie des types JDBC classiques.
        assertEquals(java.sql.Date.valueOf("2026-10-08"), base.derniere().param(1));
        assertEquals(java.sql.Time.valueOf("10:30:00"), base.derniere().param(2));
    }

    @Test
    public void constantesNonMesureesEcritesEnNull() {
        consultation c = new consultation(Donnees.patient(3, "X"), Donnees.medecin(1));
        c.setDate_cons(LocalDate.parse("2026-10-08"));
        c.setHeure_cons(LocalTime.parse("10:30:00"));
        c.setPoids(72.5);
        daos.getConsultationDao().insert(c);
        FauxJdbc.Execution e = base.derniere();
        assertNull("température", e.param(5));
        assertNull("pulsation", e.param(6));
        assertNull("pression vide", e.param(7));
        assertNull("respiration", e.param(8));
        assertEquals(72.5, e.param(9));
    }

    @Test
    public void constantesNullReluesCommeNonRenseignees() {
        base.quand("FROM consultation", ligne("idConsultation", 4, "dateConsultation",
                java.sql.Date.valueOf("2026-10-08"), "heureConsultation", java.sql.Time.valueOf("10:30:00"),
                "idMedecin", 1, "idPatient", 3, "temperature", null, "pulsation", 80, "pression", null,
                "respiration", null, "poids", 70.0));
        consultation c = daos.getConsultationDao().findByPatient(3).get(0);
        assertEquals(consultation.NON_RENSEIGNE, c.getTemperature(), 0);
        assertEquals(80, c.getPul_Cardiaque());
        assertEquals("", c.getPressionA());
        assertEquals(consultation.NON_RENSEIGNE, c.getTauxRespir());
        assertEquals(70.0, c.getPoids(), 0);
    }

    @Test
    public void symptomesInseresEnLot() {
        daos.getConsultationDao().ajouterSymptomes(9,
                Arrays.asList(new symptome(1, "Toux"), new symptome(2, "Fièvre")));
        assertEquals(2, base.executions.size());
        assertEquals(9, base.executions.get(1).param(1));
        assertEquals(2, base.executions.get(1).param(2));
    }

    @Test
    public void lignesDOrdonnanceRelues() {
        base.quand("FROM prescrire", ligne("idMedicament", 5, "nomMedicament", "AMOXICILLINE 1G", "unite", 1,
                "posologie", 2, "duree", 7, "mode", 1));
        List<medicament> l = daos.getPrescriptionDao().findByConsultation(9);
        assertEquals("AMOXICILLINE 1G", l.get(0).getDesignmed());
        assertEquals(5, l.get(0).getIdmed()); // l'ancien code ne relisait pas l'identifiant
        assertEquals(7, l.get(0).getDuree());
    }

    @Test
    public void annulerUnRendezVousCibleLePatient() {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(Donnees.patient(4, "Y"));
        r.setDate(LocalDate.of(2026, 10, 9));
        r.setHeure(LocalTime.parse("09:15:00"));
        daos.getRendezVousDao().delete(r);
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.contains("idPatient=?"));
        assertEquals(4, e.param(1));
    }

    @Test
    public void rendezVousLusAvecPatientEtUtilisateurEnUneRequete() {
        base.quand("FROM rendezvous", ligne("idUser", 1, "idPatient", 2, "date", java.sql.Date.valueOf("2026-10-09"),
                "heure", java.sql.Time.valueOf("09:15:00"),
                "prisLe", java.sql.Timestamp.valueOf("2026-10-01 08:00:00"), "etat", 0, "nomPatient", "B", "prenomPatient", "b",
                "date_Nais_Patient", java.sql.Date.valueOf("2000-01-01"), "sexePatient", 0, "nom", "SAHAB",
                "prenom", "Said", "qualite", "Médecin"));
        List<Rendez_Vous> l = daos.getRendezVousDao().findEntre(LocalDate.parse("2026-10-04"),
                LocalDate.parse("2026-10-10"));
        assertEquals(1, base.executions.size());
        assertEquals("B", l.get(0).getPatient().getNom());
        assertEquals("SAHAB", l.get(0).getUtilisateur().getNom());
        assertEquals(LocalDate.parse("2026-10-01"), l.get(0).getDateFix());
        assertEquals(LocalTime.parse("08:00:00"), l.get(0).getHeureFix());
    }

    @Test
    public void symptomeIntrouvable() {
        assertNull(daos.getSymptomeDao().findByDesignation("inconnu"));
    }

    // ----- comptes et journal des accès -----

    @Test
    public void lectureDUnCompteParSonIdentifiant() {
        base.quand("FROM utilisateur WHERE login=?", ligne("idUtilisateur", 3, "nom", "SAHAB", "prenom", "Said",
                "qualite", "Médecin", "login", "medecin", "actif", 1, "idMedecin", 2,
                "motDePasse", "pbkdf2-sha256$1$c2Vs$ZW1w"));
        Compte c = daos.getUtilisateurDao().findCompte("medecin");
        assertEquals(3, c.getUtilisateur().getIdent());
        assertEquals("medecin", c.getUtilisateur().getLogin());
        assertTrue(c.getUtilisateur().isActif());
        assertEquals(Integer.valueOf(2), c.getUtilisateur().getIdMedecin());
        assertEquals("pbkdf2-sha256$1$c2Vs$ZW1w", c.getEmpreinte());
        assertEquals("medecin", base.derniere().param(1));
    }

    @Test
    public void leRoleDuCompteEstLu() {
        base.quand("FROM utilisateur WHERE idUtilisateur=?", ligne("idUtilisateur", 4, "nom", "BENALI",
                "prenom", "Amina", "qualite", null, "role", "SECRETAIRE", "login", "amina", "actif", 1,
                "idMedecin", null));
        assertEquals(Role.SECRETAIRE, daos.getUtilisateurDao().findById(4).getRole());
        assertTrue(base.derniere().sql.contains("role"));
    }

    @Test
    public void ajoutDUnMembreEtChangementDeRole() {
        User u = new User(0, "BENALI", "Amina", "Secrétaire");
        u.setRole(Role.SECRETAIRE);
        assertEquals(1001, daos.getUtilisateurDao().insert(u).getIdent());
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.startsWith("INSERT INTO utilisateur"));
        assertEquals("SECRETAIRE", e.param(4));
        assertNull(e.param(6));

        daos.getUtilisateurDao().definirRole(4, Role.MEDECIN, 2);
        e = base.derniere();
        assertEquals("UPDATE utilisateur SET role=?, idMedecin=? WHERE idUtilisateur=?", e.sql);
        assertEquals("MEDECIN", e.param(1));
        assertEquals(2, e.param(2));

        LocalDateTime t = LocalDateTime.of(2026, 10, 22, 8, 15);
        daos.getUtilisateurDao().noterConnexion(4, t);
        e = base.derniere();
        assertTrue(e.sql.contains("derniereConnexion"));
        assertEquals(Timestamp.valueOf(t), e.param(1));
    }

    @Test
    public void lesIdentifiantsSontEcritsEnParametres() {
        daos.getUtilisateurDao().definirIdentifiants(3, "medecin", "pbkdf2-sha256$1$a$b");
        FauxJdbc.Execution e = base.derniere();
        assertEquals("UPDATE utilisateur SET login=?, motDePasse=? WHERE idUtilisateur=?", e.sql);
        assertEquals("medecin", e.param(1));
        assertEquals("pbkdf2-sha256$1$a$b", e.param(2));
        assertEquals(3, e.param(3));
    }

    @Test
    public void ajoutAuJournalDesAcces() {
        LocalDateTime t = LocalDateTime.of(2026, 10, 21, 9, 30);
        daos.getJournalDao().ajouter(new EntreeJournal(t, 1, "OUVERTURE_DOSSIER", 7, null, "POSTE-1"));
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.startsWith("INSERT INTO journal_acces"));
        assertEquals(Timestamp.valueOf(t), e.param(1));
        assertEquals(1, e.param(2));
        assertEquals("OUVERTURE_DOSSIER", e.param(3));
        assertEquals(7, e.param(4));
        assertNull(e.param(5));
        assertEquals("POSTE-1", e.param(6));
    }

    @Test
    public void lectureDuJournalDUnPatient() {
        base.quand("FROM journal_acces WHERE idPatient=?", ligne("dateHeure", Timestamp.valueOf("2026-10-21 09:30:00"),
                "idUtilisateur", null, "action", "ECHEC_CONNEXION", "idPatient", 7, "detail", "x", "poste", null));
        List<EntreeJournal> r = daos.getJournalDao().findByPatient(7);
        assertEquals(1, r.size());
        assertNull(r.get(0).getIdUtilisateur());
        assertEquals(LocalDateTime.of(2026, 10, 21, 9, 30), r.get(0).getDateHeure());
    }

    @Test
    public void listeDesMedecinsTrieeParNom() {
        base.quand("FROM medecin ORDER BY nomMedecin",
                ligne("idMedecin", 2, "nomMedecin", "AMRANI", "prenomMedecin", "Lila", "titre", "Dr"),
                ligne("idMedecin", 1, "nomMedecin", "SAHAB", "prenomMedecin", "Said", "titre", "Dr"));
        List<medecin> r = daos.getMedecinDao().findAll();
        assertEquals(2, r.size());
        assertEquals(2, r.get(0).getIdmedecin());
        assertEquals("Dr AMRANI Lila", r.get(0).getNomComplet());
        assertEquals(0, base.derniere().parametres.size());
    }
}
