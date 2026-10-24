package My_application.rendezvous;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import My_application.entities.Rendez_Vous;
import My_application.entities.medecin;
import My_application.entities.patient;
import My_application.fake.Donnees;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

/** Agenda en planning : placement des cartes, statuts, recherche (sans afficher de fenêtre). */
public class AgendaSemaineTest {

    private static final LocalDate DIMANCHE = LocalDate.of(2026, 10, 4);
    /** Mardi 6 octobre 2026, 10 h 05. */
    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 10, 6, 10, 5);

    private patient dossier;
    private AgendaSemaine agenda;

    @Before
    public void preparer() {
        dossier = Donnees.patient(1, "Dubois", "Marie", LocalDate.of(1980, 1, 1), false);
        agenda = new AgendaSemaine(8, 9, 4, dossier);
        agenda.setHorloge(Clock.fixed(MAINTENANT.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
        agenda.setSize(700, 9 * 4 * AgendaSemaine.HAUTEUR_CRENEAU); // 100 px par jour
    }

    private static Rendez_Vous rdv(patient p, LocalDate jour, String heure) {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(p);
        r.setDate(jour);
        r.setHeure(LocalTime.parse(heure));
        return r;
    }

    private static Rendez_Vous rdv(patient p, LocalDate jour, String heure, Integer idMedecin) {
        Rendez_Vous r = rdv(p, jour, heure);
        r.setIdMedecin(idMedecin);
        return r;
    }

    private void deuxMedecins() {
        medecin amrani = Donnees.medecin(2);
        amrani.setNom("Amrani");
        amrani.setPrenom("Lila");
        agenda.setMedecins(Arrays.asList(Donnees.medecin(1), amrani));
    }

    private String infoBulle(int x, int y) {
        return agenda.getToolTipText(new MouseEvent(agenda, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 0, false));
    }

    @Test
    public void laCarteEstPlaceeAuJourEtAuQuartDHeure() {
        Rendez_Vous r = rdv(dossier, LocalDate.of(2026, 10, 6), "11:15:00"); // mardi
        agenda.afficherSemaine(DIMANCHE, Arrays.asList(r));
        int h = AgendaSemaine.HAUTEUR_CRENEAU;
        assertEquals(new Rectangle(200, (3 * 4 + 1) * h, 100, h), agenda.rectangleDe(r));
        assertSame(r, agenda.rendezVousA(new Point(250, 13 * h + 5)));
        assertNull(agenda.rendezVousA(new Point(150, 13 * h + 5)));
        assertEquals(LocalDate.of(2026, 10, 6), agenda.jourA(250));
        assertEquals(11, agenda.heureA(13 * h + 5));
        assertEquals(-1, agenda.heureA(36 * h));
    }

    @Test
    public void unRendezVousHorsDeLaSemaineOuDesHeuresNEstPasDessine() {
        assertNull(agenda.rectangleDe(rdv(dossier, LocalDate.of(2026, 10, 6), "11:00:00"))); // semaine pas chargée
        agenda.afficherSemaine(DIMANCHE, Arrays.<Rendez_Vous>asList());
        assertNull(agenda.rectangleDe(rdv(dossier, LocalDate.of(2026, 10, 11), "11:00:00")));
        assertNull(agenda.rectangleDe(rdv(dossier, LocalDate.of(2026, 10, 6), "07:45:00")));
        assertNull(agenda.rectangleDe(rdv(dossier, LocalDate.of(2026, 10, 6), "17:00:00")));
    }

    @Test
    public void statutSelonLaDateEtLePatient() {
        patient autre = Donnees.patient(2, "Martin");
        assertEquals(StatutRendezVous.PASSE, agenda.statut(rdv(dossier, LocalDate.of(2026, 10, 6), "10:00:00")));
        assertEquals(StatutRendezVous.PATIENT_COURANT, agenda.statut(rdv(dossier, LocalDate.of(2026, 10, 8), "09:00:00")));
        assertEquals(StatutRendezVous.AUJOURDHUI, agenda.statut(rdv(autre, LocalDate.of(2026, 10, 6), "15:00:00")));
        assertEquals(StatutRendezVous.A_VENIR, agenda.statut(rdv(autre, LocalDate.of(2026, 10, 7), "09:00:00")));
    }

    @Test
    public void laRechercheIgnoreLesAccentsEtLaCasse() {
        Rendez_Vous r = rdv(Donnees.patient(3, "Rousseau", "Émilie", LocalDate.of(1990, 1, 1), false),
                LocalDate.of(2026, 10, 7), "09:00:00");
        assertTrue(AgendaSemaine.correspond(r, "emilie"));
        assertTrue(AgendaSemaine.correspond(r, "ROUSS"));
        assertTrue(AgendaSemaine.correspond(r, "emilie rousseau"));
        assertTrue(AgendaSemaine.correspond(r, "  "));
        assertFalse(AgendaSemaine.correspond(r, "martin"));
        agenda.afficherSemaine(DIMANCHE, Arrays.asList(r, rdv(dossier, LocalDate.of(2026, 10, 7), "10:00:00")));
        agenda.setFiltre("dubois");
        assertEquals(1, agenda.nombreVisibles());
    }

    @Test
    public void laSelectionSuitLeRechargement() {
        Rendez_Vous r = rdv(dossier, LocalDate.of(2026, 10, 7), "09:15:00");
        agenda.afficherSemaine(DIMANCHE, Arrays.asList(r));
        agenda.setSelection(r);
        Rendez_Vous relu = rdv(dossier, LocalDate.of(2026, 10, 7), "09:15:00");
        agenda.afficherSemaine(DIMANCHE, Arrays.asList(relu));
        assertSame(relu, agenda.getSelection());
        agenda.afficherSemaine(DIMANCHE, Arrays.<Rendez_Vous>asList());
        assertNull(agenda.getSelection());
    }

    @Test
    public void libelleDeLaSemaine() {
        assertEquals("Semaine du 4 au 10 octobre 2026", AgendaSemaine.libelleSemaine(DIMANCHE));
        assertEquals("Semaine du 27 septembre au 3 octobre 2026",
                AgendaSemaine.libelleSemaine(LocalDate.of(2026, 9, 27)));
        assertEquals("Semaine du 27 décembre 2026 au 2 janvier 2027",
                AgendaSemaine.libelleSemaine(LocalDate.of(2026, 12, 27)));
    }

    @Test
    public void leFiltreMedecinMasqueLesRendezVousDesAutres() {
        LocalDate mercredi = LocalDate.of(2026, 10, 7);
        Rendez_Vous chezSahab = rdv(dossier, mercredi, "09:00:00", 1);
        Rendez_Vous chezAmrani = rdv(Donnees.patient(2, "Martin"), mercredi, "10:00:00", 2);
        agenda.afficherSemaine(DIMANCHE, Arrays.asList(chezSahab, chezAmrani));
        int h = AgendaSemaine.HAUTEUR_CRENEAU;
        agenda.setSelection(chezSahab);
        assertEquals(2, agenda.nombreAffiches());
        assertEquals(2, agenda.nombreDuJour(mercredi));

        agenda.setFiltreMedecin(2);
        assertEquals(Integer.valueOf(2), agenda.getFiltreMedecin());
        assertEquals(1, agenda.nombreAffiches());
        assertEquals(1, agenda.nombreDuJour(mercredi));
        assertNull("rendez-vous masqué : plus cliquable", agenda.rendezVousA(new Point(350, 4 * h + 5)));
        assertSame(chezAmrani, agenda.rendezVousA(new Point(350, 8 * h + 5)));
        assertNull("la sélection masquée est abandonnée", agenda.getSelection());
        agenda.setFiltre("dubois");
        assertEquals(0, agenda.nombreVisibles());

        agenda.setFiltreMedecin(null);
        assertEquals(1, agenda.nombreVisibles());
    }

    @Test
    public void deuxMedecinsAuMemeCreneauSePartagentLaCase() {
        LocalDate mercredi = LocalDate.of(2026, 10, 7);
        Rendez_Vous chezAmrani = rdv(Donnees.patient(2, "Martin"), mercredi, "09:00:00", 2);
        Rendez_Vous chezSahab = rdv(dossier, mercredi, "09:00:00", 1);
        agenda.afficherSemaine(DIMANCHE, Arrays.asList(chezAmrani, chezSahab));
        int y = 4 * AgendaSemaine.HAUTEUR_CRENEAU;
        // Triées par médecin : le n° 1 à gauche.
        assertEquals(new Rectangle(300, y, 50, AgendaSemaine.HAUTEUR_CRENEAU), agenda.carteDe(chezSahab));
        assertEquals(new Rectangle(350, y, 50, AgendaSemaine.HAUTEUR_CRENEAU), agenda.carteDe(chezAmrani));
        assertSame(chezSahab, agenda.rendezVousA(new Point(320, y + 5)));
        assertSame(chezAmrani, agenda.rendezVousA(new Point(380, y + 5)));

        // Un créneau proposé au même quart d'heure prend sa part de la case.
        Rendez_Vous propose = rdv(Donnees.patient(3, "Petit"), mercredi, "09:00:00", 3);
        agenda.setPropose(propose);
        assertEquals(new Rectangle(366, y, 34, AgendaSemaine.HAUTEUR_CRENEAU), agenda.carteDe(propose));
        agenda.setPropose(null);

        agenda.setFiltreMedecin(2);
        assertEquals(agenda.rectangleDe(chezAmrani), agenda.carteDe(chezAmrani));
    }

    @Test
    public void lesPlacesLibresSontCompteesChezLeMedecinDesProchainsRendezVous() {
        LocalDate mercredi = LocalDate.of(2026, 10, 7);
        agenda.afficherSemaine(DIMANCHE, Arrays.asList(
                rdv(dossier, mercredi, "09:00:00", 1),
                rdv(Donnees.patient(2, "Martin"), mercredi, "09:15:00", 2),
                rdv(Donnees.patient(3, "Petit"), mercredi, "09:30:00", null)));
        assertEquals("sans médecin choisi : tout le cabinet", 3, agenda.nombreDansLHeure(mercredi, 9));
        agenda.setMedecinReservation(2);
        assertEquals("un rendez-vous sans médecin compte pour tous", 2, agenda.nombreDansLHeure(mercredi, 9));

        deuxMedecins();
        int h = AgendaSemaine.HAUTEUR_CRENEAU;
        assertTrue(infoBulle(350, 7 * h + 5).contains("2 / 4 places prises de 9 h à 10 h chez Dr AMRANI"));
        assertTrue(infoBulle(350, 5 * h + 5).contains("Avec Dr Amrani Lila"));
        assertEquals("Dr SAHAB", agenda.nomMedecin(rdv(dossier, mercredi, "11:00:00", 1)));
        medecin sansTitre = Donnees.medecin(3);
        sansTitre.setTitre(null);
        assertEquals("SAHAB", AgendaSemaine.nomCourt(sansTitre));
        assertNull(agenda.nomMedecin(rdv(dossier, mercredi, "11:00:00", null)));
    }

    @Test
    public void leDessinAfficheLeMedecinSansErreur() {
        LocalDate mardi = LocalDate.of(2026, 10, 6);
        Rendez_Vous a = rdv(dossier, mardi, "11:00:00", 1);
        Rendez_Vous b = rdv(Donnees.patient(2, "Martin"), mardi, "11:00:00", 2);
        agenda.afficherSemaine(DIMANCHE, Arrays.asList(a, b, rdv(dossier, mardi, "08:00:00", 2)));
        deuxMedecins();
        agenda.setSelection(a);
        agenda.setPropose(rdv(dossier, mardi, "11:15:00", 1));
        BufferedImage image = new BufferedImage(700, agenda.getHeight(), BufferedImage.TYPE_INT_RGB);
        agenda.paint(image.getGraphics());
        agenda.setFiltreMedecin(1);
        agenda.paint(image.getGraphics());
        assertEquals(1, agenda.nombreDuJour(mardi));
    }
}
