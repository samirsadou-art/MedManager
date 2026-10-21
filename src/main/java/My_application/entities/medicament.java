package My_application.entities;

/**
 * Un médicament et, lorsqu'il fait partie d'une ordonnance, sa prescription
 * (unités, posologie, moment de prise, durée).
 */
public class medicament {

    /** Valeurs du champ {@code mode} (moment de prise). */
    public static final int MODE_SOIR = 0;
    public static final int MODE_MATIN = 1;
    public static final int MODE_INDIFFERENT = 2;

    private int idmed;
    private String designmed;
    private int unite;
    private int posologie;
    private int mode;
    private int duree;

    public medicament() {
    }

    /** Copie (utilisée pour éditer une ordonnance sans modifier l'original). */
    public medicament(medicament m) {
        this.idmed = m.idmed;
        this.designmed = m.designmed;
        this.unite = m.unite;
        this.posologie = m.posologie;
        this.mode = m.mode;
        this.duree = m.duree;
    }

    public Boolean egal(medicament med) {
        return med != null && med.getDesignmed() != null && med.getDesignmed().equals(this.designmed);
    }

    public int getIdmed() {
        return idmed;
    }

    public void setIdmed(int idmed) {
        this.idmed = idmed;
    }

    public String getDesignmed() {
        return designmed;
    }

    public void setDesignmed(String designmed) {
        this.designmed = designmed;
    }

    public int getUnite() {
        return unite;
    }

    public void setUnite(int unite) {
        this.unite = unite;
    }

    public int getPosologie() {
        return posologie;
    }

    public void setPosologie(int posologie) {
        this.posologie = posologie;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    @Override
    public String toString() {
        return designmed;
    }
}
