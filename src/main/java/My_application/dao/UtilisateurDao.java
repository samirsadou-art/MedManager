package My_application.dao;

import My_application.entities.Compte;
import My_application.entities.User;
import java.util.List;

public interface UtilisateurDao {

    User findById(int id);

    List<User> findAll();

    /** Compte portant cet identifiant de connexion, ou null. */
    Compte findCompte(String login);

    /**
     * Vrai si au moins un utilisateur a déjà reçu des identifiants, même désactivé :
     * désactiver tous les comptes ne doit pas rouvrir la « première utilisation ».
     */
    boolean existeUnCompte();

    /** Enregistre l'identifiant et l'empreinte du mot de passe de l'utilisateur. */
    void definirIdentifiants(int idUtilisateur, String login, String empreinte);

    void definirActif(int idUtilisateur, boolean actif);
}
