package My_application.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import My_application.entities.consultation;
import My_application.entities.medicament;
import My_application.fake.Donnees;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Aperçu : rendu PDFBox des documents OpenPDF, zoom, et repli sans écran. */
public class ApercuPdfTest {

    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    private byte[] ordonnance() {
        consultation c = new consultation(Donnees.patient(1, "AMRANI", "Karim", LocalDate.of(1980, 5, 17), true),
                Donnees.medecin(1));
        c.setDate_cons(LocalDate.of(2026, 10, 8));
        c.setHeure_cons(LocalTime.parse("09:30:00"));
        c.getOrdoPat().ajouter(Donnees.medicament(1, "DOLIPRANE 1G", 1, 3, medicament.MODE_SOIR, 5));
        return new RapportService(dossier.getRoot(), new RapportServiceTest.FauxAfficheur()).pdfOrdonnance(c);
    }

    @Test
    public void lesDocumentsOpenPdfSontDessinesParPdfBox() throws IOException {
        try (PDDocument doc = Loader.loadPDF(ordonnance())) {
            assertEquals(1, doc.getNumberOfPages());
            BufferedImage image = ApercuPdf.rendre(new PDFRenderer(doc), 0, 1f);
            // A4 à 72 ppp : 595 × 842 points.
            assertEquals(595, image.getWidth());
            assertEquals(842, image.getHeight());
            assertTrue("la page n'est pas vide", contientDuTexte(image));
        }
    }

    private static boolean contientDuTexte(BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x += 2) {
            for (int y = 0; y < image.getHeight(); y += 2) {
                if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void leZoomResteDansSesBornes() {
        assertEquals(ApercuPdf.ZOOM_MIN, ApercuPdf.borner(0.1f), 0.0001f);
        assertEquals(ApercuPdf.ZOOM_MAX, ApercuPdf.borner(10f), 0.0001f);
        assertEquals(1.5f, ApercuPdf.borner(1.5f), 0.0001f);
    }

    @Test
    public void ajusterALaLargeur() {
        // 595 points à 96 ppp = 793 pixels : à 100 % la page tient juste.
        assertEquals(1f, ApercuPdf.zoomPourLargeur(793, 595f), 0.01f);
        assertEquals(0.5f, ApercuPdf.zoomPourLargeur(397, 595f), 0.01f);
        assertEquals(1f, ApercuPdf.zoomPourLargeur(0, 595f), 0.0001f);
    }

    @Test
    public void sansEcranLeDocumentPartDansLaVisionneuseDuSysteme() throws IOException {
        RapportServiceTest.FauxAfficheur secours = new RapportServiceTest.FauxAfficheur();
        AfficheurApercu afficheur = new AfficheurApercu(secours);
        File pdf = dossier.newFile("Ordonnance_test.pdf");
        Files.write(pdf.toPath(), ordonnance());

        afficheur.ouvrir(pdf);
        assertTrue(afficheur.imprimer(pdf));

        assertEquals(1, secours.ouverts.size());
        assertEquals(1, secours.imprimes.size());
    }
}
