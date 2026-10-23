package My_application.service;

import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.medecin;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rendez-vous et passages au cabinet d'un patient, affichés dans son dossier
 * (onglets « Vue d'ensemble » et « Rendez-vous et règlements »).
 */
public class HistoriquePatient {

    /** Historique vide : quand la lecture a échoué, le dossier s'ouvre quand même. */
    public static final HistoriquePatient VIDE = new HistoriquePatient(
            Collections.<Rendez_Vous>emptyList(), Collections.<Passage>emptyList(), Collections.<medecin>emptyList());

    private final List<Rendez_Vous> rendezVous;
    private final List<Passage> passages;
    private final Map<Integer, medecin> medecins = new HashMap<>();

    /**
     * @param rendezVous du plus récent au plus ancien
     * @param passages du plus récent au plus ancien
     * @param medecins pour nommer le médecin de chaque rendez-vous
     */
    public HistoriquePatient(List<Rendez_Vous> rendezVous, List<Passage> passages, List<medecin> medecins) {
        this.rendezVous = Collections.unmodifiableList(new ArrayList<>(rendezVous));
        this.passages = Collections.unmodifiableList(new ArrayList<>(passages));
        for (medecin m : medecins) {
            this.medecins.put(m.getIdmedecin(), m);
        }
    }

    public List<Rendez_Vous> getRendezVous() {
        return rendezVous;
    }

    public List<Passage> getPassages() {
        return passages;
    }

    /** « Dr NOM Prénom », ou chaîne vide si le médecin est inconnu. */
    public String nomMedecin(Integer idMedecin) {
        medecin m = idMedecin == null ? null : medecins.get(idMedecin);
        return m == null ? "" : m.getNomComplet();
    }

    /**
     * Prochain rendez-vous prévu à partir de ce jour (le plus proche), ou
     * null s'il n'y en a pas.
     */
    public Rendez_Vous prochainRendezVous(LocalDate aPartirDe) {
        Rendez_Vous prochain = null;
        for (Rendez_Vous r : rendezVous) {
            if (r.getEtat() == Rendez_Vous.ETAT_PREVU && !r.getDate().isBefore(aPartirDe)
                    && (prochain == null || r.getDebut().isBefore(prochain.getDebut()))) {
                prochain = r;
            }
        }
        return prochain;
    }

    /** Passage en cours (patient présent au cabinet ce jour-là), ou null. */
    public Passage passageEnCours(LocalDate jour) {
        for (Passage p : passages) {
            if (p.isPresent() && jour.equals(p.getJour())) {
                return p;
            }
        }
        return null;
    }

    /** Dernier passage dont les frais ont été encaissés, ou null. */
    public Passage dernierReglement() {
        for (Passage p : passages) {
            if (p.getMontantRegle() != null) {
                return p;
            }
        }
        return null;
    }

    /** Ce qui reste dû sur un passage (frais moins montant réglé), 0 si rien. */
    public static int resteDu(Passage p) {
        if (p.getFrais() == null || p.getMontantRegle() == null) {
            return 0;
        }
        return Math.max(0, p.getFrais() - p.getMontantRegle());
    }
}
