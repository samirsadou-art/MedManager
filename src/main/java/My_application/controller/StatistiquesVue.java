package My_application.controller;

import My_application.entities.Statistiques;
import My_application.entities.medecin;
import My_application.service.StatistiquesService;
import java.util.List;
import java.util.Map;

/** Tableau de bord des statistiques (compte administrateur). */
public interface StatistiquesVue extends Vue {

    /** Médecins de la liste de choix (tous, même désactivés). */
    void afficherMedecins(List<medecin> medecins);

    void afficherStatistiques(Statistiques s, List<StatistiquesService.Barre> serie, Map<Integer, medecin> medecins);
}
