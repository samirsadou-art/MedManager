package My_application.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MotDePasseTest {

    private static final int RAPIDE = 1000; // itérations réduites : le format et la vérification suffisent ici

    @Test
    public void leBonMotDePasseEstAccepte() {
        String e = MotDePasse.hacher("Cabinet2026".toCharArray(), RAPIDE);
        assertTrue(MotDePasse.verifier("Cabinet2026".toCharArray(), e));
        assertFalse(MotDePasse.verifier("cabinet2026".toCharArray(), e));
    }

    @Test
    public void lEmpreinteNeContientPasLeMotDePasseEtEstSalee() {
        String a = MotDePasse.hacher("Cabinet2026".toCharArray(), RAPIDE);
        String b = MotDePasse.hacher("Cabinet2026".toCharArray(), RAPIDE);
        assertFalse(a.contains("Cabinet2026"));
        assertTrue(a.startsWith("pbkdf2-sha256$" + RAPIDE + "$"));
        assertNotEquals("deux empreintes du même mot de passe diffèrent (sel aléatoire)", a, b);
    }

    @Test
    public void leNombreDIterationsParDefautEstCeluiDeLEmpreinte() {
        assertTrue(MotDePasse.hacher("Cabinet2026".toCharArray()).startsWith("pbkdf2-sha256$" + MotDePasse.ITERATIONS + "$"));
    }

    @Test
    public void uneEmpreinteAbsenteOuIllisibleRefuseLaConnexion() {
        char[] m = "Cabinet2026".toCharArray();
        assertFalse(MotDePasse.verifier(m, null));
        assertFalse(MotDePasse.verifier(m, ""));
        assertFalse(MotDePasse.verifier(m, "Cabinet2026"));
        assertFalse(MotDePasse.verifier(m, "md5$1$abc$def"));
        assertFalse(MotDePasse.verifier(m, "pbkdf2-sha256$xx$abc$def"));
        assertFalse(MotDePasse.verifier(m, "pbkdf2-sha256$10$%%%$def"));
    }

    @Test(expected = MetierException.class)
    public void tropCourt() {
        MotDePasse.verifierRobustesse("Abc123".toCharArray());
    }

    @Test(expected = MetierException.class)
    public void sansChiffre() {
        MotDePasse.verifierRobustesse("Abcdefghij".toCharArray());
    }

    @Test
    public void robusteEtEffacable() {
        char[] m = "medecin2026".toCharArray();
        MotDePasse.verifierRobustesse(m);
        MotDePasse.effacer(m);
        assertArrayEquals(new char[11], m);
    }
}
