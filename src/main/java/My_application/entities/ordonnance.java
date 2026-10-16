package My_application.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ordonnance d'une consultation : la liste des médicaments prescrits.
 * <p>
 * Avant la refonte, cette classe contenait un {@code TableModel} Swing, du SQL
 * et la génération du rapport Jasper. Ces responsabilités sont désormais dans
 * {@code my_modelOrdonnance} (vue), {@code PrescriptionDao} (DAO) et
 * {@code report.RapportService}.
 */
public class ordonnance {

    private final List<medicament> medicaments = new ArrayList<>();

    public ordonnance() {
    }

    /** Ajoute un médicament s'il n'est pas déjà prescrit ; renvoie true si ajouté. */
    public boolean ajouter(medicament med) {
        for (medicament m : medicaments) {
            if (med.egal(m)) {
                return false;
            }
        }
        medicaments.add(med);
        return true;
    }

    public void retirer(int index) {
        medicaments.remove(index);
    }

    /** Remplace le contenu (validation du formulaire d'ordonnance). */
    public void setMedicaments(List<medicament> nouveaux) {
        medicaments.clear();
        medicaments.addAll(nouveaux);
    }

    /** Vue non modifiable : les ajouts passent par {@link #ajouter}. */
    public List<medicament> getMedicaments() {
        return Collections.unmodifiableList(medicaments);
    }

    public medicament getMedicament(int i) {
        return medicaments.get(i);
    }

    public int getNombreMedicament() {
        return medicaments.size();
    }

    public boolean estVide() {
        return medicaments.isEmpty();
    }
}
