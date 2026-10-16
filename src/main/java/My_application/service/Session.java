package My_application.service;

import My_application.dao.DAOFactory;
import My_application.entities.User;
import My_application.entities.medecin;

/**
 * Utilisateur connecté et médecin traitant.
 * <p>
 * L'ancien code codait en dur {@code medecin.chargerFromDB(1)} et
 * {@code new User()} (« SAHAB SAID », id 1) à plusieurs endroits. Les
 * identifiants sont maintenant centralisés ici (en attendant un véritable
 * écran de connexion).
 */
public class Session {

    private final DAOFactory daos;
    private final int idUtilisateur;
    private final int idMedecin;
    private User utilisateur;
    private medecin medecinTraitant;

    public Session(DAOFactory daos, int idUtilisateur, int idMedecin) {
        this.daos = daos;
        this.idUtilisateur = idUtilisateur;
        this.idMedecin = idMedecin;
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
