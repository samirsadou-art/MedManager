package My_application.controller;

import My_application.entities.Statistiques;
import My_application.entities.medecin;
import My_application.report.RapportService;
import My_application.service.StatistiquesService;
import java.io.File;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistiques de l'administrateur : le calcul (quelques requêtes
 * d'agrégation) se fait en arrière-plan, puis l'écran est rempli. Les
 * exports reprennent les derniers chiffres affichés.
 */
public class StatistiquesController extends ControleurBase {

    private final StatistiquesVue vue;
    private final StatistiquesService service;
    private final RapportService rapports;
    private final Map<Integer, medecin> medecins = new LinkedHashMap<>();
    private Statistiques dernieres;

    public StatistiquesController(StatistiquesVue vue, StatistiquesService service, RapportService rapports,
            Executeur executeur) {
        super(vue, executeur);
        this.vue = vue;
        this.service = service;
        this.rapports = rapports;
    }

    /** Charge la liste des médecins (une fois, à l'ouverture de la page). */
    public void chargerMedecins() {
        tenter(() -> {
            List<medecin> liste = service.medecins();
            medecins.clear();
            for (medecin m : liste) {
                medecins.put(m.getIdmedecin(), m);
            }
            vue.afficherMedecins(liste);
        });
    }

    public void calculer(LocalDate debut, LocalDate fin, Integer idMedecin) {
        enArrierePlan(() -> service.calculer(debut, fin, idMedecin), s -> {
            dernieres = s;
            vue.afficherStatistiques(s, StatistiquesService.serie(s), medecins);
        });
    }

    /** Derniers chiffres affichés, ou null. */
    public Statistiques getDernieres() {
        return dernieres;
    }

    public void exporterPdf() {
        if (dernieres != null) {
            tenter(() -> rapports.statistiques(dernieres, medecins));
        }
    }

    public void exporterCsv(File fichier) {
        if (dernieres != null && tenter(() -> service.exporterCsv(dernieres, medecins, fichier))) {
            vue.afficherInformation("Statistiques enregistrées dans :\n" + fichier.getAbsolutePath());
        }
    }

    /** Nom proposé pour l'export CSV. */
    public String nomCsv() {
        return dernieres == null ? "Statistiques.csv"
                : "Statistiques_" + dernieres.getDebut() + "_" + dernieres.getFin() + ".csv";
    }
}
