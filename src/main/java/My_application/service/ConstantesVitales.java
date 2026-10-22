package My_application.service;

/**
 * Validation des constantes saisies dans le dossier patient.
 * <p>
 * Ces règles étaient codées dans les gestionnaires « focusLost » de la vue ;
 * isolées ici, elles sont réutilisables et testables. Chaque méthode renvoie
 * {@link My_application.entities.consultation#NON_RENSEIGNE} pour un champ vide
 * et lève une {@link MetierException} pour une valeur invalide.
 */
public final class ConstantesVitales {

    private ConstantesVitales() {
    }

    public static double temperature(String saisie) {
        if (vide(saisie)) {
            return -1;
        }
        double t = decimal(saisie, "Température");
        if (t < 25 || t > 45) {
            throw new MetierException("La valeur du champ [Température] est invalide (25 à 45 °C).");
        }
        return t;
    }

    public static double poids(String saisie) {
        if (vide(saisie)) {
            return -1;
        }
        double p = decimal(saisie, "Poids");
        if (p <= 0 || p >= 1000) {
            throw new MetierException("La valeur du champ [Poids] est invalide.");
        }
        return p;
    }

    public static int pulsation(String saisie) {
        if (vide(saisie)) {
            return -1;
        }
        int p = entier(saisie, "Pulsation");
        if (p <= 0 || p > 300) {
            throw new MetierException("La valeur du champ [Pulsation] est invalide.");
        }
        return p;
    }

    public static int respiration(String saisie) {
        if (vide(saisie)) {
            return -1;
        }
        int r = entier(saisie, "Respiration");
        if (r <= 0 || r > 100) {
            throw new MetierException("La valeur du champ [Respiration] est invalide.");
        }
        return r;
    }

    /** Au-delà, une valeur de pression est en mmHg ; jusque-là, en cmHg (12/8). */
    static final double LIMITE_CMHG = 30;

    /**
     * Pression artérielle « systolique/diastolique », en cmHg (12/8, 13,5/8)
     * ou en mmHg (120/80) : chaque valeur de 30 ou moins est en cmHg.
     *
     * @return {systolique, diastolique} en mmHg, ou deux
     *         {@link My_application.entities.consultation#NON_RENSEIGNE} si rien n'est saisi
     */
    public static int[] pression(String saisie) {
        if (vide(saisie)) {
            return new int[]{-1, -1};
        }
        String s = saisie.trim().replace(',', '.');
        if (!s.matches("\\d{1,3}(\\.\\d)?/\\d{1,3}(\\.\\d)?")) {
            throw new MetierException("La valeur du champ [Pression Artérielle] est invalide (ex. 12/8 ou 120/80).");
        }
        String[] parties = s.split("/");
        int systolique = enMmHg(Double.parseDouble(parties[0]));
        int diastolique = enMmHg(Double.parseDouble(parties[1]));
        if (systolique < 40 || systolique > 300 || diastolique < 20 || diastolique > 200) {
            throw new MetierException("La valeur du champ [Pression Artérielle] est invraisemblable "
                    + "(systolique 40 à 300 mmHg, diastolique 20 à 200 mmHg).");
        }
        if (systolique <= diastolique) {
            throw new MetierException("La pression systolique (première valeur) doit être supérieure "
                    + "à la diastolique (ex. 12/8).");
        }
        return new int[]{systolique, diastolique};
    }

    private static int enMmHg(double valeur) {
        return (int) Math.round(valeur <= LIMITE_CMHG ? valeur * 10 : valeur);
    }

    private static boolean vide(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static double decimal(String s, String champ) {
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new MetierException("La valeur du champ [" + champ + "] est invalide.");
        }
    }

    private static int entier(String s, String champ) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new MetierException("La valeur du champ [" + champ + "] est invalide.");
        }
    }
}
