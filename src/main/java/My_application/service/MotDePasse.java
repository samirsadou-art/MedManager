package My_application.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Empreintes de mots de passe (PBKDF2-HMAC-SHA256, sel aléatoire).
 * <p>
 * Format stocké dans {@code utilisateur.motDePasse} :
 * {@code pbkdf2-sha256$<itérations>$<sel base64>$<empreinte base64>}. Le
 * nombre d'itérations est enregistré avec l'empreinte : on peut l'augmenter
 * plus tard sans invalider les comptes existants.
 */
public final class MotDePasse {

    static final String PREFIXE = "pbkdf2-sha256";
    static final int ITERATIONS = 210000;
    public static final int LONGUEUR_MIN = 8;
    private static final String ALGORITHME = "PBKDF2WithHmacSHA256";
    private static final int OCTETS_SEL = 16;
    private static final int BITS_EMPREINTE = 256;
    private static final SecureRandom HASARD = new SecureRandom();

    private MotDePasse() {
    }

    public static String hacher(char[] motDePasse) {
        return hacher(motDePasse, ITERATIONS);
    }

    static String hacher(char[] motDePasse, int iterations) {
        byte[] sel = new byte[OCTETS_SEL];
        HASARD.nextBytes(sel);
        byte[] empreinte = pbkdf2(motDePasse, sel, iterations);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIXE + "$" + iterations + "$" + b64.encodeToString(sel) + "$" + b64.encodeToString(empreinte);
    }

    /** Vrai si le mot de passe correspond à l'empreinte ; faux si l'empreinte est absente ou illisible. */
    public static boolean verifier(char[] motDePasse, String stocke) {
        if (motDePasse == null || stocke == null) {
            return false;
        }
        String[] parties = stocke.split("\\$");
        if (parties.length != 4 || !PREFIXE.equals(parties[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parties[1]);
            byte[] sel = Base64.getDecoder().decode(parties[2]);
            byte[] attendue = Base64.getDecoder().decode(parties[3]);
            // Comparaison en temps constant.
            return MessageDigest.isEqual(attendue, pbkdf2(motDePasse, sel, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Règle de robustesse minimale, contrôlée à la création ou au changement du mot de passe. */
    public static void verifierRobustesse(char[] motDePasse) {
        if (motDePasse == null || motDePasse.length < LONGUEUR_MIN) {
            throw new MetierException("Le mot de passe doit contenir au moins " + LONGUEUR_MIN + " caractères.");
        }
        boolean lettre = false;
        boolean chiffre = false;
        for (char c : motDePasse) {
            lettre |= Character.isLetter(c);
            chiffre |= Character.isDigit(c);
        }
        if (!lettre || !chiffre) {
            throw new MetierException("Le mot de passe doit contenir au moins une lettre et un chiffre.");
        }
    }

    /** Efface le mot de passe en mémoire dès qu'il n'est plus utile. */
    public static void effacer(char[] motDePasse) {
        if (motDePasse != null) {
            Arrays.fill(motDePasse, '\0');
        }
    }

    private static byte[] pbkdf2(char[] motDePasse, byte[] sel, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(motDePasse, sel, iterations, BITS_EMPREINTE);
        try {
            return SecretKeyFactory.getInstance(ALGORITHME).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHME + " indisponible dans cette JVM", e);
        } finally {
            spec.clearPassword();
        }
    }
}
