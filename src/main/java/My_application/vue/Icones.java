package My_application.vue;

import My_application.entities.patient;
import java.net.URL;
import javax.swing.ImageIcon;

/**
 * Icônes de la couche vue. {@code patient.getIcon()} plaçait du Swing dans
 * l'entité ; le choix de l'image (homme/femme, adulte/enfant/nourrisson) est
 * maintenant fait ici.
 */
public final class Icones {

    private Icones() {
    }

    /** Nom du fichier d'icône, sans dépendance Swing (testable). */
    public static String nomIconePatient(patient p, int taille) {
        int age = p.getAge();
        String prefixe;
        if (p.isMasculin()) {
            prefixe = age > 18 ? "h" : age > 0 ? "b" : "bb";
        } else {
            prefixe = age > 18 ? "f" : age > 0 ? "g" : "bg";
        }
        return "/res/icons/" + prefixe + taille + ".png";
    }

    public static ImageIcon patient(patient p, int taille) {
        return icone(nomIconePatient(p, taille));
    }

    /** Icône du classpath, ou null si le fichier est absent (au lieu d'une NullPointerException). */
    public static ImageIcon icone(String chemin) {
        URL url = Icones.class.getResource(chemin);
        return url == null ? null : new ImageIcon(url);
    }
}
