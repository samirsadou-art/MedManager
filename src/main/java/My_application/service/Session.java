package My_application.service;

import My_application.dao.DAOFactory;
import My_application.entities.Role;
import My_application.entities.User;
import My_application.entities.medecin;
import java.util.Locale;

/**
 * Utilisateur connecté et médecin traitant.
 * <p>
 * L'utilisateur est fixé par l'écran de connexion ({@link #ouvrir(User)}).
 * Le médecin traitant est celui rattaché à l'utilisateur
 * ({@code utilisateur.idMedecin}), sinon celui donné au lancement
 * ({@code -Dgestimed.medecin}, 1 par défaut).
 * <p>
 * Tant qu'aucune session n'est ouverte (tests, outils), {@link #getUtilisateur()}
 * renvoie l'utilisateur {@code idUtilisateur} par défaut, mais
 * {@link #getIdUtilisateurConnecte()} renvoie null : le journal des accès ne
 * l'attribue à personne.
 * <p>
 * Le rôle de l'utilisateur connecté ({@link #getRole()}) limite ce qu'il peut
 * faire : les services appellent {@link #exiger(Role...)}. Sans session
 * ouverte (outils en ligne de commande lancés par l'administrateur du poste,
 * tests), aucun rôle n'est exigé.
 */
public class Session implements ControleAcces {

    private final DAOFactory daos;
    private final int idUtilisateur;
    private int idMedecin;
    private User utilisateur;
    private User connecte;
    private medecin medecinTraitant;

    public Session(DAOFactory daos, int idUtilisateur, int idMedecin) {
        this.daos = daos;
        this.idUtilisateur = idUtilisateur;
        this.idMedecin = idMedecin;
    }

    /** Médecin du poste : celui de l'utilisateur connecté, sinon celui donné au lancement. */
    public synchronized int getIdMedecin() {
        return idMedecin;
    }

    /**
     * Médecin avec lequel ce poste prend les rendez-vous : celui du compte
     * connecté s'il est médecin, null pour un autre compte (secrétaire,
     * administrateur), qui doit choisir le médecin. Sans session ouverte
     * (outils), le médecin donné au lancement.
     */
    public synchronized Integer getIdMedecinDuPoste() {
        if (connecte != null && connecte.getRole() != Role.MEDECIN) {
            return null;
        }
        return idMedecin;
    }

    /** Ouvre la session de l'utilisateur authentifié. */
    public synchronized void ouvrir(User u) {
        connecte = u;
        utilisateur = u;
        if (u.getIdMedecin() != null && u.getIdMedecin() != idMedecin) {
            idMedecin = u.getIdMedecin();
            medecinTraitant = null;
        }
    }

    /** Rôle de l'utilisateur connecté, ou null si personne n'est connecté. */
    public synchronized Role getRole() {
        return connecte == null ? null : connecte.getRole();
    }

    @Override
    public synchronized void exiger(Role... roles) {
        if (connecte == null) {
            return;
        }
        StringBuilder permis = new StringBuilder();
        for (Role r : roles) {
            if (r == connecte.getRole()) {
                return;
            }
            permis.append(permis.length() == 0 ? "" : ", ").append(r.getLibelle().toLowerCase(Locale.FRENCH));
        }
        throw new MetierException("Cette action est réservée aux comptes : " + permis + ".\n"
                + "Vous êtes connecté avec un compte « " + connecte.getRole().getLibelle() + " ».");
    }

    public synchronized boolean estOuverte() {
        return connecte != null;
    }

    /** Identifiant de l'utilisateur authentifié, ou null si personne n'est connecté. */
    public synchronized Integer getIdUtilisateurConnecte() {
        return connecte == null ? null : connecte.getIdent();
    }

    /** Utilisateur proposé à la première utilisation (avant qu'aucun compte n'existe). */
    public int getIdUtilisateurParDefaut() {
        return idUtilisateur;
    }

    public synchronized User getUtilisateur() {
        if (utilisateur == null) {
            utilisateur = daos.getUtilisateurDao().findById(idUtilisateur);
            if (utilisateur == null) {
                utilisateur = new User(idUtilisateur, "", "", "");
            }
        }
        return utilisateur;
    }

    public synchronized medecin getMedecinTraitant() {
        if (medecinTraitant == null) {
            medecinTraitant = daos.getMedecinDao().findById(idMedecin);
            if (medecinTraitant == null) {
                throw new MetierException("Le médecin n° " + idMedecin + " est introuvable dans la base.");
            }
        }
        return medecinTraitant;
    }
}
