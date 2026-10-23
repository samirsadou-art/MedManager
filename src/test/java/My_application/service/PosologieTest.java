package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import My_application.entities.consultation;
import My_application.entities.medicament;
import My_application.fake.Donnees;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class PosologieTest {

    @Test
    public void libelleDureeEtMoment() {
        assertEquals("1 unité, 2 fois par jour, le matin",
                Posologie.libelle(Donnees.medicament(1, "X", 1, 2, medicament.MODE_MATIN, 7)));
        assertEquals("2 unités, 3 fois par jour, le soir",
                Posologie.libelle(Donnees.medicament(1, "X", 2, 3, medicament.MODE_SOIR, 7)));
        assertEquals("1 unité, 1 fois par jour",
                Posologie.libelle(Donnees.medicament(1, "X", 1, 1, medicament.MODE_INDIFFERENT, 0)));
        assertEquals("", Posologie.duree(0));
        assertEquals("1 jour", Posologie.duree(1));
        assertEquals("30 jours", Posologie.duree(30));
    }

    @Test
    public void quantiteTotale() {
        assertEquals(14, Posologie.total(1, 2, 7));
        assertEquals(0, Posologie.total(1, 2, 0));
        assertEquals("Soit 14 unités pour 7 jours", Posologie.besoin(1, 2, 7));
        assertEquals("Soit 1 unité pour 1 jour", Posologie.besoin(1, 1, 1));
        assertEquals("", Posologie.besoin(2, 2, 0));
    }

    @Test
    public void allergiesSansAccentsNiCasse() {
        List<String> allergies = Arrays.asList("Pénicilline", "Aspirine", "Lait");
        assertEquals(Collections.singletonList("Pénicilline"),
                Posologie.allergiesConcernees("PENICILLINE V 1 MUI cp", allergies));
        assertEquals(Collections.singletonList("Aspirine"),
                Posologie.allergiesConcernees("aspirine 500 mg", allergies));
        // Le premier mot du médicament contenu dans le libellé de l'allergie.
        assertEquals(Collections.singletonList("Allergie à l'amoxicilline"),
                Posologie.allergiesConcernees("Amoxicilline 1 g cp",
                        Collections.singletonList("Allergie à l'amoxicilline")));
        assertTrue(Posologie.allergiesConcernees("Paracétamol 1 g", allergies).isEmpty());
        assertTrue(Posologie.allergiesConcernees("", allergies).isEmpty());
        assertTrue(Posologie.allergiesConcernees(null, allergies).isEmpty());
    }

    private static consultation consultation(LocalDate jour, LocalTime heure, boolean avecOrdonnance) {
        consultation c = new consultation();
        c.setDate_cons(jour);
        c.setHeure_cons(heure);
        c.addOrdonnance();
        if (avecOrdonnance) {
            c.getOrdoPat().ajouter(Donnees.medicament(1, "Amlodipine 5 mg cp", 1, 1, medicament.MODE_MATIN, 30));
        }
        return c;
    }

    @Test
    public void ordonnancePrecedenteLaPlusRecente() {
        consultation ancienne = consultation(LocalDate.of(2025, 6, 5), LocalTime.of(10, 0), true);
        consultation matin = consultation(LocalDate.of(2026, 10, 15), LocalTime.of(9, 0), true);
        consultation soir = consultation(LocalDate.of(2026, 10, 15), LocalTime.of(18, 0), true);
        consultation sansOrdo = consultation(LocalDate.of(2026, 10, 20), LocalTime.of(9, 0), false);
        consultation courante = consultation(LocalDate.of(2026, 10, 23), LocalTime.of(20, 0), true);
        assertSame(soir, Posologie.ordonnancePrecedente(
                Arrays.asList(ancienne, soir, matin, sansOrdo, courante), courante));
        assertNull(Posologie.ordonnancePrecedente(Arrays.asList(sansOrdo, courante), courante));
    }
}
