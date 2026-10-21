package My_application.dao.jdbc;

import static My_application.fake.FauxJdbc.ligne;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.Compte;
import My_application.entities.EntreeJournal;
import My_application.entities.Rendez_Vous;
import My_application.entities.consultation;
import My_application.entities.medicament;
import My_application.entities.patient;
import My_application.entities.symptome;
import My_application.fake.Donnees;
import My_application.fake.FauxJdbc;
import java.sql.Date;
import java.sql.Time;
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
                "prenomPatient", "Sean", "date_Nais_Patient", Date.valueOf("1990-01-02"), "sexePatient", 1,
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
                ligne("idConsul", 5, "idMedicament", 10, "nomMedicament", "DOLIPRANE", "unite", 1,
                        "posologie", 3, "duree", 5, "mode", 2),
                ligne("idConsul", 8, "idMedicament", 11, "nomMedicament", "AUGMENTIN", "unite", 1,
                        "posologie", 2, "duree", 7, "mode", 1),
                ligne("idConsul", 5, "idMedicament", 12, "nomMedicament", "SPASFON", "unite", 2,
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
        c.setDate_cons(Date.valueOf("2026-10-08"));
        c.setHeure_cons(Time.valueOf("10:30:00"));
        daos.getConsultationDao().insert(c);
        assertTrue(c.getIdConsul() > 0);
        assertEquals(1, base.executions.size()); // plus de « select idConsultation where date=... »
        assertEquals(3, base.derniere().param(4));
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
        r.setDate(Date.valueOf(LocalDate.of(2026, 10, 9)));
        r.setHeure(Time.valueOf("09:15:00"));
        daos.getRendezVousDao().delete(r);
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.contains("idPatient=?"));
        assertEquals(4, e.param(1));
    }

    @Test
    public void rendezVousLusAvecPatientEtUtilisateurEnUneRequete() {
        base.quand("FROM rondezvous", ligne("idUser", 1, "idPatient", 2, "date", Date.valueOf("2026-10-09"),
                "heure", Time.valueOf("09:15:00"), "dateFix", Date.valueOf("2026-10-01"),
                "heureFix", Time.valueOf("08:00:00"), "etat", 0, "nomPatient", "B", "prenomPatient", "b",
                "date_Nais_Patient", Date.valueOf("2000-01-01"), "sexePatient", 0, "nom", "SAHAB",
                "prenom", "Said", "qualite", "Médecin"));
        List<Rendez_Vous> l = daos.getRendezVousDao().findEntre(Date.valueOf("2026-10-04"), Date.valueOf("2026-10-10"));
        assertEquals(1, base.executions.size());
        assertEquals("B", l.get(0).getPatient().getNom());
        assertEquals("SAHAB", l.get(0).getUtilsateur().getNom());
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
}
