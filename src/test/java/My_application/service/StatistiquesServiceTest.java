package My_application.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.entities.Statistiques;
import My_application.entities.User;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import My_application.service.StatistiquesService.Barre;
import My_application.service.StatistiquesService.Periode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tableau de bord de l'administrateur : nombres agrégés par période et par médecin. */
public class StatistiquesServiceTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    private MemoireDAOFactory base;
    private Session session;
    private StatistiquesService service;

    @Before
    public void preparer() {
        base = Donnees.base();
        medecin amrani = Donnees.medecin(2);
        amrani.setNom("OUALI");
        base.medecins.put(2, amrani);
        session = new Session(base, 1, 1);
        User admin = Donnees.utilisateur(9);
        admin.setRole(Role.ADMIN);
        session.ouvrir(admin);
        service = new StatistiquesService(base, session);

        // Patient 1 : vu en septembre puis en octobre (pas nouveau en octobre) ; patient 2 : nouveau le 3.
        consulter(1, 1, LocalDate.of(2026, 9, 20));
        consulter(1, 1, OCT_1);
        consulter(2, 2, OCT_1.plusDays(2));
        consulter(2, 1, OCT_1.plusDays(2));
        rdv(1, OCT_1, Rendez_Vous.ETAT_TERMINE);
        rdv(1, OCT_1.plusDays(1), Rendez_Vous.ETAT_ABSENT);
        rdv(2, OCT_1.plusDays(2), Rendez_Vous.ETAT_ARRIVE);
        rdv(2, OCT_1.plusDays(3), Rendez_Vous.ETAT_ANNULE);
        rdv(1, OCT_1.plusDays(20), Rendez_Vous.ETAT_PREVU);
        passage(1, OCT_1, 10, null, false, Passage.TERMINE);
        passage(2, OCT_1.plusDays(2), 30, 5, true, Passage.TERMINE);
        passage(1, OCT_1.plusDays(2), null, 6, false, Passage.PARTI);
    }

    private void consulter(int idPatient, int idMedecin, LocalDate jour) {
        consultation c = new consultation(base.patients.get(idPatient), base.medecins.get(idMedecin));
        c.setDate_cons(jour);
        c.setHeure_cons(LocalTime.of(9, 0));
        base.consultations.add(c);
    }

    private void rdv(int idMedecin, LocalDate jour, int etat) {
        Rendez_Vous r = new Rendez_Vous();
        r.setPatient(base.patients.get(1));
        r.setIdMedecin(idMedecin);
        r.setDate(jour);
        r.setHeure(LocalTime.of(9, 0));
        r.setEtat(etat);
        base.rendezVous.add(r);
    }

    private void passage(int idMedecin, LocalDate jour, Integer minutes, Integer idRdv, boolean urgence, int etat) {
        Passage p = new Passage();
        p.setPatient(base.patients.get(1));
        p.setIdMedecin(idMedecin);
        p.setJour(jour);
        LocalDateTime arrivee = jour.atTime(9, 0);
        p.setArriveeLe(arrivee);
        p.setAppeleLe(minutes == null ? null : arrivee.plusMinutes(minutes));
        p.setIdRendezVous(idRdv);
        p.setUrgence(urgence);
        p.setEtat(etat);
        base.passages.add(p);
    }

    private static void refuse(Runnable action, String fragment) {
        try {
            action.run();
            fail("action permise à tort");
        } catch (MetierException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(fragment));
        }
    }

    @Test
    public void activiteDuCabinetSurUnMois() {
        Statistiques s = service.calculer(OCT_1, LocalDate.of(2026, 10, 31), null);
        assertEquals(3, s.getConsultations());
        assertEquals(2, s.getPatientsDistincts());
        assertEquals(1, s.getNouveauxPatients());
        assertEquals(Integer.valueOf(2), s.getConsultationsParMedecin().get(1));
        assertEquals(Integer.valueOf(1), s.getConsultationsParMedecin().get(2));
        assertEquals(5, s.getRendezVous());
        assertEquals(2, s.getRendezVousHonores());
        assertEquals(100.0 / 3, s.getTauxAbsence(), 0.001);
        assertEquals(3, s.getSalle().getPassages());
        assertEquals(1, s.getSalle().getSansRendezVous());
        assertEquals(1, s.getSalle().getUrgences());
        assertEquals(1, s.getSalle().getPartis());
        assertEquals(20.0, s.getSalle().getAttenteMoyenne(), 0.001);

        Map<String, String> ind = StatistiquesService.indicateurs(s);
        assertEquals("3", ind.get("Consultations"));
        assertEquals("33,3 %", ind.get("Taux d'absence"));
        assertEquals("20 min", ind.get("Attente moyenne"));
    }

    @Test
    public void filtreParMedecin() {
        Statistiques s = service.calculer(OCT_1, LocalDate.of(2026, 10, 31), 2);
        assertEquals(1, s.getConsultations());
        assertEquals(1, s.getNouveauxPatients());
        assertEquals(1, s.getConsultationsParMedecin().size());
        assertEquals(2, s.getRendezVous());
        assertEquals(1, s.getSalle().getPassages());
        assertEquals(Integer.valueOf(2), s.getIdMedecin());
    }

    @Test
    public void periodeSansActivite() {
        Statistiques s = service.calculer(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), null);
        assertEquals(0, s.getConsultations());
        assertNull(s.getTauxAbsence());
        assertNull(s.getSalle().getAttenteMoyenne());
        assertEquals("–", StatistiquesService.indicateurs(s).get("Taux d'absence"));
        assertEquals("–", StatistiquesService.indicateurs(s).get("Attente moyenne"));
    }

    @Test
    public void periodeInvalideOuTropLongue() {
        refuse(() -> service.calculer(null, OCT_1, null), "début");
        refuse(() -> service.calculer(OCT_1, OCT_1.minusDays(1), null), "avant");
        refuse(() -> service.calculer(OCT_1.minusYears(4), OCT_1, null), "trois ans");
        service.calculer(OCT_1.minusYears(3).plusDays(5), OCT_1, null);
    }

    @Test
    public void reserveAuxAdministrateurs() {
        session.ouvrir(Donnees.utilisateur(1));
        refuse(() -> service.calculer(OCT_1, OCT_1, null), "administrateur");
        refuse(() -> service.medecins(), "administrateur");
    }

    @Test
    public void serieParJourSemaineOuMois() {
        List<Barre> jours = StatistiquesService.serie(service.calculer(OCT_1, LocalDate.of(2026, 10, 31), null));
        assertEquals(31, jours.size());
        assertEquals("1 oct.", jours.get(0).getLibelle());
        assertEquals("Jeudi 1 octobre 2026", jours.get(0).getDetail());
        assertEquals(1, jours.get(0).getValeur());
        assertEquals(2, jours.get(2).getValeur());
        assertEquals(0, jours.get(1).getValeur());

        Statistiques trimestre = service.calculer(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 30), null);
        assertEquals("semaine", StatistiquesService.grain(trimestre));
        List<Barre> semaines = StatistiquesService.serie(trimestre);
        assertTrue(semaines.get(0).getDetail().startsWith("Semaine du lundi 31 août"));
        int total = 0;
        for (Barre b : semaines) {
            total += b.getValeur();
        }
        assertEquals(4, total);

        Statistiques annee = service.calculer(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null);
        assertEquals("mois", StatistiquesService.grain(annee));
        List<Barre> mois = StatistiquesService.serie(annee);
        assertEquals(12, mois.size());
        assertEquals("Octobre 2026", mois.get(9).getDetail());
        assertEquals(3, mois.get(9).getValeur());
        assertEquals(1, mois.get(8).getValeur());
    }

    @Test
    public void bornesDesPeriodesProposees() {
        LocalDate jeudi = LocalDate.of(2026, 10, 22);
        assertArrayEquals(new LocalDate[] {jeudi, jeudi}, Periode.AUJOURDHUI.bornes(jeudi));
        assertArrayEquals(new LocalDate[] {LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25)},
                Periode.SEMAINE.bornes(jeudi));
        assertArrayEquals(new LocalDate[] {OCT_1, LocalDate.of(2026, 10, 31)}, Periode.MOIS.bornes(jeudi));
        assertArrayEquals(new LocalDate[] {LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)},
                Periode.MOIS_PRECEDENT.bornes(jeudi));
        assertArrayEquals(new LocalDate[] {LocalDate.of(2026, 7, 23), jeudi}, Periode.TRIMESTRE.bornes(jeudi));
        assertArrayEquals(new LocalDate[] {LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)},
                Periode.ANNEE.bornes(jeudi));
        assertArrayEquals(new LocalDate[] {LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)},
                Periode.ANNEE_PRECEDENTE.bornes(jeudi));
        assertEquals("Ce mois-ci", Periode.MOIS.toString());
    }

    @Test
    public void exportCsvPourExcel() throws Exception {
        Statistiques s = service.calculer(OCT_1, LocalDate.of(2026, 10, 3), null);
        Map<Integer, medecin> medecins = new HashMap<>();
        for (medecin m : service.medecins()) {
            medecins.put(m.getIdmedecin(), m);
        }
        medecins.get(2).setNom("OUALI; dit \"Lina\"");
        File f = dossier.newFile("stats.csv");
        service.exporterCsv(s, medecins, f);
        byte[] octets = Files.readAllBytes(f.toPath());
        assertEquals((byte) 0xEF, octets[0]);
        String csv = new String(octets, 3, octets.length - 3, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("Activité du cabinet;du 01/10/2026 au 03/10/2026\r\n"));
        assertTrue(csv.contains("Médecin;Tous les médecins\r\n"));
        assertTrue(csv.contains("Consultations;3\r\n"));
        assertTrue(csv.contains("\"Dr OUALI; dit \"\"Lina\"\" Said\";1\r\n"));
        assertTrue(csv.contains("Samedi 3 octobre 2026;2\r\n"));
        assertTrue("aucun nom de patient", !csv.contains("AMRANI") && !csv.contains("BELKACEM"));

        File dossierAbsent = new File(dossier.getRoot(), "absent/stats.csv");
        refuse(() -> service.exporterCsv(s, medecins, dossierAbsent), "Impossible d'enregistrer");
    }
}
