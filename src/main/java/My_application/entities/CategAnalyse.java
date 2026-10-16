package My_application.entities;

/** Catégorie d'analyses biologiques (ex. « Bilan lipidique »). */
public class CategAnalyse {

    int id;
    String designation;

    public CategAnalyse() {
    }

    public CategAnalyse(int id, String designation) {
        this.id = id;
        this.designation = designation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    @Override
    public String toString() {
        return designation;
    }
}
