package My_application.vue;

import My_application.entities.patient;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Icônes de la couche vue. {@code patient.getIcon()} plaçait du Swing dans
 * l'entité ; le choix de l'image (homme/femme, adulte/enfant/nourrisson) est
 * maintenant fait ici.
 * <p>
 * Les icônes d'interface sont des SVG (Tabler Icons, licence MIT, dans
 * {@code /res/svg/}) teintés aux couleurs de la charte ({@link Theme}). Les
 * anciennes icônes PNG ont chacune un équivalent SVG (voir
 * {@link #remplacement}). Les avatars des patients sont des SVG en couleurs
 * dessinés dans la même palette ({@code /res/svg/patients/}) : homme, femme,
 * garçon, fille, bébé garçon, bébé fille.
 */
public final class Icones {

    private static final String DOSSIER_SVG = "res/svg/";

    /** Ancienne icône PNG (nom de fichier) → icône SVG. */
    private static final Map<String, String> EQUIVALENTS = new HashMap<>();
    /** Icônes affichées dans une couleur particulière (sinon : couleur demandée). */
    private static final Map<String, Color> COULEURS = new HashMap<>();

    static {
        equivalent("stethoscope", "consult48.png");
        equivalent("users", "manicon48.png");
        equivalent("message-circle", "chat24.png", "chat48.png", "ActivChat32.png");
        equivalent("message-circle-off", "StopChat32.png");
        equivalent("bell", "Bell.png");
        equivalent("speakerphone", "megaphone32.png");
        equivalent("player-stop", "stop.png");
        equivalent("trash", "trash2.png", "corbeilleBlue.png", "Corbeille-vide-1-icon.png", "supp.png");
        equivalent("circle-plus", "addCombo.png");
        equivalent("circle-minus", "supp24.png");
        equivalent("plus", "add.png");
        equivalent("pencil", "mod.png");
        equivalent("printer", "Print-icon.png");
        equivalent("eye", "visualiser.png");
        equivalent("prescription", "ordo.png", "ordo32.png", "ordon.png");
        equivalent("pill", "med.png", "pills5.png");
        equivalent("flask", "analyse1.png", "analyse32.png");
        equivalent("droplet", "blood-icon.png", "blood-icon128.png");
        equivalent("scan", "radio.png");
        equivalent("cut", "bistouri1.png");
        equivalent("calendar-event", "rendez48.png");
        equivalent("calendar", "jour16.png", "date1_16.png");
        equivalent("chevrons-left", "first.png");
        equivalent("chevron-left", "previous.png", "previous32.png");
        equivalent("chevron-right", "next.png", "next32.png");
        equivalent("chevrons-right", "last.png");
        COULEURS.put("trash", Theme.DANGER);
    }

    private static void equivalent(String svg, String... png) {
        for (String p : png) {
            EQUIVALENTS.put(p, svg);
        }
    }

    private Icones() {
    }

    /** Nom du fichier d'icône, sans dépendance Swing (testable). */
    public static String nomIconePatient(patient p) {
        int age = p.getAge();
        String nom;
        if (p.isMasculin()) {
            nom = age > 18 ? "homme" : age > 0 ? "garcon" : "bebe-garcon";
        } else {
            nom = age > 18 ? "femme" : age > 0 ? "fille" : "bebe-fille";
        }
        return DOSSIER_SVG + "patients/" + nom + ".svg";
    }

    /** Avatar du patient (selon sexe et âge), carré, dans ses propres couleurs. */
    public static Icon patient(patient p, int taille) {
        return new FlatSVGIcon(nomIconePatient(p), taille, taille);
    }

    /**
     * Icône du classpath (son équivalent SVG si elle en a un), ou null si le
     * fichier est absent (au lieu d'une NullPointerException).
     */
    public static Icon icone(String chemin) {
        URL url = Icones.class.getResource(chemin);
        if (url == null) {
            return null;
        }
        ImageIcon png = new ImageIcon(url);
        Icon svg = remplacement(chemin, png.getIconWidth(), Theme.PRIMAIRE);
        return svg != null ? svg : png;
    }

    /** Icône SVG {@code /res/svg/<nom>.svg}, carrée, teintée dans la couleur donnée. */
    public static Icon svg(String nom, int taille, Color couleur) {
        FlatSVGIcon icone = new FlatSVGIcon(DOSSIER_SVG + nom + ".svg", taille, taille);
        final Color c = COULEURS.containsKey(nom) ? COULEURS.get(nom) : couleur;
        icone.setColorFilter(new FlatSVGIcon.ColorFilter(origine -> c));
        return icone;
    }

    /**
     * Équivalent SVG d'une ancienne icône PNG, d'après son chemin ou son URL
     * (description d'une {@code ImageIcon}), ou null si elle n'en a pas.
     */
    static Icon remplacement(String cheminOuUrl, int largeurOrigine, Color couleur) {
        if (cheminOuUrl == null) {
            return null;
        }
        String fichier = cheminOuUrl.substring(cheminOuUrl.lastIndexOf('/') + 1);
        String nom = EQUIVALENTS.get(fichier);
        return nom == null ? null : svg(nom, taille(largeurOrigine), couleur);
    }

    /** Les grandes icônes en relief deviennent des pictogrammes plus sobres. */
    static int taille(int largeurOrigine) {
        if (largeurOrigine >= 96) {
            return 64;
        }
        if (largeurOrigine >= 44) {
            return 32;
        }
        if (largeurOrigine >= 30) {
            return 24;
        }
        if (largeurOrigine >= 22) {
            return 20;
        }
        return 16;
    }
}
