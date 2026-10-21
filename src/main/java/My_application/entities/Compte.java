package My_application.entities;

/**
 * Identifiants de connexion d'un utilisateur : l'utilisateur et l'empreinte
 * de son mot de passe (jamais le mot de passe lui-même).
 * <p>
 * Séparé de {@link User} pour que l'empreinte ne circule pas dans les vues
 * avec l'utilisateur connecté.
 */
public class Compte {

    private final User utilisateur;
    private final String empreinte;

    public Compte(User utilisateur, String empreinte) {
        this.utilisateur = utilisateur;
        this.empreinte = empreinte;
    }

    public User getUtilisateur() {
        return utilisateur;
    }

    /** Empreinte PBKDF2 du mot de passe (voir {@code service.MotDePasse}), ou null. */
    public String getEmpreinte() {
        return empreinte;
    }
}
