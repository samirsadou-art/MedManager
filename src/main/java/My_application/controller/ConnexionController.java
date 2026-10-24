package My_application.controller;

import My_application.entities.User;
import My_application.service.AuthentificationService;
import My_application.service.MotDePasse;

/** Contrôleur de l'écran de connexion ({@code ConnexionForm}) et du changement de mot de passe. */
public class ConnexionController extends ControleurBase {

    private final Vue vue;
    private final AuthentificationService service;

    public ConnexionController(Vue vue, AuthentificationService service) {
        super(vue);
        this.vue = vue;
        this.service = service;
    }

    /**
     * Vrai s'il faut créer le premier compte, faux s'il faut se connecter,
     * null si la base est inaccessible (message déjà affiché).
     */
    public Boolean premiereUtilisation() {
        return tenter(service::premiereUtilisation, null);
    }

    /** Utilisateur dont on crée le compte à la première utilisation ; null si introuvable. */
    public User utilisateurInitial() {
        return tenter(service::utilisateurInitial, null);
    }

    /** Utilisateur connecté, ou null (refus affiché). Le mot de passe est effacé dans tous les cas. */
    public User connecter(String login, char[] motDePasse) {
        try {
            return tenter(() -> service.connecter(login, motDePasse), null);
        } finally {
            MotDePasse.effacer(motDePasse);
        }
    }

    /** Crée le premier compte et connecte l'utilisateur ; null si refusé (message affiché). */
    public User creerPremierCompte(String login, char[] motDePasse, char[] confirmation) {
        try {
            return tenter(() -> service.creerPremierCompte(login, motDePasse, confirmation), null);
        } finally {
            MotDePasse.effacer(motDePasse);
            MotDePasse.effacer(confirmation);
        }
    }

    /**
     * Remplace le mot de passe provisoire et ouvre la session ; null si
     * refusé (message affiché). Les mots de passe sont effacés.
     */
    public User remplacerMotDePasseProvisoire(char[] nouveau, char[] confirmation) {
        try {
            return tenter(() -> service.remplacerMotDePasseProvisoire(nouveau, confirmation), null);
        } finally {
            MotDePasse.effacer(nouveau);
            MotDePasse.effacer(confirmation);
        }
    }

    /** Le membre n'a pas changé son mot de passe provisoire : pas de connexion. */
    public void abandonnerConnexion() {
        service.abandonnerConnexion();
    }

    /** Vrai si le mot de passe a été changé (sinon le refus est affiché). */
    public boolean changerMonMotDePasse(char[] actuel, char[] nouveau, char[] confirmation) {
        try {
            return tenter(() -> {
                service.changerMonMotDePasse(actuel, nouveau, confirmation);
                vue.afficherInformation("Votre mot de passe a été changé.");
                return true;
            }, false);
        } finally {
            MotDePasse.effacer(actuel);
            MotDePasse.effacer(nouveau);
            MotDePasse.effacer(confirmation);
        }
    }
}
