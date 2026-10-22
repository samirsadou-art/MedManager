package My_application.service;

import My_application.entities.Role;

/**
 * Vérification du rôle de l'utilisateur connecté, faite par les services
 * eux-mêmes : masquer un bouton ne suffit pas à protéger le secret médical.
 */
public interface ControleAcces {

    /**
     * Ne fait rien si le rôle de l'utilisateur connecté fait partie de
     * {@code roles}.
     *
     * @throws MetierException sinon (message affichable)
     */
    void exiger(Role... roles);

    /** Aucun contrôle (tests des services qui ne portent pas sur les rôles). */
    ControleAcces AUCUN = new ControleAcces() {
        @Override
        public void exiger(Role... roles) {
            // rien
        }
    };
}
