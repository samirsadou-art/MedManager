package My_application.controller;

import My_application.entities.DossierPatient;
import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.consultation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Fenêtre principale : activité du jour et salle d'attente. */
public interface AccueilVue extends Vue {

    void afficherRendezVousDuJour(List<Rendez_Vous> rendezVous);

    void afficherConsultationsDuJour(List<consultation> consultations);

    /**
     * File de la salle d'attente, dans l'ordre d'appel.
     *
     * @param medecins nom de chaque médecin, par identifiant
     * @param maintenant heure de lecture, pour les minutes d'attente
     */
    void afficherSalleAttente(List<Passage> file, Map<Integer, String> medecins, LocalDateTime maintenant);

    /** Un médecin vient d'appeler un patient (annonce pour la secrétaire). */
    void annoncerAppel(String annonce);

    /**
     * Le médecin veut appeler un patient alors qu'un autre est encore en
     * consultation : il doit d'abord la terminer en indiquant les frais.
     *
     * @param fraisProposes frais pré-remplis (vide si aucun)
     * @param terminer      appelé avec les frais saisis ; vrai si la consultation est terminée
     * @return vrai si la consultation a été terminée, faux si le médecin a renoncé
     */
    boolean terminerConsultationEnCours(Passage enCours, String fraisProposes, Predicate<String> terminer);

    /** Le médecin a appelé un patient : son dossier s'ouvre. */
    void ouvrirDossier(DossierPatient dossier);
}
