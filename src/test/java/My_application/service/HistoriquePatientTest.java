package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.fake.Donnees;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class HistoriquePatientTest {

    private static Rendez_Vous rdv(String jour, String heure, int etat) {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(Donnees.patient(1, "A"));
        r.setDate(LocalDate.parse(jour));
        r.setHeure(LocalTime.parse(heure));
        r.setEtat(etat);
        r.setIdMedecin(1);
        return r;
    }

    private static Passage passage(String jour, int etat, Integer frais, Integer regle) {
        Passage p = new Passage();
        p.setPatient(Donnees.patient(1, "A"));
        p.setJour(LocalDate.parse(jour));
        p.setArriveeLe(LocalDate.parse(jour).atTime(9, 0));
        p.setEtat(etat);
        p.setFrais(frais);
        p.setMontantRegle(regle);
        if (regle != null) {
            p.setRegleLe(LocalDateTime.of(LocalDate.parse(jour), LocalTime.of(10, 0)));
        }
        return p;
    }

    @Test
    public void leProchainRendezVousEstLePlusProcheDesPrevus() {
        HistoriquePatient h = new HistoriquePatient(Arrays.asList(
                rdv("2026-11-20", "10:00", Rendez_Vous.ETAT_PREVU),
                rdv("2026-11-04", "10:30", Rendez_Vous.ETAT_PREVU),
                rdv("2026-11-01", "09:00", Rendez_Vous.ETAT_ANNULE),
                rdv("2026-10-01", "09:00", Rendez_Vous.ETAT_PREVU)),
                Collections.<Passage>emptyList(), Arrays.asList(Donnees.medecin(1)));
        assertEquals(LocalDate.parse("2026-11-04"), h.prochainRendezVous(LocalDate.parse("2026-10-23")).getDate());
        assertNull(h.prochainRendezVous(LocalDate.parse("2026-12-01")));
        assertEquals(Donnees.medecin(1).getNomComplet(), h.nomMedecin(1));
        assertEquals("", h.nomMedecin(9));
        assertEquals("", h.nomMedecin(null));
        assertEquals(4, h.getRendezVous().size());
    }

    @Test
    public void passageEnCoursDernierReglementEtResteDu() {
        Passage aujourdhui = passage("2026-10-23", Passage.EN_ATTENTE, null, null);
        Passage regle = passage("2026-10-15", Passage.TERMINE, 2000, 2000);
        Passage partiel = passage("2026-06-18", Passage.TERMINE, 2000, 1000);
        HistoriquePatient h = new HistoriquePatient(Collections.<Rendez_Vous>emptyList(),
                Arrays.asList(aujourdhui, regle, partiel), Collections.emptyList());
        assertEquals(aujourdhui, h.passageEnCours(LocalDate.parse("2026-10-23")));
        assertNull("hier, il n'est plus au cabinet", h.passageEnCours(LocalDate.parse("2026-10-24")));
        assertEquals(regle, h.dernierReglement());
        assertEquals(0, HistoriquePatient.resteDu(regle));
        assertEquals(1000, HistoriquePatient.resteDu(partiel));
        assertEquals(0, HistoriquePatient.resteDu(aujourdhui));
        assertTrue(HistoriquePatient.VIDE.getPassages().isEmpty());
        assertNull(HistoriquePatient.VIDE.dernierReglement());
    }
}
