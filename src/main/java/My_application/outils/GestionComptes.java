package My_application.outils;

import My_application.AppContext;
import My_application.dao.DataAccessException;
import My_application.entities.EntreeJournal;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.service.AuthentificationService;
import My_application.service.MembresService;
import My_application.service.MetierException;
import My_application.service.MotDePasse;
import java.io.Console;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Gestion des comptes en ligne de commande, pour l'administrateur du cabinet.
 * <p>
 * Utilise la même configuration que l'application ({@code gestimed.properties}).
 * <pre>
 *   java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.GestionComptes liste
 *   ... GestionComptes ajouter &lt;role&gt; &lt;identifiant&gt; &lt;nom&gt; &lt;prenom&gt; [idMedecin]
 *                                     (role : secretaire, medecin ou admin ; mot de passe demandé au clavier)
 *   ... GestionComptes role &lt;idUtilisateur&gt; &lt;role&gt; [idMedecin]
 *   ... GestionComptes definir &lt;idUtilisateur&gt; &lt;identifiant&gt;   (mot de passe demandé au clavier)
 *   ... GestionComptes desactiver &lt;idUtilisateur&gt;
 *   ... GestionComptes activer &lt;idUtilisateur&gt;
 *   ... GestionComptes journal &lt;idPatient&gt;                    (qui a accédé au dossier)
 * </pre>
 * {@code ajouter} crée le membre et ses identifiants en une fois (par exemple
 * les comptes « secretaire » et « admin » du PC de la secrétaire). Un compte
 * médecin indique sa fiche {@code medecin} ({@code idMedecin}).
 */
public final class GestionComptes {

    private GestionComptes() {
    }

    public static void main(String[] args) {
        int code;
        try (AppContext ctx = AppContext.mysql()) {
            code = executer(args, ctx, System.out, System.err, CONSOLE);
        }
        System.exit(code);
    }

    static int executer(String[] args, AppContext ctx, PrintStream out, PrintStream err) {
        return executer(args, ctx, out, err, CONSOLE);
    }

    static int executer(String[] args, AppContext ctx, PrintStream out, PrintStream err, Clavier clavier) {
        if (args.length == 0) {
            usage(err);
            return 2;
        }
        AuthentificationService service = ctx.getAuthentificationService();
        MembresService membres = ctx.getMembresService();
        try {
            switch (args[0]) {
                case "liste":
                    for (User u : ctx.getDaos().getUtilisateurDao().findAll()) {
                        out.printf("%4d  %-30s %-15s %-20s %s%n", u.getIdent(), u, u.getRole().getLibelle(),
                                u.getLogin() == null ? "(aucun)" : u.getLogin(), u.isActif() ? "actif" : "désactivé");
                    }
                    return 0;
                case "ajouter": {
                    exiger(args, 5, err);
                    Role role = role(args[1]);
                    Integer idMedecin = args.length > 5 ? Integer.valueOf(args[5]) : null;
                    char[][] saisie = lireMotDePasse(clavier, err);
                    if (saisie == null) {
                        return 1;
                    }
                    try {
                        // Identifiant et mot de passe vérifiés avant de créer le membre.
                        service.verifierNouveauxIdentifiants(args[2], saisie[0], saisie[1]);
                        User u = membres.ajouter(args[3], args[4], role, idMedecin);
                        service.definirIdentifiants(u.getIdent(), args[2], saisie[0], saisie[1]);
                        out.println("Membre n° " + u.getIdent() + " (" + u + ", " + role.getLibelle()
                                + ") créé avec l'identifiant " + args[2].trim().toLowerCase(Locale.ROOT) + ".");
                    } finally {
                        MotDePasse.effacer(saisie[0]);
                        MotDePasse.effacer(saisie[1]);
                    }
                    return 0;
                }
                case "role":
                    exiger(args, 3, err);
                    membres.definirRole(Integer.parseInt(args[1]), role(args[2]),
                            args.length > 3 ? Integer.valueOf(args[3]) : null);
                    out.println("Rôle enregistré.");
                    return 0;
                case "definir": {
                    exiger(args, 3, err);
                    char[][] saisie = lireMotDePasse(clavier, err);
                    if (saisie == null) {
                        return 1;
                    }
                    try {
                        service.definirIdentifiants(Integer.parseInt(args[1]), args[2], saisie[0], saisie[1]);
                    } finally {
                        MotDePasse.effacer(saisie[0]);
                        MotDePasse.effacer(saisie[1]);
                    }
                    out.println("Identifiants enregistrés.");
                    return 0;
                }
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

    /** Lecture du mot de passe sans écho ; remplaçable dans les tests. */
    interface Clavier {
        /** Mot de passe saisi, ou null si aucune console n'est disponible. */
        char[] motDePasse(String invite);
    }

    static final Clavier CONSOLE = new Clavier() {
        @Override
        public char[] motDePasse(String invite) {
            Console console = System.console();
            return console == null ? null : console.readPassword(invite);
        }
    };

    /** {mot de passe, confirmation}, ou null (message affiché) sans console. */
    private static char[][] lireMotDePasse(Clavier clavier, PrintStream err) {
        char[] mdp = clavier.motDePasse("Nouveau mot de passe : ");
        if (mdp == null) {
            err.println("Pas de console : lancez la commande depuis une invite de commandes.");
            return null;
        }
        return new char[][] {mdp, clavier.motDePasse("Confirmation : ")};
    }

    private static Role role(String code) {
        try {
            return Role.depuis(code);
        } catch (IllegalArgumentException e) {
            throw new MetierException(e.getMessage());
        }
    }

    private static void exiger(String[] args, int n, PrintStream err) {
        if (args.length < n) {
            throw new MetierException("Arguments manquants. Lancez la commande sans argument pour l'aide.");
        }
    }

    private static void usage(PrintStream err) {
        err.println("Usage : GestionComptes liste\n"
                + "  | ajouter <secretaire|medecin|admin> <identifiant> <nom> <prenom> [idMedecin]\n"
                + "  | role <idUtilisateur> <secretaire|medecin|admin> [idMedecin]\n"
                + "  | definir <idUtilisateur> <identifiant>\n"
                + "  | desactiver <idUtilisateur> | activer <idUtilisateur> | journal <idPatient>");
    }
}
