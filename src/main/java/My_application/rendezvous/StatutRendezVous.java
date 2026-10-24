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
 * L'état enregistré (colonne {@code etat}) l'emporte : arrivé, en
 * consultation, terminé, annulé, absent. Un rendez-vous encore « prévu »
 * est coloré selon sa date et le patient pour lequel l'agenda est ouvert.
 */
public enum StatutRendezVous {

    PATIENT_COURANT("Patient du dossier", Theme.SUCCES, Theme.SUCCES_CLAIR),
    AUJOURDHUI("Aujourd'hui", Theme.AVERTISSEMENT, Theme.AVERTISSEMENT_CLAIR),
    A_VENIR("À venir", Theme.PRIMAIRE, Theme.PRIMAIRE_CLAIR),
    ARRIVE("Arrivé, en salle d'attente", Theme.INFO, Theme.INFO_CLAIR),
    EN_CONSULTATION("En consultation", Theme.ACCENT, Theme.ACCENT_CLAIR),
    TERMINE("Terminé", Theme.TEXTE, Theme.FOND),
    PASSE("Passé, non pointé", Theme.TEXTE_SECONDAIRE, Theme.FOND),
    ABSENT("Absent", Theme.DANGER, Theme.DANGER_CLAIR),
    ANNULE("Annulé", Theme.TEXTE_SECONDAIRE, Theme.SURFACE);

    private final String libelle;
    private final Color couleur;
    private final Color fond;

    StatutRendezVous(String libelle, Color couleur, Color fond) {
        this.libelle = libelle;
        this.couleur = couleur;
        this.fond = fond;
    }

    /**
     * Statut du rendez-vous à l'instant donné. Un rendez-vous prévu mais passé
     * reste « passé » même s'il appartient au patient du dossier.
     */
    public static StatutRendezVous de(Rendez_Vous r, patient courant, LocalDateTime maintenant) {
        switch (r.getEtat()) {
            case Rendez_Vous.ETAT_ARRIVE:
                return ARRIVE;
            case Rendez_Vous.ETAT_EN_CONSULTATION:
                return EN_CONSULTATION;
            case Rendez_Vous.ETAT_TERMINE:
                return TERMINE;
            case Rendez_Vous.ETAT_ANNULE:
                return ANNULE;
            case Rendez_Vous.ETAT_ABSENT:
                return ABSENT;
            default:
                break;
        }
        if (r.isPassed(maintenant)) {
            return PASSE;
        }
        if (courant != null && r.getPatient() != null
                && r.getPatient().getIdentPat() == courant.getIdentPat()) {
            return PATIENT_COURANT;
        }
        if (r.getDate().equals(maintenant.toLocalDate())) {
            return AUJOURDHUI;
        }
        return A_VENIR;
    }

    /** Les annulés ne figurent pas dans l'agenda, seulement dans la recherche. */
    public boolean dansLAgenda() {
        return this != ANNULE;
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
