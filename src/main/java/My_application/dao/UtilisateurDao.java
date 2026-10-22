package My_application.dao;

import My_application.entities.Compte;
import My_application.entities.Role;
import My_application.entities.User;
import java.time.LocalDateTime;
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

    /** Ajoute un membre (sans identifiants) ; renvoie l'utilisateur avec son identifiant. */
    User insert(User u);

    /** Change le rôle et le médecin rattaché (null pour une secrétaire ou un administrateur). */
    void definirRole(int idUtilisateur, Role role, Integer idMedecin);

    /** Date et heure de la dernière connexion réussie. */
    void noterConnexion(int idUtilisateur, LocalDateTime quand);
}
