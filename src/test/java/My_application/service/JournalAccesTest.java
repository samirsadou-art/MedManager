package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.dao.DataAccessException;
import My_application.entities.DossierPatient;
import My_application.entities.EntreeJournal;
import My_application.entities.Rendez_Vous;
import My_application.entities.consultation;
import My_application.entities.patient;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;

/** Les accès au dossier médical sont tracés au nom de l'utilisateur connecté. */
public class JournalAccesTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 21, 9, 30, 15);

    private MemoireDAOFactory base;
    private Session session;
    private Clock horloge;
    private JournalAcces journal;

    @Before
    public void preparer() {
        base = Donnees.base();
        session = new Session(base, 1, 1);
        session.ouvrir(Donnees.utilisateur(1));
        ZoneId zone = ZoneId.systemDefault();
        horloge = Clock.fixed(MAINTENANT.atZone(zone).toInstant(), zone);
        journal = new JournalAccesEnBase(base.getJournalDao(), session, horloge, "POSTE-1");
    }

    private EntreeJournal derniere() {
        return base.journal.get(base.journal.size() - 1);
    }

    @Test
    public void uneEntreeContientLUtilisateurLaDateEtLePoste() {
        journal.noter(JournalAcces.Action.OUVERTURE_DOSSIER, 2, null);
        EntreeJournal e = derniere();
        assertEquals(Integer.valueOf(1), e.getIdUtilisateur());
        assertEquals("OUVERTURE_DOSSIER", e.getAction());
        assertEquals(Integer.valueOf(2), e.getIdPatient());
        assertEquals(MAINTENANT, e.getDateHeure());
        assertEquals("POSTE-1", e.getPoste());
    }

    @Test
    public void leDetailEstTronque() {
        StringBuilder long_ = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            long_.append('x');
        }
        journal.noter(JournalAcces.Action.CONNEXION, null, long_.toString());
        assertEquals(JournalAccesEnBase.LONGUEUR_DETAIL, derniere().getDetail().length());
    }

    @Test
    public void sansSessionLEntreeNEstAttribueeAPersonne() {
        new JournalAccesEnBase(base.getJournalDao(), new Session(base, 1, 1), horloge, null)
                .noter(JournalAcces.Action.ECHEC_CONNEXION, null, "identifiant : x");
        assertNull(derniere().getIdUtilisateur());
    }

    @Test
    public void ouvrirUnDossierEstTrace() {
        new DossierService(base, journal).ouvrirDossier(base.patients.get(2), base.medecins.get(1));
        assertEquals("OUVERTURE_DOSSIER", derniere().getAction());
        assertEquals(Integer.valueOf(2), derniere().getIdPatient());
    }

    @Test
    public void unDossierNeSOuvrePasSiLaTraceEchoue() {
        base.echouerSur("journal.ajouter");
        try {
            new DossierService(base, journal).ouvrirDossier(base.patients.get(2), base.medecins.get(1));
            fail();
        } catch (DataAccessException attendu) {
            assertTrue(base.journal.isEmpty());
        }
    }

    @Test
    public void enregistrerUneConsultationEstTraceDansLaMemeTransaction() {
        DossierService service = new DossierService(base, journal);
        DossierPatient d = service.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        consultation c = service.nouvelleConsultation(d);
        c.setTemperature(37.5);
        service.enregistrer(d);
        assertEquals("ENREGISTREMENT_CONSULTATION", derniere().getAction());
        assertEquals("consultation n° " + c.getIdConsul(), derniere().getDetail());
    }

    @Test
    public void siLaTraceEchoueLaConsultationNEstPasEnregistree() {
        DossierService service = new DossierService(base, journal);
        DossierPatient d = service.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        service.nouvelleConsultation(d);
        int entrees = base.journal.size();
        base.echouerSur("journal.ajouter");
        try {
            service.enregistrer(d);
            fail();
        } catch (DataAccessException attendu) {
            assertTrue(base.consultations.isEmpty());
            assertEquals(entrees, base.journal.size());
            assertEquals(1, base.rollbacks);
        }
    }

    @Test
    public void creerUnPatientEstTrace() {
        patient p = new PatientService(base, journal).creer(
                Donnees.patient(0, "NOUVEAU", "Ali", LocalDate.of(1980, 1, 1), true));
        assertEquals("CREATION_PATIENT", derniere().getAction());
        assertEquals(Integer.valueOf(p.getIdentPat()), derniere().getIdPatient());
    }

    @Test
    public void prendreEtAnnulerUnRendezVousEstTrace() {
        RendezVousService rdv = new RendezVousService(base, horloge, 8, 9, 4, journal);
        LocalDate demain = MAINTENANT.toLocalDate().plusDays(1);
        Rendez_Vous r = rdv.proposer(base.patients.get(1), Donnees.utilisateur(1), demain, 10);
        rdv.confirmer(r);
        assertEquals("RENDEZ_VOUS_PRIS", derniere().getAction());
        assertEquals(demain + " 10:00:00", derniere().getDetail());
        rdv.annuler(r, base.patients.get(1));
        assertEquals("RENDEZ_VOUS_ANNULE", derniere().getAction());
        assertEquals(Integer.valueOf(1), derniere().getIdPatient());
    }
}
