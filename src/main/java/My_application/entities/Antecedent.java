package My_application.entities;

import java.time.LocalDateTime;

/**
 * Antécédent noté dans le dossier médical, hors consultation (table
 * {@code antecedent}, migration V18) : maladie, opération, antécédent
 * familial, allergie, habitude de vie ou traitement au long cours.
 */
public class Antecedent {

    /** Rubrique de l'antécédent ; le nom est la valeur de la colonne {@code categorie}. */
    public enum Categorie {
        ALLERGIE("Allergies"),
        MEDICAL("Médicaux"),
        CHIRURGICAL("Chirurgicaux"),
        FAMILIAL("Familiaux"),
        TRAITEMENT("Traitement au long cours"),
        HABITUDE("Habitudes de vie");

        private final String libelle;

        Categorie(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }

        @Override
        public String toString() {
            return libelle;
        }
    }

    private Integer idAntecedent;
    private int idPatient;
    private Categorie categorie;
    private String libelle;
    private String complement;
    private Integer idMedecin;
    private LocalDateTime saisiLe;

    public Integer getIdAntecedent() {
        return idAntecedent;
    }

    public void setIdAntecedent(Integer idAntecedent) {
        this.idAntecedent = idAntecedent;
    }

    public int getIdPatient() {
        return idPatient;
    }

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    /** Date, dosage ou commentaire, ou null. */
    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public Integer getIdMedecin() {
        return idMedecin;
    }

    public void setIdMedecin(Integer idMedecin) {
        this.idMedecin = idMedecin;
    }

    public LocalDateTime getSaisiLe() {
        return saisiLe;
    }

    public void setSaisiLe(LocalDateTime saisiLe) {
        this.saisiLe = saisiLe;
    }

    /** « Pénicilline (urticaire) ». */
    @Override
    public String toString() {
        return complement == null || complement.isEmpty() ? libelle : libelle + " (" + complement + ")";
    }
}
