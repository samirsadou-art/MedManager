package My_application.entities;

/** Entité Pathologie (référentiel) — POJO ; persistance dans {@code PathologieDao}. */
public class pathologie {

    int id;
    String designation;

    public pathologie() {
    }

    public pathologie(int id, String designation) {
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
