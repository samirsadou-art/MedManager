package My_application.report;

import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Affiche les PDF dans la fenêtre d'aperçu de l'application ({@link ApercuPdf}) ;
 * « imprimer » ouvre l'aperçu puis la boîte d'impression. Si l'aperçu ne peut
 * pas lire le fichier (ou sans écran), le document part dans la visionneuse
 * du système, comme avant.
 */
public class AfficheurApercu implements RapportService.Afficheur {

    private static final Logger LOG = Logger.getLogger("GestiMed");

    private final RapportService.Afficheur secours;

    public AfficheurApercu() {
        this(new AfficheurSysteme());
    }

    public AfficheurApercu(RapportService.Afficheur secours) {
        this.secours = secours;
    }

    @Override
    public void ouvrir(File pdf) throws IOException {
        if (apercu(pdf) == null) {
            secours.ouvrir(pdf);
        }
    }

    @Override
    public boolean imprimer(File pdf) throws IOException {
        ApercuPdf apercu = apercu(pdf);
        if (apercu == null) {
            return secours.imprimer(pdf);
        }
        // Après l'affichage de la fenêtre, pour que la boîte d'impression s'ouvre au-dessus.
        SwingUtilities.invokeLater(apercu::imprimer);
        return true;
    }

    /** L'aperçu affiché, ou null s'il n'a pas pu l'être. */
    private static ApercuPdf apercu(File pdf) {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }
        try {
            return ApercuPdf.afficher(fenetreActive(), pdf);
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Aperçu impossible, ouverture dans la visionneuse du système : " + pdf, e);
            return null;
        }
    }

    private static Window fenetreActive() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    }
}
