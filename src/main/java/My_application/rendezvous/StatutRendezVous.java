package My_application.rendezvous;

import My_application.entities.Rendez_Vous;
import My_application.entities.patient;
import My_application.vue.Theme;
import java.awt.Color;
import java.time.LocalDateTime;

/**
 * Statut d'affichage d'un rendez-vous dans l'agenda : il fixe la couleur de
 * sa carte et son libellé dans la légende.
 * <p>
 * La base n'enregistre pas d'état exploitable (la colonne {@code etat} vaut
 * toujours 0) : le statut est déduit de la date du rendez-vous et du patient
 * pour lequel l'agenda est ouvert.
 */
public enum StatutRendezVous {

    PATIENT_COURANT("Patient du dossier", Theme.SUCCES, Theme.SUCCES_CLAIR),
    AUJOURDHUI("Aujourd'hui", Theme.AVERTISSEMENT, Theme.AVERTISSEMENT_CLAIR),
    A_VENIR("À venir", Theme.PRIMAIRE, Theme.PRIMAIRE_CLAIR),
    PASSE("Passé", Theme.TEXTE_SECONDAIRE, Theme.FOND);

    private final String libelle;
    private final Color couleur;
    private final Color fond;

    StatutRendezVous(String libelle, Color couleur, Color fond) {
        this.libelle = libelle;
        this.couleur = couleur;
        this.fond = fond;
    }

    /**
     * Statut du rendez-vous à l'instant donné. Un rendez-vous passé reste
     * « passé » même s'il appartient au patient du dossier.
     */
    public static StatutRendezVous de(Rendez_Vous r, patient courant, LocalDateTime maintenant) {
        if (r.isPassed(maintenant)) {
            return PASSE;
        }
        if (courant != null && r.getPatient() != null
                && r.getPatient().getIdentPat() == courant.getIdentPat()) {
            return PATIENT_COURANT;
        }
        if (r.getDate().toLocalDate().equals(maintenant.toLocalDate())) {
            return AUJOURDHUI;
        }
        return A_VENIR;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Couleur d'accent : pastille, horaire, bordure. */
    public Color getCouleur() {
        return couleur;
    }

    /** Fond de la carte. */
    public Color getFond() {
        return fond;
    }
}
