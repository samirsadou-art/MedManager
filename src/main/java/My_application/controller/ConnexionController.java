package My_application.controller;

import My_application.entities.User;
import My_application.service.AuthentificationService;
import My_application.service.MotDePasse;

/** Contrôleur de l'écran de connexion ({@code ConnexionForm}). */
public class ConnexionController extends ControleurBase {

    private final AuthentificationService service;

    public ConnexionController(Vue vue, AuthentificationService service) {
        super(vue);
        this.service = service;
    }

    /**
     * Vrai s'il faut créer le premier compte, faux s'il faut se connecter,
     * null si la base est inaccessible (message déjà affiché).
     */
    public Boolean premiereUtilisation() {
        final Boolean[] r = new Boolean[1];
        tenter(new Runnable() {
            @Override
            public void run() {
                r[0] = service.premiereUtilisation();
            }
        });
        return r[0];
    }

    /** Utilisateur dont on crée le compte à la première utilisation ; null si introuvable. */
    public User utilisateurInitial() {
        final User[] r = new User[1];
        tenter(new Runnable() {
            @Override
            public void run() {
                r[0] = service.utilisateurInitial();
            }
        });
        return r[0];
    }

    /** Utilisateur connecté, ou null (refus affiché). Le mot de passe est effacé dans tous les cas. */
    public User connecter(final String login, final char[] motDePasse) {
        final User[] r = new User[1];
        try {
            tenter(new Runnable() {
                @Override
                public void run() {
                    r[0] = service.connecter(login, motDePasse);
                }
            });
        } finally {
            MotDePasse.effacer(motDePasse);
        }
        return r[0];
    }

    /** Crée le premier compte et connecte l'utilisateur ; null si refusé (message affiché). */
    public User creerPremierCompte(final String login, final char[] motDePasse, final char[] confirmation) {
        final User[] r = new User[1];
        try {
            tenter(new Runnable() {
                @Override
                public void run() {
                    r[0] = service.creerPremierCompte(login, motDePasse, confirmation);
                }
            });
        } finally {
            MotDePasse.effacer(motDePasse);
            MotDePasse.effacer(confirmation);
        }
        return r[0];
    }
}
