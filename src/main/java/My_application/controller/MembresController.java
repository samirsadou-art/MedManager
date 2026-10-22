package My_application.controller;

import My_application.service.MembresService;

/** Liste des membres du cabinet, pour le compte administrateur. */
public class MembresController extends ControleurBase {

    private final MembresVue vue;
    private final MembresService membresService;

    public MembresController(MembresVue vue, MembresService membresService) {
        super(vue);
        this.vue = vue;
        this.membresService = membresService;
    }

    public void rafraichir() {
        tenter(() -> vue.afficherMembres(membresService.lister()));
    }
}
