package My_application.outils;

import My_application.AppContext;
import My_application.dao.DataAccessException;
import My_application.entities.EntreeJournal;
import My_application.entities.User;
import My_application.service.AuthentificationService;
import My_application.service.MetierException;
import My_application.service.MotDePasse;
import java.io.Console;
import java.io.PrintStream;

/**
 * Gestion des comptes en ligne de commande, pour l'administrateur du cabinet.
 * <p>
 * Utilise la même configuration que l'application ({@code gestimed.properties}).
 * <pre>
 *   java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.GestionComptes liste
 *   ... GestionComptes definir &lt;idUtilisateur&gt; &lt;identifiant&gt;   (mot de passe demandé au clavier)
 *   ... GestionComptes desactiver &lt;idUtilisateur&gt;
 *   ... GestionComptes activer &lt;idUtilisateur&gt;
 *   ... GestionComptes journal &lt;idPatient&gt;                    (qui a accédé au dossier)
 * </pre>
 * Les nouveaux utilisateurs s'ajoutent d'abord dans la table {@code utilisateur}
 * (nom, prénom, qualité, idMedecin), puis reçoivent leurs identifiants ici.
 */
public final class GestionComptes {

    private GestionComptes() {
    }

    public static void main(String[] args) {
        int code;
        try (AppContext ctx = AppContext.mysql()) {
            code = executer(args, ctx, System.out, System.err);
        }
        System.exit(code);
    }

    static int executer(String[] args, AppContext ctx, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            usage(err);
            return 2;
        }
        AuthentificationService service = ctx.getAuthentificationService();
        try {
            switch (args[0]) {
                case "liste":
                    for (User u : ctx.getDaos().getUtilisateurDao().findAll()) {
                        out.printf("%4d  %-30s %-20s %s%n", u.getIdent(), u, u.getLogin() == null ? "(aucun)" : u.getLogin(),
                                u.isActif() ? "actif" : "désactivé");
                    }
                    return 0;
                case "definir":
                    exiger(args, 3, err);
                    Console console = System.console();
                    if (console == null) {
                        err.println("Pas de console : lancez la commande depuis une invite de commandes.");
                        return 1;
                    }
                    char[] mdp = console.readPassword("Nouveau mot de passe : ");
                    char[] confirmation = console.readPassword("Confirmation : ");
                    try {
                        service.definirIdentifiants(Integer.parseInt(args[1]), args[2], mdp, confirmation);
                    } finally {
                        MotDePasse.effacer(mdp);
                        MotDePasse.effacer(confirmation);
                    }
                    out.println("Identifiants enregistrés.");
                    return 0;
                case "desactiver":
                case "activer":
                    exiger(args, 2, err);
                    service.definirActif(Integer.parseInt(args[1]), "activer".equals(args[0]));
                    out.println("Compte " + ("activer".equals(args[0]) ? "activé." : "désactivé."));
                    return 0;
                case "journal":
                    exiger(args, 2, err);
                    for (EntreeJournal e : ctx.getDaos().getJournalDao()
                            .findByPatient(Integer.parseInt(args[1]))) {
                        out.printf("%s  utilisateur %-5s %-28s %s  %s%n", e.getDateHeure(),
                                e.getIdUtilisateur() == null ? "-" : e.getIdUtilisateur(), e.getAction(),
                                e.getPoste() == null ? "" : e.getPoste(), e.getDetail() == null ? "" : e.getDetail());
                    }
                    return 0;
                default:
                    usage(err);
                    return 2;
            }
        } catch (NumberFormatException e) {
            err.println("Identifiant numérique attendu : " + e.getMessage());
            return 2;
        } catch (MetierException e) {
            err.println(e.getMessage());
            return 1;
        } catch (DataAccessException e) {
            err.println("Erreur d'accès à la base de données : " + e.getMessage());
            return 1;
        }
    }

    private static void exiger(String[] args, int n, PrintStream err) {
        if (args.length < n) {
            throw new MetierException("Arguments manquants. Lancez la commande sans argument pour l'aide.");
        }
    }

    private static void usage(PrintStream err) {
        err.println("Usage : GestionComptes liste | definir <idUtilisateur> <identifiant> | "
                + "desactiver <idUtilisateur> | activer <idUtilisateur> | journal <idPatient>");
    }
}
