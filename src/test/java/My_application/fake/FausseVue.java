package My_application.fake;

import My_application.controller.Vue;
import java.util.ArrayList;
import java.util.List;

/** Vue factice : enregistre les messages au lieu d'ouvrir des boîtes de dialogue. */
public class FausseVue implements Vue {

    public final List<String> erreurs = new ArrayList<>();
    public final List<String> informations = new ArrayList<>();

    @Override
    public void afficherErreur(String message) {
        erreurs.add(message);
    }

    @Override
    public void afficherInformation(String message) {
        informations.add(message);
    }
}
