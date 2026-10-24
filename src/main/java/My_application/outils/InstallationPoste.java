package My_application.outils;

import My_application.dao.jdbc.Database;
import My_application.dao.jdbc.DatabaseConfig;
import My_application.dao.jdbc.Migrations;
import My_application.entities.Role;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vérifie qu'un poste du cabinet peut travailler avec la base : à lancer sur
 * le PC de la secrétaire (serveur) puis sur chaque poste médecin, après avoir
 * écrit son {@code gestimed.properties}.
 * <pre>
 *   java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.InstallationPoste
 * </pre>
 * Contrôles : adresse du serveur, compte MySQL (pas root), connexion et son
 * délai, version du serveur, version du schéma, comptes GestiMed par rôle.
 * Chaque problème est expliqué avec la piste à suivre (voir
 * {@code docs/installation-reseau.md}). Code de sortie 0 si tout va bien.
 */
public final class InstallationPoste {

    private static final Pattern HOTE = Pattern.compile("jdbc:[a-z]+://([^:/?]+)(?::(\\d+))?/.*");

    /** Ouverture d'une connexion (remplaçable dans les tests). */
    interface Connexions {
        Connection ouvrir() throws SQLException;
    }

    /** Lecture de l'état des migrations (remplaçable dans les tests). */
    interface Schema {
        Migrations.Etat etat();
    }

    private InstallationPoste() {
    }

    public static void main(String[] args) {
        DatabaseConfig config = DatabaseConfig.charger();
        final Database base = new Database(config);
        int code;
        try {
            code = verifier(config, base::getConnection, new Migrations(config)::etat, System.out);
        } finally {
            base.close();
        }
        System.exit(code);
    }

    static int verifier(DatabaseConfig config, Connexions connexions, Schema schema, PrintStream out) {
        int problemes = 0;
        String hote = hote(config.getUrl());
        out.println("Serveur de la base : " + hote
                + (estLocal(hote) ? " (ce poste est le serveur, PC de la secrétaire)" : " (poste client)"));
        out.println("Compte MySQL : " + config.getUser()
                + (config.getPassword() == null || config.getPassword().isEmpty() ? " (sans mot de passe)" : ""));
        if (config.estCompteAdministrateur()) {
            out.println("  ATTENTION : compte root. Utilisez le compte gestimed (sql/creer_compte_gestimed.sql).");
            problemes++;
        }
        long debut = System.nanoTime();
        try (Connection c = connexions.ouvrir()) {
            long ms = (System.nanoTime() - debut) / 1_000_000;
            out.println("Connexion : réussie en " + ms + " ms");
            out.println("Version du serveur : " + version(c));
            Map<Role, Integer> comptes = comptesParRole(c);
            out.println("Comptes MedManager actifs : " + comptes.get(Role.SECRETAIRE) + " secrétaire(s), "
                    + comptes.get(Role.MEDECIN) + " médecin(s), " + comptes.get(Role.ADMIN) + " administrateur(s)");
            if (comptes.get(Role.ADMIN) == 0) {
                out.println("  À faire : créer le compte administrateur "
                        + "(GestionComptes ajouter admin <identifiant> <nom> <prenom>).");
            }
            if (comptes.get(Role.SECRETAIRE) == 0) {
                out.println("  À faire : créer le compte de la secrétaire "
                        + "(GestionComptes ajouter secretaire <identifiant> <nom> <prenom>).");
            }
        } catch (SQLException | RuntimeException e) {
            out.println("Connexion : ÉCHEC (" + e.getMessage() + ")");
            conseils(hote, out);
            return 1;
        }
        try {
            Migrations.Etat etat = schema.etat();
            out.println("Schéma : " + etat);
            if (etat.estEnRetard() || !etat.estSuivie()) {
                out.println("  À faire sur le PC de la secrétaire : sauvegarde, puis MigrationBase migrer.");
                problemes++;
            }
        } catch (RuntimeException e) {
            out.println("Schéma : lecture impossible (" + e.getMessage() + ")");
            problemes++;
        }
        out.println(problemes == 0 ? "Résultat : poste prêt." : "Résultat : " + problemes + " point(s) à corriger.");
        return problemes == 0 ? 0 : 1;
    }

    private static void conseils(String hote, PrintStream out) {
        if (estLocal(hote)) {
            out.println("  Pistes : MariaDB est-il démarré (service Windows ou panneau XAMPP) ? "
                    + "Le mot de passe de gestimed.properties est-il le bon ?");
        } else {
            out.println("  Pistes : le PC de la secrétaire est-il allumé, et MariaDB démarré ?");
            out.println("           L'adresse " + hote + " est-elle toujours la sienne (IP fixe) ?");
            out.println("           Le pare-feu de ce PC laisse-t-il entrer le port 3306 depuis le réseau du cabinet ?");
            out.println("           my.ini : bind-address n'est-il plus limité à 127.0.0.1 ?");
            out.println("           Le compte gestimed existe-t-il pour l'adresse de ce poste "
                    + "(sql/ouvrir_reseau_local.sql) ?");
        }
    }

    /** Nom ou adresse du serveur dans l'URL JDBC. */
    static String hote(String url) {
        Matcher m = HOTE.matcher(url == null ? "" : url);
        return m.matches() ? m.group(1) : "(adresse illisible : " + url + ")";
    }

    static boolean estLocal(String hote) {
        return "localhost".equalsIgnoreCase(hote) || hote.startsWith("127.") || "::1".equals(hote);
    }

    private static String version(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT VERSION()"); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : "?";
        }
    }

    private static Map<Role, Integer> comptesParRole(Connection c) throws SQLException {
        Map<Role, Integer> n = new EnumMap<>(Role.class);
        for (Role r : Role.values()) {
            n.put(r, 0);
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT role, COUNT(*) AS nombre FROM utilisateur "
                     + "WHERE actif = 1 AND login IS NOT NULL GROUP BY role");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                n.put(Role.depuis(rs.getString(1)), rs.getInt(2));
            }
        }
        return n;
    }
}
