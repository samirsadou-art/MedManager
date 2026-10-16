package My_application.entities;

/**
 * Une analyse demandée lors d'une consultation (désignation, observation,
 * résultat saisi plus tard et note d'interprétation 0..4).
 * POJO ; persistance dans {@code AnalyseDao} / {@code DemandeAnalyseDao}.
 */
public class analyse {

    int idAnal;
    String design;
    String observ;
    String resultat;
    int note;

    public analyse() {
        resultat = "";
        note = 0;
    }

    public analyse(String design, String observ, String resultat, char note) {
        this.design = design;
        this.observ = observ;
        this.resultat = resultat == null ? "" : resultat;
        this.note = note;
    }

    /** Deux analyses sont « égales » si elles ont la même désignation. */
    public Boolean egal(analyse autre) {
        return autre != null && autre.getDesign() != null && autre.getDesign().equals(this.design);
    }

    public int getIdAnal() {
        return idAnal;
    }

    public void setIdAnal(int id) {
        this.idAnal = id;
    }

    public String getDesign() {
        return design;
    }

    public void setDesign(String design) {
        this.design = design;
    }

    public String getObserv() {
        return observ;
    }

    public void setObserv(String observ) {
        this.observ = observ;
    }

    public String getResultat() {
        return resultat;
    }

    /** Un résultat vide remet la note à 0 (comportement d'origine). */
    public void setResultat(String resultat) {
        this.resultat = resultat == null ? "" : resultat;
        if (this.resultat.isEmpty()) {
            setNote(0);
        }
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return design;
    }
}
