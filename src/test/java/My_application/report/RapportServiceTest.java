package My_application.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.medicament;
import My_application.fake.Donnees;
import My_application.service.MetierException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.time.LocalTime;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Génération des documents PDF (OpenPDF) : contenu vérifié en relisant le PDF produit. */
public class RapportServiceTest {

    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    /** Afficheur factice : enregistre ce qui aurait été ouvert ou imprimé. */
    static class FauxAfficheur implements RapportService.Afficheur {
        final List<File> ouverts = new ArrayList<>();
        final List<File> imprimes = new ArrayList<>();
        boolean impressionDisponible = true;
        boolean enPanne;

        @Override
        public void ouvrir(File pdf) throws IOException {
            if (enPanne) {
                throw new IOException("aucune visionneuse");
            }
            ouverts.add(pdf);
        }

        @Override
        public boolean imprimer(File pdf) throws IOException {
            if (!impressionDisponible) {
                ouvrir(pdf);
                return false;
            }
            imprimes.add(pdf);
            return true;
        }
    }

    private FauxAfficheur afficheur;
    private RapportService service;
    private consultation c;

    @Before
    public void preparer() throws IOException {
        afficheur = new FauxAfficheur();
        service = new RapportService(dossier.newFolder("documents"), afficheur);
        c = new consultation(Donnees.patient(1, "AMRANI", "Karim", LocalDate.of(1980, 5, 17), true), Donnees.medecin(1));
        c.setDate_cons(LocalDate.of(2026, 10, 8));
        c.setHeure_cons(LocalTime.parse("09:30:00"));
        c.getOrdoPat().ajouter(Donnees.medicament(1, "DOLIPRANE 1G", 1, 3, medicament.MODE_SOIR, 5));
        c.getOrdoPat().ajouter(Donnees.medicament(2, "SPASFON", 2, 2, medicament.MODE_INDIFFERENT, 0));
        c.getAnaPat().setListeAnaly(Arrays.asList(Donnees.analyse(1, "GLYCÉMIE", "à jeun")));
    }

    private static String texte(byte[] pdf, int page) throws IOException {
        return new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(page);
    }

    private static int pages(byte[] pdf) throws IOException {
        return new PdfReader(pdf).getNumberOfPages();
    }

    // ----- contenu des PDF -----

    @Test
    public void lOrdonnanceContientEnteteePatientMedicamentsEtPiedDePage() throws IOException {
        byte[] pdf = service.pdfOrdonnance(c);
        assertTrue("signature PDF", new String(pdf, 0, 5, "ISO-8859-1").startsWith("%PDF-"));
        assertEquals(1, pages(pdf));
        String t = texte(pdf, 1);
        assertTrue(t, t.contains("ORDONNANCE"));
        assertTrue(t, t.contains("Cabinet médical"));
        assertTrue(t, t.contains("Dr SAHAB Said"));
        assertTrue(t, t.contains("Inscrit à l'ordre N° : 12345"));
        assertTrue(t, t.contains("Date : 08/10/2026"));
        assertTrue(t, t.contains("AMRANI Karim"));
        assertTrue(t, t.contains("DOLIPRANE 1G"));
        assertTrue(t, t.contains("pendant 5 jours"));
        assertTrue(t, t.contains("le soir"));
        assertTrue(t, t.contains("Adresse : Fréha, Tizi-Ouzou"));
        assertTrue(t, t.contains("Page 1"));
    }

    @Test
    public void laDemandeDAnalysesContientLesAnalysesEtLesObservations() throws IOException {
        String t = texte(service.pdfAnalyses(c), 1);
        assertTrue(t, t.contains("ANALYSES BIOLOGIQUES"));
        assertTrue(t, t.contains("GLYCÉMIE"));
        assertTrue(t, t.contains("à jeun"));
    }

    @Test
    public void uneLongueOrdonnanceTientSurPlusieursPagesAvecEnTeteRepete() throws IOException {
        for (int i = 3; i <= 60; i++) {
            c.getOrdoPat().ajouter(Donnees.medicament(i, "MEDICAMENT " + i, 1, 1, 0, 0));
        }
        byte[] pdf = service.pdfOrdonnance(c);
        assertTrue(pages(pdf) >= 2);
        String page2 = texte(pdf, 2);
        assertTrue("en-tête du tableau répété", page2.contains("Médicament"));
        assertTrue("pied de page sur chaque page", page2.contains("Page 2"));
        assertTrue(page2.contains("MEDICAMENT 60") || texte(pdf, pages(pdf)).contains("MEDICAMENT 60"));
    }

    // ----- enregistrement, ouverture, impression -----

    @Test
    public void apercuEnregistreLePdfEtLOuvre() {
        assertTrue(service.ordonnance(c, false));
        assertEquals(1, afficheur.ouverts.size());
        File f = afficheur.ouverts.get(0);
        assertTrue(f.isFile());
        assertEquals("Ordonnance_AMRANI_Karim_2026-10-08_0930.pdf", f.getName());
    }

    @Test
    public void impressionDirecte() {
        assertTrue(service.analyses(c, true));
        assertEquals(1, afficheur.imprimes.size());
    }

    @Test
    public void sansImpressionDirecteLeDocumentEstOuvert() {
        afficheur.impressionDisponible = false;
        assertFalse(service.ordonnance(c, true));
        assertEquals(1, afficheur.ouverts.size());
    }

    @Test
    public void siAucuneVisionneuseLeMessageIndiqueOuEstLeFichier() {
        afficheur.enPanne = true;
        try {
            service.ordonnance(c, false);
            fail();
        } catch (MetierException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Ordonnance_AMRANI_Karim"));
        }
    }

    @Test(expected = MetierException.class)
    public void pasDOrdonnancePasDeDocument() {
        service.ordonnance(new consultation(c.getPatient(), c.getMedecin()), false);
    }

    // ----- fonctions de mise en forme -----

    @Test
    public void telephoneSansSecondNumero() {
        // Avant : NullPointerException sur getTelephone2().isEmpty()
        assertEquals("Téléphone : 026 00 00 00    Email : cabinet@example.org", RapportService.telephone(Donnees.medecin(1)));
        medecin m = Donnees.medecin(1);
        m.setTelephone2("0555 12 34 56");
        m.setEmail(null);
        assertEquals("Téléphone : 026 00 00 00 - 0555 12 34 56", RapportService.telephone(m));
    }

    @Test
    public void nomDeFichierSansAccentsNiCaracteresInterdits() {
        c.getPatient().setNom("OULD-ALI");
        c.getPatient().setPrenom("Hélène Ç/x");
        assertEquals("Analyses_OULD-ALI_Helene_C_x_2026-10-08_0930.pdf", RapportService.nomFichier("Analyses", c));
    }

    @Test
    public void lignesDOrdonnance() {
        List<String[]> l = RapportService.lignesOrdonnance(c.getOrdoPat().getMedicaments());
        assertEquals(2, l.size());
        assertEquals(Arrays.asList("1)", "DOLIPRANE 1G", "1 U", "3 fois/jour", "le soir", "pendant 5 jours"),
                Arrays.asList(l.get(0)));
        assertEquals("", l.get(1)[5]);
    }

    @Test
    public void observationAbsenteAfficheeVide() {
        assertEquals("", RapportService.lignesAnalyses(Arrays.asList(Donnees.analyse(1, "TSH", null))).get(0)[2]);
    }
}
