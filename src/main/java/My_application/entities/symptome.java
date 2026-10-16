package My_application.entities;

/** Entité Symptôme (référentiel) — POJO ; persistance dans {@code SymptomeDao}. */
public class symptome {

    int id;
    String designation;

    public symptome() {
    }

    public symptome(int id, String designation) {
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
