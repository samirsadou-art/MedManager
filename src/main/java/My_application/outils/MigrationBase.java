package My_application.outils;

import My_application.dao.DataAccessException;
import My_application.dao.jdbc.DatabaseConfig;
import My_application.dao.jdbc.Migrations;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Création et mise à jour du schéma de la base, en ligne de commande.
 * <p>
 * Utilise l'adresse de la base de {@code gestimed.properties}, mais un compte
 * administrateur : le compte {@code gestimed} de l'application ne peut pas
 * modifier les tables.
 * <pre>
 *   java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.MigrationBase etat
 *   ... MigrationBase migrer [compteAdmin]      (root par défaut ; mot de passe demandé au clavier)
 * </pre>
 * {@code migrer} crée la base si elle n'existe pas, adopte une base créée
 * avant les migrations, puis applique les scripts {@code db/migration} en
 * attente. Faire une sauvegarde (mysqldump) avant de migrer une base réelle.
 */
public final class MigrationBase {

    private MigrationBase() {
    }

    public static void main(String[] args) {
        System.exit(executer(args, System.out, System.err));
    }

    static int executer(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || !("etat".equals(args[0]) || "migrer".equals(args[0]))) {
            err.println("Usage : MigrationBase etat");
            err.println("        MigrationBase migrer [compteAdmin]   (root par défaut)");
            return 2;
        }
        DatabaseConfig config = DatabaseConfig.charger();
        try {
            if ("etat".equals(args[0])) {
                out.println(new Migrations(config).etat());
                return 0;
            }
            String admin = args.length > 1 ? args[1] : "root";
            char[] mdp = lireMotDePasse("Mot de passe MySQL de " + admin + " (vide si aucun) : ", out);
            try {
                Migrations migrations = new Migrations(avecCreationDeLaBase(config.getUrl()), admin,
                        new String(mdp));
                int n = migrations.migrer();
                out.println(n == 0 ? "Base déjà à jour." : n + " migration(s) appliquée(s).");
                out.println(migrations.etat());
                return 0;
            } finally {
                Arrays.fill(mdp, '\0');
            }
        } catch (DataAccessException e) {
            err.println(e.getMessage());
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                err.println(e.getCause().getMessage());
            }
            return 1;
        }
    }

    /** Ajoute createDatabaseIfNotExist=true : une installation neuve part d'un serveur vide. */
    static String avecCreationDeLaBase(String url) {
        if (url.contains("createDatabaseIfNotExist")) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + "createDatabaseIfNotExist=true";
    }

    private static char[] lireMotDePasse(String invite, PrintStream out) {
        Console console = System.console();
        if (console != null) {
            char[] lu = console.readPassword(invite);
            return lu == null ? new char[0] : lu;
        }
        // Lancement depuis un IDE : pas de console, la saisie reste visible.
        out.print(invite);
        out.flush();
        try {
            String ligne = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset())).readLine();
            return ligne == null ? new char[0] : ligne.toCharArray();
        } catch (IOException e) {
            return new char[0];
        }
    }
}
