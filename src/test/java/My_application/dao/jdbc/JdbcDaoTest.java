package My_application.dao.jdbc;

import static My_application.fake.FauxJdbc.ligne;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.dao.MessageDao;
import My_application.entities.Compte;
import My_application.entities.EntreeJournal;
import My_application.entities.MessageInterne;
import My_application.entities.Passage;
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
import java.util.ArrayList;
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
    public void unRendezVousNEstPlusSupprimeMaisChangeDEtat() {
        assertTrue(daos.getRendezVousDao().changerEtat(12, Rendez_Vous.ETAT_PREVU, Rendez_Vous.ETAT_ANNULE));
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.startsWith("UPDATE rendezvous SET etat=?"));
        assertTrue("l'état attendu protège d'une modification concurrente", e.sql.endsWith("AND etat=?"));
        assertEquals(Rendez_Vous.ETAT_ANNULE, e.param(1));
        assertEquals(12, e.param(2));
        assertEquals(Rendez_Vous.ETAT_PREVU, e.param(3));
    }

    @Test
    public void deplacerEtInsererUnRendezVous() {
        assertTrue(daos.getRendezVousDao().deplacer(12, LocalDate.of(2026, 10, 9), LocalTime.of(11, 15), 2));
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.contains("SET date=?, heure=?, idMedecin=?"));
        assertEquals(2, e.param(3));
        assertEquals(12, e.param(4));
        assertEquals(Rendez_Vous.ETAT_PREVU, e.param(5));

        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(Donnees.patient(4, "Y"));
        r.setUtilisateur(Donnees.utilisateur(1));
        r.setIdMedecin(1);
        r.setDate(LocalDate.of(2026, 10, 9));
        r.setHeure(LocalTime.of(9, 15));
        r.setMotif("Contrôle");
        daos.getRendezVousDao().insert(r);
        assertEquals("clé générée par le faux pilote (après la mise à jour)", Integer.valueOf(1002),
                r.getIdRendezVous());
        assertEquals("Contrôle", base.derniere().param(6));
    }

    @Test
    public void rendezVousDePlusieursPatients() {
        base.quand("IN (?,?)", ligne("idRendezVous", 7, "idUser", 1, "idPatient", 2, "idMedecin", 1,
                "date", java.sql.Date.valueOf("2026-10-09"), "heure", java.sql.Time.valueOf("09:15:00"),
                "motif", "Résultats", "prisLe", null, "etat", 4, "nomPatient", "B", "prenomPatient", "b",
                "date_Nais_Patient", java.sql.Date.valueOf("2000-01-01"), "sexePatient", 0, "telPatient", "0555123456",
                "nom", "SAHAB", "prenom", "Said", "qualite", "Médecin"));
        List<Rendez_Vous> l = daos.getRendezVousDao().findDesPatients(Arrays.asList(2, 3));
        assertEquals(1, l.size());
        assertEquals(Integer.valueOf(7), l.get(0).getIdRendezVous());
        assertEquals("Résultats", l.get(0).getMotif());
        assertEquals(Rendez_Vous.ETAT_ANNULE, l.get(0).getEtat());
        assertEquals("0555123456", l.get(0).getPatient().getTelephone());
        assertTrue(base.derniere().sql.contains("ORDER BY r.date DESC"));
        assertTrue(daos.getRendezVousDao().findDesPatients(new ArrayList<Integer>()).isEmpty());
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

    // ----- salle d'attente et événements -----

    @Test
    public void unPassageEstInsereEtLuAvecLaFicheEtLHeureDuRendezVous() {
        Passage p = new Passage();
        p.setPatient(Donnees.patient(4, "Y"));
        p.setIdMedecin(2);
        p.setIdRendezVous(12);
        p.setJour(LocalDate.of(2026, 10, 9));
        p.setArriveeLe(LocalDateTime.of(2026, 10, 9, 8, 50));
        p.setUrgence(true);
        daos.getSalleAttenteDao().insert(p);
        assertEquals(Integer.valueOf(1001), p.getIdPassage());
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.startsWith("INSERT INTO salle_attente"));
        assertEquals(4, e.param(1));
        assertEquals(12, e.param(3));
        assertEquals(1, e.param(6));
        assertEquals(Passage.EN_ATTENTE, e.param(7));

        base.quand("FROM salle_attente", ligne("idPassage", 3, "idPatient", 4, "idMedecin", 2, "idRendezVous", null,
                "jour", java.sql.Date.valueOf("2026-10-09"), "arriveeLe", Timestamp.valueOf("2026-10-09 08:50:00"),
                "appeleLe", null, "sortieLe", null, "priorite", 0, "etat", 1, "heure", null,
                "nomPatient", "Y", "prenomPatient", "y", "date_Nais_Patient", java.sql.Date.valueOf("2000-01-01"),
                "sexePatient", 1, "villePatient", "Tizi-Ouzou"));
        List<Passage> l = daos.getSalleAttenteDao().findDuJour(LocalDate.of(2026, 10, 9));
        assertEquals(1, l.size());
        Passage lu = l.get(0);
        assertTrue(lu.isSansRendezVous());
        assertNull(lu.getHeurePrevue());
        assertNull(lu.getAppeleLe());
        assertFalse(lu.isUrgence());
        assertEquals(Passage.EN_CONSULTATION, lu.getEtat());
        assertEquals(LocalTime.of(8, 50), lu.getArriveeLe().toLocalTime());
        assertEquals("fiche complète pour ouvrir le dossier", "Tizi-Ouzou", lu.getPatient().getVille());
        assertTrue(base.derniere().sql.contains("LEFT JOIN rendezvous"));
    }

    @Test
    public void unPassageNeChangeQueSIlEstToujoursDansLEtatAttendu() {
        Passage p = new Passage();
        p.setIdPassage(3);
        p.setEtat(Passage.EN_CONSULTATION);
        p.setAppeleLe(LocalDateTime.of(2026, 10, 9, 9, 5));
        assertTrue(daos.getSalleAttenteDao().mettreAJour(p, Passage.EN_ATTENTE));
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.endsWith("WHERE idPassage=? AND etat=?"));
        assertEquals(Passage.EN_CONSULTATION, e.param(1));
        assertNull(e.param(3));
        assertEquals(0, e.param(4));
        assertEquals(3, e.param(5));
        assertEquals(Passage.EN_ATTENTE, e.param(6));
    }

    @Test
    public void evenementsPubliesLusEtPurges() {
        daos.getEvenementDao().publier("SALLE", 3);
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.startsWith("INSERT INTO evenement"));
        assertEquals("SALLE", e.param(1));
        assertEquals(3, e.param(2));

        base.quand("FROM evenement", ligne("dernier", 42L));
        assertEquals(42L, daos.getEvenementDao().dernier());
        daos.getEvenementDao().purgerAvant(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertTrue(base.derniere().sql.startsWith("DELETE FROM evenement WHERE creeLe < ?"));
    }

    @Test
    public void sansEvenementLeDernierVautZero() {
        assertEquals(0L, daos.getEvenementDao().dernier());
    }

    // ----- messagerie et présence -----

    @Test
    public void unMessageEstEnregistreAvecSonPatient() {
        MessageInterne m = new MessageInterne();
        m.setIdExpediteur(3);
        m.setTexte("Résultats arrivés");
        m.setPatient(Donnees.patient(7, "Y"));
        m.setEnvoyeLe(LocalDateTime.of(2026, 10, 22, 9, 30));
        daos.getMessageDao().insert(m);
        assertEquals(Integer.valueOf(1001), m.getIdMessage());
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.startsWith("INSERT INTO message_interne"));
        assertEquals(3, e.param(1));
        assertNull("pour tout le cabinet", e.param(2));
        assertEquals(7, e.param(4));
    }

    @Test
    public void unFilPriveLitLesDeuxSensDuPlusAncienAuPlusRecent() {
        base.quand("FROM message_interne m",
                ligne("idMessage", 9, "idExpediteur", 1, "idDestinataire", 3, "texte", "réponse",
                        "idPatient", null, "envoyeLe", Timestamp.valueOf("2026-10-22 09:31:00"),
                        "nom", "SAHAB", "prenom", "Said", "nomPatient", null, "prenomPatient", null, "lu", 0),
                ligne("idMessage", 8, "idExpediteur", 3, "idDestinataire", 1, "texte", "question",
                        "idPatient", 7, "envoyeLe", Timestamp.valueOf("2026-10-22 09:30:00"),
                        "nom", "BELKACEM", "prenom", "Lila", "nomPatient", "Y", "prenomPatient", "y", "lu", 1));
        List<MessageInterne> l = daos.getMessageDao().conversation(3, 1, 200);
        assertEquals(Integer.valueOf(8), l.get(0).getIdMessage());
        assertEquals("BELKACEM Lila", l.get(0).getNomExpediteur());
        assertEquals(7, l.get(0).getPatient().getIdentPat());
        assertTrue(l.get(0).isLuParDestinataire());
        assertNull(l.get(1).getPatient());
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.contains("ORDER BY m.idMessage DESC LIMIT ?"));
        assertEquals(3, e.param(1));
        assertEquals(1, e.param(2));
        assertEquals(1, e.param(3));
        assertEquals(3, e.param(4));
        assertEquals(200, e.param(5));

        daos.getMessageDao().conversation(3, null, 50);
        assertTrue(base.derniere().sql.contains("WHERE m.idDestinataire IS NULL"));
        assertEquals(50, base.derniere().param(1));
    }

    @Test
    public void nonLusParFilEtMarquageDesLus() {
        base.quand("GROUP BY fil", ligne("fil", 0, "n", 2), ligne("fil", 1, "n", 1));
        Map<Integer, Integer> n = daos.getMessageDao().nonLus(3);
        assertEquals(Integer.valueOf(2), n.get(MessageDao.TOUT_LE_CABINET));
        assertEquals(Integer.valueOf(1), n.get(1));
        assertEquals(3, base.derniere().param(1));

        daos.getMessageDao().marquerLus(3, 1, LocalDateTime.of(2026, 10, 22, 9, 40));
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.startsWith("INSERT INTO message_lu"));
        assertTrue(e.sql.contains("m.idExpediteur=? AND m.idDestinataire=?"));
        assertEquals(3, e.param(1));
        assertEquals(3, e.param(3));
        assertEquals(1, e.param(4));
        assertEquals(3, e.param(5));
        assertEquals(3, e.param(6));
        daos.getMessageDao().marquerLus(3, null, LocalDateTime.of(2026, 10, 22, 9, 40));
        assertTrue(base.derniere().sql.contains("m.idDestinataire IS NULL"));
    }

    @Test
    public void dernierMessagePurgeEtPresence() {
        assertEquals(0L, daos.getMessageDao().dernier());
        base.quand("MAX(idMessage)", ligne("dernier", 12L));
        assertEquals(12L, daos.getMessageDao().dernier());
        daos.getMessageDao().purgerAvant(LocalDateTime.of(2025, 10, 22, 0, 0));
        assertTrue(base.derniere().sql.startsWith("DELETE FROM message_interne WHERE envoyeLe < ?"));

        daos.getPresenceDao().signaler(3, "ACCUEIL");
        assertTrue(base.derniere().sql.contains("ON DUPLICATE KEY UPDATE"));
        assertEquals("ACCUEIL", base.derniere().param(2));
        daos.getPresenceDao().quitter(3);
        assertTrue(base.derniere().sql.startsWith("DELETE FROM utilisateur_presence"));
        base.quand("FROM utilisateur_presence", ligne("idUtilisateur", 2));
        assertTrue(daos.getPresenceDao().enLigne(60).contains(2));
        assertEquals(60, base.derniere().param(1));
    }

    @Test
    public void symptomeIntrouvable() {
        assertNull(daos.getSymptomeDao().findByDesignation("inconnu"));
    }

    // ----- comptes et journal des accès -----

    @Test
    public void lectureDUnCompteParSonIdentifiant() {
        base.quand("FROM utilisateur WHERE login=?", ligne("idUtilisateur", 3, "nom", "SAHAB", "prenom", "Said",
                "qualite", "Médecin", "login", "medecin", "actif", 1, "idMedecin", 2, "doitChanger", 1,
                "derniereConnexion", Timestamp.valueOf(LocalDateTime.of(2026, 10, 21, 18, 5)),
                "motDePasse", "pbkdf2-sha256$1$c2Vs$ZW1w"));
        Compte c = daos.getUtilisateurDao().findCompte("medecin");
        assertTrue(c.getUtilisateur().isDoitChanger());
        assertEquals(LocalDateTime.of(2026, 10, 21, 18, 5), c.getUtilisateur().getDerniereConnexion());
        assertEquals(3, c.getUtilisateur().getIdent());
        assertEquals("medecin", c.getUtilisateur().getLogin());
        assertTrue(c.getUtilisateur().isActif());
        assertEquals(Integer.valueOf(2), c.getUtilisateur().getIdMedecin());
        assertEquals("pbkdf2-sha256$1$c2Vs$ZW1w", c.getEmpreinte());
        assertEquals("medecin", base.derniere().param(1));
    }

    @Test
    public void rechercheParTelephoneEtDoublons() {
        daos.getPatientDao().findByTelephoneCommencantPar("0661");
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.contains("telPatient LIKE ?"));
        assertEquals("0661%", e.param(1));

        LocalDate naissance = LocalDate.of(1980, 5, 17);
        daos.getPatientDao().findDoublons("AMRANI", naissance, "0555123456");
        e = base.derniere();
        assertTrue(e.sql.contains("OR telPatient=?"));
        assertEquals("AMRANI", e.param(1));
        assertEquals(java.sql.Date.valueOf(naissance), e.param(2));
        assertEquals("0555123456", e.param(3));

        daos.getPatientDao().findDoublons("AMRANI", naissance, null);
        assertFalse(base.derniere().sql.contains("telPatient"));
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
        assertEquals("UPDATE utilisateur SET login=?, motDePasse=?, doitChanger=0 WHERE idUtilisateur=?", e.sql);
        assertEquals("medecin", e.param(1));
        assertEquals("pbkdf2-sha256$1$a$b", e.param(2));
        assertEquals(3, e.param(3));

        daos.getUtilisateurDao().definirMotDePasse(3, "pbkdf2-sha256$1$c$d", true);
        e = base.derniere();
        assertEquals("UPDATE utilisateur SET motDePasse=?, doitChanger=? WHERE idUtilisateur=?", e.sql);
        assertEquals("pbkdf2-sha256$1$c$d", e.param(1));
        assertEquals(true, e.param(2));
        assertEquals(3, e.param(3));

        User u = new User(3, "AMRANI", "Lina", "Médecin");
        daos.getUtilisateurDao().modifier(u);
        e = base.derniere();
        assertEquals("UPDATE utilisateur SET nom=?, prenom=?, qualite=? WHERE idUtilisateur=?", e.sql);
        assertEquals("AMRANI", e.param(1));
        assertEquals(3, e.param(4));
    }

    @Test
    public void fichesMedecinCreeesModifieesEtDesactivees() {
        medecin m = Donnees.medecin(0);
        m.setOrdmedcin("16/123");
        daos.getMedecinDao().insert(m);
        FauxJdbc.Execution e = base.derniere();
        assertTrue(e.sql.startsWith("INSERT INTO medecin (ordreMedecin, nomMedecin"));
        assertEquals("16/123", e.param(1));
        assertEquals(true, e.param(11));
        assertEquals(1001, m.getIdmedecin());

        m.setSpecialite("Pédiatrie");
        daos.getMedecinDao().modifier(m);
        e = base.derniere();
        assertTrue(e.sql.startsWith("UPDATE medecin SET ordreMedecin=?"));
        assertFalse(e.sql.contains("actif"));
        assertEquals("Pédiatrie", e.param(5));
        assertEquals(1001, e.param(11));

        daos.getMedecinDao().definirActif(1001, false);
        e = base.derniere();
        assertEquals("UPDATE medecin SET actif=? WHERE idMedecin=?", e.sql);
        assertEquals(false, e.param(1));

        base.quand("FROM medecin ORDER BY", ligne("idMedecin", 4, "ordreMedecin", null, "nomMedecin", "SAHAB",
                "prenomMedecin", "Said", "titre", "Dr", "specialite", null, "adresse", null, "telephone1", null,
                "telephone2", null, "email", null, "nomCabinet", null, "actif", 0));
        assertFalse(daos.getMedecinDao().findAll().get(0).isActif());

        base.quand("COUNT(*) AS n FROM rendezvous", ligne("n", 2L));
        assertEquals(2, daos.getRendezVousDao().compterPrevus(1001, LocalDate.of(2026, 10, 22)));
        e = base.derniere();
        assertEquals(1001, e.param(1));
        assertEquals(Rendez_Vous.ETAT_PREVU, e.param(2));
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
