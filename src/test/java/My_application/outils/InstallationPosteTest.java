package My_application.outils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import My_application.dao.jdbc.DatabaseConfig;
import My_application.fake.FauxJdbc;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import org.junit.Test;

/** Vérification d'un poste du cabinet, sans serveur réel. */
public class InstallationPosteTest {

    private final ByteArrayOutputStream sortie = new ByteArrayOutputStream();

    private String texte() {
        return new String(sortie.toByteArray(), StandardCharsets.UTF_8);
    }

    private PrintStream out() throws Exception {
        return new PrintStream(sortie, true, "UTF-8");
    }

    private static DatabaseConfig config(String url, String user) {
        return new DatabaseConfig(url, user, "secret", "com.mysql.cj.jdbc.Driver");
    }

    @Test
    public void adresseDuServeurLueDansLUrl() {
        assertEquals("192.168.1.10", InstallationPoste.hote("jdbc:mysql://192.168.1.10:3306/medicale?sslMode=PREFERRED"));
        assertEquals("localhost", InstallationPoste.hote("jdbc:mysql://localhost/medicale"));
        assertTrue(InstallationPoste.estLocal("localhost"));
        assertTrue(InstallationPoste.estLocal("127.0.0.1"));
        assertFalse(InstallationPoste.estLocal("192.168.1.10"));
        assertTrue(InstallationPoste.hote("n'importe quoi").contains("illisible"));
    }

    @Test
    public void serveurInjoignableDepuisUnPosteMedecin() throws Exception {
        int code = InstallationPoste.verifier(config("jdbc:mysql://192.168.1.10:3306/medicale", "gestimed"),
                () -> {
                    throw new SQLException("Communications link failure");
                }, () -> {
                    throw new IllegalStateException("non appelé");
                }, out());
        assertEquals(1, code);
        String t = texte();
        assertTrue(t, t.contains("poste client"));
        assertTrue(t, t.contains("ÉCHEC"));
        assertTrue(t, t.contains("pare-feu"));
        assertTrue(t, t.contains("ouvrir_reseau_local.sql"));
    }

    @Test
    public void connexionReussieMaisCompteRootEtSchemaIllisible() throws Exception {
        FauxJdbc base = new FauxJdbc();
        base.quand("SELECT VERSION()", FauxJdbc.ligne("v", "10.4.32-MariaDB"));
        base.quand("GROUP BY role", FauxJdbc.ligne("role", "MEDECIN", "nombre", 2),
                FauxJdbc.ligne("role", "SECRETAIRE", "nombre", 1));
        int code = InstallationPoste.verifier(config("jdbc:mysql://localhost:3306/medicale", "root"),
                base::getConnection, () -> {
                    throw new IllegalStateException("Flyway indisponible");
                }, out());
        assertEquals(1, code);
        String t = texte();
        assertTrue(t, t.contains("ce poste est le serveur"));
        assertTrue(t, t.contains("compte root"));
        assertTrue(t, t.contains("10.4.32-MariaDB"));
        assertTrue(t, t.contains("1 secrétaire(s), 2 médecin(s), 0 administrateur(s)"));
        assertTrue(t, t.contains("créer le compte administrateur"));
        assertTrue(t, t.contains("lecture impossible"));
        assertEquals("statements et résultats fermés", 0, base.statementsOuverts + base.resultSetsOuverts);
    }
}
