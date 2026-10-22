package My_application.controller;

import My_application.entities.Role;
import My_application.entities.User;
import My_application.entities.medecin;
import My_application.service.MembresService;
import My_application.service.MotDePasse;
import java.util.List;

/**
 * Page des membres du cabinet, pour le compte administrateur : comptes de
 * connexion et fiches médecin. Les méthodes qui modifient renvoient vrai si
 * c'est fait (la liste est alors relue), faux si c'est refusé (message
 * affiché, la boîte de saisie reste ouverte).
 */
public class MembresController extends ControleurBase {

    private final MembresVue vue;
    private final MembresService membresService;

    public MembresController(MembresVue vue, MembresService membresService) {
        super(vue);
        this.vue = vue;
        this.membresService = membresService;
    }

    public void rafraichir() {
        tenter(() -> {
            List<User> membres = membresService.lister();
            vue.afficherMembres(membres, membresService.medecins());
        });
    }

    /** Nouveau membre, avec identifiant et mot de passe provisoire ; {@code nouvelleFiche} pour un nouveau médecin. */
    public boolean creerCompte(String nom, String prenom, Role role, Integer idMedecin, medecin nouvelleFiche,
            String login, char[] motDePasse, char[] confirmation) {
        try {
            return fait(() -> {
                User u = membresService.creerCompte(nom, prenom, role, idMedecin, nouvelleFiche, login,
                        motDePasse, confirmation);
                vue.afficherInformation("Le compte de " + u + " est créé (identifiant « " + u.getLogin() + " »).\n"
                        + "Il choisira son mot de passe à sa première connexion.");
            });
        } finally {
            MotDePasse.effacer(motDePasse);
            MotDePasse.effacer(confirmation);
        }
    }

    public boolean modifier(int idUtilisateur, String nom, String prenom, Role role, Integer idMedecin) {
        return fait(() -> membresService.modifier(idUtilisateur, nom, prenom, role, idMedecin));
    }

    public boolean motDePasseProvisoire(User u, char[] motDePasse, char[] confirmation) {
        try {
            return fait(() -> {
                membresService.motDePasseProvisoire(u.getIdent(), motDePasse, confirmation);
                vue.afficherInformation("Mot de passe provisoire enregistré : " + u
                        + " devra le changer à sa prochaine connexion.");
            });
        } finally {
            MotDePasse.effacer(motDePasse);
            MotDePasse.effacer(confirmation);
        }
    }

    public boolean definirActif(User u, boolean actif) {
        return fait(() -> membresService.definirActif(u.getIdent(), actif));
    }

    public boolean modifierMedecin(medecin m) {
        return fait(() -> membresService.modifierMedecin(m));
    }

    public boolean definirMedecinActif(medecin m, boolean actif) {
        return fait(() -> membresService.definirMedecinActif(m.getIdmedecin(), actif));
    }

    private boolean fait(Runnable action) {
        boolean ok = tenter(action);
        if (ok) {
            rafraichir();
        }
        return ok;
    }
}
