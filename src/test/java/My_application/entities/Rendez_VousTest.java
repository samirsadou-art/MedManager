package My_application.entities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import My_application.fake.Donnees;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.Test;

public class Rendez_VousTest {

    private static Rendez_Vous rdv(int idPatient, LocalDate jour, LocalTime heure) {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(Donnees.patient(idPatient, "P" + idPatient));
        r.setDate(jour);
        r.setHeure(heure);
        return r;
    }

    @Test
    public void passeOuAVenir() {
        Rendez_Vous r = rdv(1, LocalDate.of(2026, 10, 8), LocalTime.of(10, 15));
        assertFalse(r.isPassed(LocalDateTime.of(2026, 10, 8, 10, 14)));
        assertTrue(r.isPassed(LocalDateTime.of(2026, 10, 8, 10, 15)));
        assertTrue(r.isPassed(LocalDateTime.of(2026, 10, 9, 8, 0)));
        assertFalse(r.isPassed(LocalDateTime.of(2025, 12, 31, 23, 59)));
    }

    @Test
    public void memeCreneauEtPatient() {
        Rendez_Vous a = rdv(1, LocalDate.of(2026, 10, 8), LocalTime.of(9, 30));
        assertTrue(a.memeCreneauEtPatient(rdv(1, LocalDate.of(2026, 10, 8), LocalTime.of(9, 30))));
        assertFalse(a.memeCreneauEtPatient(rdv(2, LocalDate.of(2026, 10, 8), LocalTime.of(9, 30))));
        assertFalse(a.memeCreneauEtPatient(rdv(1, LocalDate.of(2026, 10, 8), LocalTime.of(9, 45))));
    }
}
