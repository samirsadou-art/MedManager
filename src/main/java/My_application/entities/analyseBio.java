package My_application.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Demande d'analyses biologiques d'une consultation.
 * <p>
 * Le drapeau {@code change} indique que des résultats ont été saisis après
 * coup et doivent être enregistrés. Le SQL est dans {@code DemandeAnalyseDao},
 * le rapport dans {@code report.RapportService}.
 */
public class analyseBio {

    private boolean change;
    private List<analyse> listeAnaly = new ArrayList<>();

    public analyseBio() {
    }

    public boolean isChange() {
        return change;
    }

    public void setChange(boolean change) {
        this.change = change;
    }

    public List<analyse> getListeAnaly() {
        return listeAnaly;
    }

    public void setListeAnaly(List<analyse> listeAnal) {
        this.listeAnaly = listeAnal == null ? new ArrayList<analyse>() : listeAnal;
    }

    public int getNombreAnalyse() {
        return listeAnaly.size();
    }

    public boolean estVide() {
        return listeAnaly.isEmpty();
    }
}
