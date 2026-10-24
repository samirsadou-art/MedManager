package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.entities.EntreeJournal;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.entities.medecin;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;

/** Gestion des comptes et des fiches médecin depuis la page Membres (administrateur). */
public class MembresServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-10-22T08:00:00Z"), ZoneOffset.UTC);

    private MemoireDAOFactory base;
    private Session session;
    private MembresService membres;
    private User admin;

    @Before
    public void preparer() {
        base = Donnees.base();
        session = new Session(base, 1, 1);
        membres = new MembresService(base, new JournalAccesEnBase(base.getJournalDao(), session, HORLOGE, "POSTE"),
                session, HORLOGE);
        admin = Donnees.utilisateur(9);
        admin.setRole(Role.ADMIN);
        base.utilisateurs.put(9, admin);
        session.ouvrir(admin);
    }

    private static char[] mdp(String s) {
        return s.toCharArray();
    }

    private static void refuse(Runnable action, String fragment) {
        try {
            action.run();
            fail("action permise à tort");
        } catch (MetierException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(fragment));
        }
    }

    private EntreeJournal dernierJournal() {
        return base.journal.get(base.journal.size() - 1);
    }

    private User secretaire() {
        return membres.creerCompte("benali", "amina", Role.SECRETAIRE, null, null, "amina",
                mdp("Provisoire1"), mdp("Provisoire1"));
    }

    // ----- création de comptes -----

    @Test
    public void unCompteCreeAUnMotDePasseProvisoire() {
        User u = secretaire();
        assertEquals("BENALI", u.getNom());
        assertEquals("Amina", u.getPrenom());
        assertEquals("amina", base.logins.get(u.getIdent()));
        assertTrue(MotDePasse.verifier(mdp("Provisoire1"), base.empreintes.get(u.getIdent())));
        assertTrue(base.aChanger.contains(u.getIdent()));
        assertNull(u.getIdMedecin());
        assertEquals("CREATION_COMPTE", dernierJournal().getAction());
        assertTrue(dernierJournal().getDetail().contains("amina"));
    }

    @Test
    public void unNouveauMedecinRecoitSaFicheDansLaMemeTransaction() {
        medecin fiche = new medecin();
        fiche.setNom(" amrani ");
        fiche.setPrenom("lina");
        fiche.setOrdmedcin(" 15/987 ");
        fiche.setSpecialite("  ");
        User u = membres.creerCompte("amrani", "lina", Role.MEDECIN, null, fiche, "dr.amrani",
                mdp("Provisoire1"), mdp("Provisoire1"));
        medecin cree = base.medecins.get(u.getIdMedecin());
        assertEquals("AMRANI", cree.getNom());
        assertEquals("Dr", cree.getTitre());
        assertEquals("15/987", cree.getOrdmedcin());
        assertNull(cree.getSpecialite());
        assertTrue(cree.isActif());
        assertEquals(Role.MEDECIN, base.utilisateurs.get(u.getIdent()).getRole());
    }

    @Test
    public void unMedecinPeutEtreRattacheAUneFicheExistante() {
        User u = membres.creerCompte("sahab", "said", Role.MEDECIN, 1, null, "sahab",
                mdp("Provisoire1"), mdp("Provisoire1"));
        assertEquals(Integer.valueOf(1), u.getIdMedecin());
        assertEquals(1, base.medecins.size());
    }

    @Test
    public void creationRefuseeAvecUnMessageClair() {
        secretaire();
        refuse(() -> membres.creerCompte("x", "y", Role.SECRETAIRE, null, null, "Amina",
                mdp("Provisoire1"), mdp("Provisoire1")), "déjà utilisé");
        refuse(() -> membres.creerCompte("x", "y", Role.SECRETAIRE, null, null, "a b",
                mdp("Provisoire1"), mdp("Provisoire1")), "identifiant");
        refuse(() -> membres.creerCompte("x", "y", Role.SECRETAIRE, null, null, "autre",
                mdp("court"), mdp("court")), "8");
        refuse(() -> membres.creerCompte("x", "y", Role.SECRETAIRE, null, null, "autre",
                mdp("Provisoire1"), mdp("Provisoire2")), "différents");
        refuse(() -> membres.creerCompte("x", "y", Role.MEDECIN, null, null, "autre",
                mdp("Provisoire1"), mdp("Provisoire1")), "fiche médecin");
        medecin sansNom = new medecin();
        refuse(() -> membres.creerCompte("x", "y", Role.MEDECIN, null, sansNom, "autre",
                mdp("Provisoire1"), mdp("Provisoire1")), "obligatoires");
        medecin memeOrdre = Donnees.medecin(0);
        refuse(() -> membres.creerCompte("x", "y", Role.MEDECIN, null, memeOrdre, "autre",
                mdp("Provisoire1"), mdp("Provisoire1")), "numéro d'ordre");
        assertEquals(1, base.medecins.size());
    }

    @Test
    public void reserveAuxAdministrateurs() {
        User medecin = Donnees.utilisateur(1);
        session.ouvrir(medecin);
        refuse(() -> secretaire(), "administrateur");
        refuse(() -> membres.medecins(), "administrateur");
        refuse(() -> membres.definirMedecinActif(1, false), "administrateur");
    }

    // ----- modification -----

    @Test
    public void modifierLeNomEtLeRole() {
        User u = secretaire();
        membres.modifier(u.getIdent(), "benali-saidi", "amina", Role.MEDECIN, 1);
        User lu = base.utilisateurs.get(u.getIdent());
        assertEquals("BENALI-SAIDI", lu.getNom());
        assertEquals(Role.MEDECIN, lu.getRole());
        assertEquals(Integer.valueOf(1), lu.getIdMedecin());
        assertEquals("MODIFICATION_COMPTE", dernierJournal().getAction());

        refuse(() -> membres.modifier(9, "admin", "seul", Role.SECRETAIRE, null), "dernier");
        refuse(() -> membres.modifier(77, "x", "y", Role.SECRETAIRE, null), "introuvable");
        refuse(() -> membres.modifier(u.getIdent(), " ", "y", Role.SECRETAIRE, null), "nom");
    }

    @Test
    public void unMedecinNeSeRattachePasAUneFicheDesactivee() {
        User u = secretaire();
        base.medecins.get(1).setActif(false);
        refuse(() -> membres.modifier(u.getIdent(), "benali", "amina", Role.MEDECIN, 1), "désactivée");
    }

    // ----- mot de passe provisoire et activation -----

    @Test
    public void motDePasseProvisoireApresUnOubli() {
        User u = secretaire();
        base.aChanger.clear();
        membres.motDePasseProvisoire(u.getIdent(), mdp("Nouveau2026"), mdp("Nouveau2026"));
        assertTrue(base.aChanger.contains(u.getIdent()));
        assertTrue(MotDePasse.verifier(mdp("Nouveau2026"), base.empreintes.get(u.getIdent())));
        assertTrue(dernierJournal().getDetail().contains("provisoire"));

        refuse(() -> membres.motDePasseProvisoire(1, mdp("Nouveau2026"), mdp("Nouveau2026")), "identifiant");
        refuse(() -> membres.motDePasseProvisoire(u.getIdent(), mdp("abc"), mdp("abc")), "8");
    }

    @Test
    public void activerEtDesactiverUnCompte() {
        User u = secretaire();
        membres.definirActif(u.getIdent(), false);
        assertTrue(base.inactifs.contains(u.getIdent()));
        membres.definirActif(u.getIdent(), true);
        assertFalse(base.inactifs.contains(u.getIdent()));
        refuse(() -> membres.definirActif(9, false), "dernier");
    }

    @Test
    public void unCompteMedecinNeSeReactivePasSurUneFicheDesactivee() {
        base.utilisateurs.get(1).setRole(Role.MEDECIN);
        base.utilisateurs.get(1).setIdMedecin(1);
        base.inactifs.add(1);
        base.medecins.get(1).setActif(false);
        refuse(() -> membres.definirActif(1, true), "désactivée");
    }

    // ----- fiches médecin -----

    @Test
    public void modifierUneFicheMedecin() {
        medecin m = Donnees.medecin(1);
        m.setSpecialite("Pédiatrie");
        m.setTitre("");
        membres.modifierMedecin(m);
        assertEquals("Pédiatrie", base.medecins.get(1).getSpecialite());
        assertEquals("Dr", base.medecins.get(1).getTitre());

        medecin autre = Donnees.medecin(2);
        autre.setOrdmedcin("999");
        base.medecins.put(2, autre);
        medecin doublon = Donnees.medecin(2);
        refuse(() -> membres.modifierMedecin(doublon), "numéro d'ordre");
        refuse(() -> membres.modifierMedecin(Donnees.medecin(42)), "introuvable");
    }

    @Test
    public void unMedecinQuiPartEstDesactiveSeulementSansRendezVousNiCompteActif() {
        RendezVousService rdv = new RendezVousService(base);
        base.utilisateurs.get(1).setIdMedecin(1);
        Rendez_Vous prevu = new Rendez_Vous();
        prevu.setPatient(base.patients.get(1));
        prevu.setIdMedecin(1);
        prevu.setDate(LocalDate.of(2026, 10, 30));
        prevu.setHeure(LocalTime.of(9, 0));
        base.rendezVous.add(prevu);
        refuse(() -> membres.definirMedecinActif(1, false), "1 rendez-vous à venir");

        prevu.setEtat(Rendez_Vous.ETAT_ANNULE);
        refuse(() -> membres.definirMedecinActif(1, false), "désactivez-le");

        base.inactifs.add(1);
        membres.definirMedecinActif(1, false);
        assertFalse(base.medecins.get(1).isActif());
        assertTrue(dernierJournal().getDetail().contains("fiche médecin n° 1"));
        assertTrue(rdv.medecins().isEmpty());
        assertEquals(1, rdv.tousLesMedecins().size());
        assertEquals(1, membres.medecins().size());
        refuse(() -> rdv.medecinPour(1), "ne fait plus partie");

        membres.definirMedecinActif(1, true);
        assertEquals(1, rdv.medecins().size());
        assertEquals(1, rdv.medecinPour(1));
        refuse(() -> membres.definirMedecinActif(42, true), "introuvable");
    }
}
