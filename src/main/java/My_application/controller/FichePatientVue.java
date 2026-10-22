package My_application.controller;

import My_application.entities.patient;
import java.util.List;

/** Fiche administrative d'un patient (inscription ou modification). */
public interface FichePatientVue extends Vue {

    /** Réponse de {@link #proposerDoublons(List)} : créer la nouvelle fiche malgré tout. */
    int CREER_QUAND_MEME = -1;
    /** Réponse de {@link #proposerDoublons(List)} : ne rien enregistrer. */
    int ANNULER = -2;

    /**
     * Montre les fiches qui ressemblent au patient saisi.
     *
     * @return l'indice de la fiche existante choisie, {@link #CREER_QUAND_MEME}
     *         ou {@link #ANNULER}
     */
    int proposerDoublons(List<patient> doublons);
}
