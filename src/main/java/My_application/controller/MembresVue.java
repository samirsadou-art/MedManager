package My_application.controller;

import My_application.entities.User;
import java.util.List;

/** Page des membres du cabinet (compte administrateur). */
public interface MembresVue extends Vue {

    void afficherMembres(List<User> membres);
}
