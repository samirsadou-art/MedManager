package My_application.controller;

import My_application.entities.User;
import My_application.entities.medecin;
import java.util.List;

/** Page des membres du cabinet (compte administrateur). */
public interface MembresVue extends Vue {

    /** Comptes du cabinet, et toutes les fiches médecin (pour nommer le médecin rattaché). */
    void afficherMembres(List<User> membres, List<medecin> medecins);
}
