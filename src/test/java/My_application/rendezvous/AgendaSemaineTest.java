package My_application.rendezvous;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import My_application.entities.Rendez_Vous;
import My_application.entities.patient;
import My_application.fake.Donnees;
import java.awt.Point;
import java.awt.Rectangle;
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
}
