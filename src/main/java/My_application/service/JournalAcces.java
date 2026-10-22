package My_application.service;

/**
 * Traçabilité des accès au dossier médical : chaque ouverture ou
 * modification d'un dossier est enregistrée avec l'utilisateur connecté.
 * <p>
 * Une écriture du journal qui échoue fait échouer l'action elle-même : un
 * accès au dossier ne doit pas pouvoir échapper à la trace.
 */
public interface JournalAcces {

    /** Ce qui est enregistré dans {@code journal_acces.action}. */
    enum Action {
        CONNEXION,
        ECHEC_CONNEXION,
        CREATION_COMPTE,
        MODIFICATION_COMPTE,
        OUVERTURE_DOSSIER,
        CREATION_PATIENT,
        MODIFICATION_PATIENT,
        ENREGISTREMENT_CONSULTATION,
        SAISIE_RESULTATS,
        RENDEZ_VOUS_PRIS,
        RENDEZ_VOUS_ANNULE,
        RENDEZ_VOUS_DEPLACE,
        RENDEZ_VOUS_ABSENT
    }

    /**
     * Ajoute une entrée au nom de l'utilisateur connecté.
     *
     * @param idPatient dossier concerné, ou null
     * @param detail    précision courte (tronquée à 255 caractères), ou null
     */
    void noter(Action action, Integer idPatient, String detail);

    /** Journal qui n'enregistre rien (tests des services qui ne portent pas sur la traçabilité). */
    JournalAcces AUCUN = new JournalAcces() {
        @Override
        public void noter(Action action, Integer idPatient, String detail) {
            // rien
        }
    };
}
