package My_application.outils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Test;

public class MigrationBaseTest {

    @Test
    public void laCreationDeLaBaseEstDemandeeAuPiloteUneSeuleFois() {
        assertEquals("jdbc:mysql://h/medicale?createDatabaseIfNotExist=true",
                MigrationBase.avecCreationDeLaBase("jdbc:mysql://h/medicale"));
        assertEquals("jdbc:mysql://h/medicale?sslMode=PREFERRED&createDatabaseIfNotExist=true",
                MigrationBase.avecCreationDeLaBase("jdbc:mysql://h/medicale?sslMode=PREFERRED"));
        String deja = "jdbc:mysql://h/medicale?createDatabaseIfNotExist=true";
        assertEquals(deja, MigrationBase.avecCreationDeLaBase(deja));
    }

    @Test
    public void commandeInconnueAfficheLUsage() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(2, MigrationBase.executer(new String[]{"supprimer"}, System.out, new PrintStream(err)));
        assertTrue(err.toString().contains("MigrationBase migrer"));
    }
}
