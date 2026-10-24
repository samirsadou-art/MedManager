package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.analyse;
import My_application.entities.consultation;
import My_application.entities.medicament;
import My_application.entities.pathologie;
import My_application.entities.symptome;
import My_application.fake.Donnees;
import My_application.service.SyntheseDossier.Examen;
import My_application.service.SyntheseDossier.Frequence;
import My_application.service.SyntheseDossier.Interpretation;
import My_application.service.SyntheseDossier.Mesure;
import My_application.service.SyntheseDossier.NiveauTension;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SyntheseDossierTest {

    private static consultation consultation(String jour, String heure) {
        consultation c = new consultation(Donnees.patient(1, "A"), Donnees.medecin(1));
        c.setDate_cons(LocalDate.parse(jour));
        c.setHeure_cons(LocalTime.parse(heure));
        return c;
    }

    /** Trois consultations dans l'ordre chronologique, comme le dossier les garde. */
    private static List<consultation> dossier() {
        consultation a = consultation("2026-01-12", "09:00");
        a.setPoids(85);
        a.setPression(150, 95);
        a.addPathologie(new pathologie(1, "Hypertension artérielle"));
        a.addSymptome(new symptome(1, "Céphalées"));
        consultation b = consultation("2026-06-18", "11:00");
        b.setPoids(83.5);
        b.setTemperature(37);
        b.addPathologie(new pathologie(1, "HYPERTENSION ARTÉRIELLE"));
        b.addPathologie(new pathologie(2, "Diabète type 2"));
        b.addSymptome(new symptome(1, "Céphalées"));
        b.addSymptome(new symptome(2, "Vertiges"));
        b.getAnaPat().setListeAnaly(Arrays.asList(Donnees.analyse(1, "Glycémie", "à jeun")));
        consultation c = consultation("2026-10-15", "10:12");
        c.setPoids(82);
        c.setPression(135, 85);
        c.addPathologie(new pathologie(1, "Hypertension artérielle"));
        c.getOrdoPat().ajouter(Donnees.medicament(5, "Amlodipine 5 mg", 1, 1, medicament.MODE_MATIN, 30));
        analyse hba1c = Donnees.analyse(2, "HbA1c", "");
        hba1c.setResultat("7,4 %");
        hba1c.setNote(3);
        c.getAnaPat().setListeAnaly(Arrays.asList(hba1c));
        return new ArrayList<>(Arrays.asList(a, b, c));
    }

    @Test
    public void lesConsultationsLesPlusRecentesDAbord() {
        List<consultation> l = dossier();
        consultation memeJourPlusTard = consultation("2026-10-15", "16:00");
        l.add(0, memeJourPlusTard);
        List<consultation> triees = SyntheseDossier.plusRecentesDAbord(l);
        assertEquals(memeJourPlusTard, triees.get(0));
        assertEquals(LocalDate.parse("2026-01-12"), triees.get(3).getDate_cons());
        assertEquals("la liste du dossier n'est pas touchée", memeJourPlusTard, l.get(0));
    }

    @Test
    public void laTendanceCompareLesDeuxDernieresMesures() {
        List<consultation> l = dossier();
        assertEquals("▼ 1,5 kg", SyntheseDossier.tendance(l, Mesure.POIDS));
        assertEquals(-1.5, SyntheseDossier.ecart(l, Mesure.POIDS), 1e-9);
        assertEquals("la tension n'a pas été prise en juin", "▼ 15 mmHg",
                SyntheseDossier.tendance(l, Mesure.TENSION));
        assertEquals("une seule mesure", "", SyntheseDossier.tendance(l, Mesure.TEMPERATURE));
        assertEquals(0, SyntheseDossier.ecart(l, Mesure.TEMPERATURE), 0);
        assertEquals("jamais mesurée", 0, SyntheseDossier.deuxDernieresMesures(l, Mesure.POULS).size());
        l.get(2).setPoids(83.5);
        l.get(2).setPoids(83.5);
        assertEquals("= stable", SyntheseDossier.tendance(l, Mesure.POIDS));
    }

    @Test
    public void chaqueMesureSeLitEtSAffiche() {
        consultation c = consultation("2026-10-15", "10:00");
        assertEquals("", Mesure.POULS.texte(c));
        c.setTemperature(37.25);
        c.setPul_Cardiaque(78);
        c.setTauxRespir(16);
        c.setPression(120, 80);
        assertEquals("37,2", Mesure.TEMPERATURE.texte(c).substring(0, 4));
        assertEquals("78", Mesure.POULS.texte(c));
        assertEquals("16", Mesure.RESPIRATION.texte(c));
        assertEquals("120/80", Mesure.TENSION.texte(c));
        assertEquals(120, Mesure.TENSION.valeur(c), 0);
        assertEquals("Respiration", Mesure.RESPIRATION.getLibelle());
        assertEquals("/min", Mesure.RESPIRATION.getUnite());
        assertEquals("82", SyntheseDossier.nombre(82.0));
    }

    @Test
    public void laTensionEstClasseeSelonLesSeuils() {
        consultation c = consultation("2026-10-15", "10:00");
        assertNull(NiveauTension.de(c));
        c.setPression(120, 80);
        assertEquals(NiveauTension.NORMALE, NiveauTension.de(c));
        c.setPression(132, 80);
        assertEquals(NiveauTension.LIMITE, NiveauTension.de(c));
        c.setPression(125, 86);
        assertEquals(NiveauTension.LIMITE, NiveauTension.de(c));
        c.setPression(140, 80);
        assertEquals(NiveauTension.ELEVEE, NiveauTension.de(c));
        c.setPression(120, 90);
        assertEquals(NiveauTension.ELEVEE, NiveauTension.de(c));
    }

    @Test
    public void lesPathologiesSontCompteesSansTenirCompteDeLaCasse() {
        List<Frequence> l = SyntheseDossier.pathologies(dossier());
        assertEquals(2, l.size());
        Frequence hta = l.get(0);
        assertEquals("Hypertension artérielle", hta.getDesignation());
        assertEquals(3, hta.getNombre());
        assertEquals(LocalDate.parse("2026-01-12"), hta.getPremiere());
        assertEquals(LocalDate.parse("2026-10-15"), hta.getDerniere());
        assertEquals("Diabète type 2", l.get(1).getDesignation());

        List<Frequence> s = SyntheseDossier.symptomes(dossier());
        assertEquals("Céphalées", s.get(0).getDesignation());
        assertEquals(2, s.get(0).getNombre());
        assertEquals(1, s.get(1).getNombre());
        assertTrue(SyntheseDossier.pathologies(Collections.<consultation>emptyList()).isEmpty());
    }

    @Test
    public void unResumeDitSymptomesDiagnosticEtTension() {
        List<consultation> l = dossier();
        assertEquals("Céphalées · Hypertension artérielle · TA 150/95", SyntheseDossier.resume(l.get(0)));
        assertEquals("Céphalées, Vertiges · HYPERTENSION ARTÉRIELLE, Diabète type 2",
                SyntheseDossier.resume(l.get(1)));
        assertEquals("Rien de noté", SyntheseDossier.resume(consultation("2026-10-15", "10:00")));
    }

    @Test
    public void lesExamensDuPlusRecentAuPlusAncienAvecLeurInterpretation() {
        List<Examen> l = SyntheseDossier.examens(dossier());
        assertEquals(2, l.size());
        assertEquals("HbA1c", l.get(0).getAnalyse().getDesign());
        assertEquals(Interpretation.ANORMAL, l.get(0).getInterpretation());
        assertEquals(LocalDate.parse("2026-10-15"), l.get(0).getConsultation().getDate_cons());
        assertEquals(Interpretation.EN_ATTENTE, l.get(1).getInterpretation());

        analyse a = Donnees.analyse(3, "Créatinine", "");
        a.setResultat("9 mg/l");
        assertEquals(Interpretation.NON_INTERPRETE, Interpretation.de(a));
        a.setNote(1);
        assertEquals(Interpretation.NORMAL, Interpretation.de(a));
        a.setNote(2);
        assertEquals("À surveiller", Interpretation.de(a).getLibelle());
        a.setNote(4);
        assertEquals(Interpretation.NON_INTERPRETE, Interpretation.de(a));
    }

    @Test
    public void lesOrdonnancesEtLeurPosologie() {
        List<consultation> l = SyntheseDossier.ordonnances(dossier());
        assertEquals(1, l.size());
        medicament m = l.get(0).getOrdoPat().getMedicaments().get(0);
        assertEquals("1 U · 1 fois/jour le matin · pendant 30 jours", SyntheseDossier.posologie(m));
        assertEquals("2 U · 3 fois/jour le soir",
                SyntheseDossier.posologie(Donnees.medicament(6, "X", 2, 3, medicament.MODE_SOIR, 0)));
        assertEquals("1 U · 2 fois/jour · pendant 7 jours",
                SyntheseDossier.posologie(Donnees.medicament(7, "Y", 1, 2, medicament.MODE_INDIFFERENT, 7)));
    }
}
