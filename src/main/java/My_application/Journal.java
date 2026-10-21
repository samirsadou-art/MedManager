package My_application;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Journal de l'application, écrit dans un fichier.
 * <p>
 * Les fenêtres n'affichent plus le détail technique des erreurs (requête SQL,
 * trace Java) : il est enregistré ici, dans {@code ~/GestiMed/gestimed0.log}
 * (dossier modifiable par {@code -Dgestimed.journal=...}). Rotation sur
 * {@value #NOMBRE_FICHIERS} fichiers de 1 Mo.
 */
public final class Journal {

    private static final int TAILLE_MAX = 1024 * 1024;
    private static final int NOMBRE_FICHIERS = 3;

    private static File dossier;

    private Journal() {
    }

    /** Branche le fichier journal sur le logger racine ; renvoie le dossier, ou null en cas d'échec. */
    public static synchronized File installer() {
        if (dossier != null) {
            return dossier;
        }
        File d = new File(System.getProperty("gestimed.journal",
                System.getProperty("user.home") + File.separator + "GestiMed"));
        Logger racine = Logger.getLogger("");
        try {
            if (!d.isDirectory() && !d.mkdirs()) {
                throw new IOException("création impossible de " + d.getAbsolutePath());
            }
            FileHandler fichier = new FileHandler(
                    new File(d, "gestimed%g.log").getPath(), TAILLE_MAX, NOMBRE_FICHIERS, true);
            fichier.setFormatter(new SimpleFormatter());
            fichier.setEncoding("UTF-8");
            fichier.setLevel(Level.INFO);
            racine.addHandler(fichier);
            dossier = d;
        } catch (IOException | SecurityException e) {
            // Sans fichier, le journal reste sur la console : l'application démarre quand même.
            racine.log(Level.WARNING, "Journal non disponible dans " + d.getAbsolutePath(), e);
        }
        return dossier;
    }
}
