package My_application.service;

import My_application.entities.consultation;
import My_application.entities.medicament;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Textes et calculs de la saisie d'ordonnance (sans écran ni base) : libellé
 * de la posologie, quantité totale pour la durée, allergies du patient qui
 * concernent le médicament saisi, ordonnance précédente à reprendre.
 */
public final class Posologie {

    private Posologie() {
    }

    /** « le matin », « le soir », ou vide pour un moment indifférent. */
    public static String moment(int mode) {
        switch (mode) {
            case medicament.MODE_MATIN:
                return "le matin";
            case medicament.MODE_SOIR:
                return "le soir";
            default:
                return "";
        }
    }

    /** Ex. « 2 unités, 3 fois par jour, le matin ». */
    public static String libelle(medicament m) {
        String s = unites(m.getUnite()) + ", " + m.getPosologie() + " fois par jour";
        String moment = moment(m.getMode());
        return moment.isEmpty() ? s : s + ", " + moment;
    }

    /** « 1 jour », « 30 jours », ou vide quand la durée n'est pas précisée (0). */
    public static String duree(int jours) {
        if (jours <= 0) {
            return "";
        }
        return jours + (jours == 1 ? " jour" : " jours");
    }

    /** Nombre d'unités pour tout le traitement ; 0 sans durée. */
    public static int total(int unite, int fois, int jours) {
        return jours <= 0 ? 0 : unite * fois * jours;
    }

    /** Ex. « Soit 14 unités pour 7 jours », vide sans durée. */
    public static String besoin(int unite, int fois, int jours) {
        int total = total(unite, fois, jours);
        return total == 0 ? "" : "Soit " + unites(total) + " pour " + duree(jours);
    }

    private static String unites(int n) {
        return n + (n > 1 ? " unités" : " unité");
    }

    /**
     * Allergies du patient dont le libellé apparaît dans le nom du médicament
     * (ou l'inverse), sans tenir compte des accents ni de la casse : une
     * allergie « Pénicilline » concerne « PENICILLINE V 1 MUI ».
     */
    public static List<String> allergiesConcernees(String nomMedicament, List<String> allergies) {
        List<String> concernees = new ArrayList<>();
        String nom = simplifier(nomMedicament);
        if (nom.isEmpty()) {
            return concernees;
        }
        String premierMot = nom.split(" ")[0];
        for (String a : allergies) {
            String allergie = simplifier(a);
            if (allergie.length() >= 3 && (nom.contains(allergie)
                    || (premierMot.length() >= 4 && allergie.contains(premierMot)))) {
                concernees.add(a);
            }
        }
        return concernees;
    }

    static String simplifier(String s) {
        if (s == null) {
            return "";
        }
        String sansAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.FRENCH).replaceAll("\\s+", " ").trim();
    }

    /**
     * La consultation la plus récente, autre que {@code courante}, qui a une
     * ordonnance ; null s'il n'y en a pas.
     */
    public static consultation ordonnancePrecedente(List<consultation> consultations, consultation courante) {
        consultation retenue = null;
        for (consultation c : consultations) {
            if (c == courante || !c.hasOrdonnance() || c.getDate_cons() == null) {
                continue;
            }
            if (retenue == null || plusRecente(c, retenue)) {
                retenue = c;
            }
        }
        return retenue;
    }

    private static boolean plusRecente(consultation a, consultation b) {
        int jour = a.getDate_cons().compareTo(b.getDate_cons());
        if (jour != 0) {
            return jour > 0;
        }
        return a.getHeure_cons() != null && (b.getHeure_cons() == null || a.getHeure_cons().isAfter(b.getHeure_cons()));
    }
}
