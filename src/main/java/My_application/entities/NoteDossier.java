package My_application.entities;

import java.time.LocalDateTime;

/** Note libre du médecin dans le dossier médical (table {@code note_dossier}, migration V18). */
public class NoteDossier {

    private Integer idNote;
    private int idPatient;
    private String texte;
    private Integer idMedecin;
    private LocalDateTime ecriteLe;

    public Integer getIdNote() {
        return idNote;
    }

    public void setIdNote(Integer idNote) {
        this.idNote = idNote;
    }

    public int getIdPatient() {
        return idPatient;
    }

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public Integer getIdMedecin() {
        return idMedecin;
    }

    public void setIdMedecin(Integer idMedecin) {
        this.idMedecin = idMedecin;
    }

    public LocalDateTime getEcriteLe() {
        return ecriteLe;
    }

    public void setEcriteLe(LocalDateTime ecriteLe) {
        this.ecriteLe = ecriteLe;
    }
}
