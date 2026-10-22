package My_application.entities;

import java.util.Locale;

/**
 * Rôle d'un compte (colonne {@code utilisateur.role}, migration V10). Il
 * décide de l'écran d'accueil et des actions permises :
 * <ul>
 * <li>{@link #SECRETAIRE} : patients (fiche administrative), rendez-vous,
 * salle d'attente, messagerie ; jamais le dossier médical ;</li>
 * <li>{@link #MEDECIN} : tout ce qui touche au dossier médical ;</li>
 * <li>{@link #ADMIN} : comptes des membres et statistiques du cabinet ;
 * jamais le dossier médical.</li>
 * </ul>
 */
public enum Role {

    SECRETAIRE("Secrétaire"),
    MEDECIN("Médecin"),
    ADMIN("Administrateur");

    private final String libelle;

    Role(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /**
     * Rôle à partir de son code en base ou saisi en ligne de commande
     * (« secretaire », « MEDECIN », « admin »…).
     *
     * @throws IllegalArgumentException si le code ne désigne aucun rôle
     */
    public static Role depuis(String code) {
        String c = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        for (Role r : values()) {
            if (r.name().equals(c)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Rôle inconnu : « " + code + " » (attendu : secretaire, medecin ou admin).");
    }
}
