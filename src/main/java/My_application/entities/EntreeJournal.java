package My_application.entities;

import java.time.LocalDateTime;

/** Une ligne du journal des accès : qui a fait quoi, sur quel dossier, et quand. */
public class EntreeJournal {

    private final LocalDateTime dateHeure;
    private final Integer idUtilisateur;
    private final String action;
    private final Integer idPatient;
    private final String detail;
    private final String poste;

    public EntreeJournal(LocalDateTime dateHeure, Integer idUtilisateur, String action, Integer idPatient,
            String detail, String poste) {
        this.dateHeure = dateHeure;
        this.idUtilisateur = idUtilisateur;
        this.action = action;
        this.idPatient = idPatient;
        this.detail = detail;
        this.poste = poste;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    /** Null pour une tentative de connexion échouée. */
    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public String getAction() {
        return action;
    }

    public Integer getIdPatient() {
        return idPatient;
    }

    public String getDetail() {
        return detail;
    }

    public String getPoste() {
        return poste;
    }
}
