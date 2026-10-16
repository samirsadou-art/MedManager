package My_application.report;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * Ouvre les PDF dans la visionneuse du système (Adobe Reader, Edge, SumatraPDF…)
 * et les imprime via l'application associée aux fichiers PDF.
 * <p>
 * L'impression directe ({@link Desktop#print}) n'existe que si une application
 * PDF capable d'imprimer est installée et associée. Sinon le document est
 * ouvert, et l'utilisateur l'imprime depuis la visionneuse.
 */
public class AfficheurSysteme implements RapportService.Afficheur {

    @Override
    public void ouvrir(File pdf) throws IOException {
        bureau().open(pdf);
    }

    @Override
    public boolean imprimer(File pdf) throws IOException {
        Desktop d = bureau();
        if (d.isSupported(Desktop.Action.PRINT)) {
            try {
                d.print(pdf);
                return true;
            } catch (IOException | UnsupportedOperationException aucuneApplication) {
                // aucune application associée à l'impression des PDF : on ouvre le document
            }
        }
        d.open(pdf);
        return false;
    }

    private static Desktop bureau() {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new UnsupportedOperationException("ouverture de fichiers non prise en charge par ce système");
        }
        return Desktop.getDesktop();
    }
}
