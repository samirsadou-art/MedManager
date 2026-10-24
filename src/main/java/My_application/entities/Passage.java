package My_application.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Passage d'un patient dans la salle d'attente (table salle_attente,
 * migration V13) : de son arrivée au cabinet à sa sortie, avec ou sans
 * rendez-vous.
 */
public class Passage {

    public static final int EN_ATTENTE = 0;
    public static final int EN_CONSULTATION = 1;
    public static final int TERMINE = 2;
    /** Le patient est reparti sans être vu. */
    public static final int PARTI = 3;
    /**
     * Consultation terminée (V17) : le patient est encore au cabinet, la
     * secrétaire encaisse les frais puis le libère (état {@link #TERMINE}).
     */
    public static final int A_REGLER = 4;

    private Integer idPassage;
    private patient patient;
    private int idMedecin;
    /** Rendez-vous à l'origine du passage ; null pour un patient sans rendez-vous. */
    private Integer idRendezVous;
    /** Heure du rendez-vous (lue avec le passage) ; null sans rendez-vous. */
    private LocalTime heurePrevue;
    private LocalDate jour;
    private LocalDateTime arriveeLe;
    private LocalDateTime appeleLe;
    private LocalDateTime sortieLe;
    private boolean urgence;
    private int etat = EN_ATTENTE;
    /** Frais fixés par le médecin, en dinars ; null s'il ne les a pas précisés. */
    private Integer frais;
    /** Montant encaissé par la secrétaire, en dinars ; null si rien n'a été encaissé. */
    private Integer montantRegle;
    private LocalDateTime regleLe;

    /** Encore au cabinet : en attente, en consultation ou à régler. */
    public boolean isPresent() {
        return isDansLaFile() || etat == A_REGLER;
    }

    /** Dans la file d'un médecin : en attente ou en consultation. */
    public boolean isDansLaFile() {
        return etat == EN_ATTENTE || etat == EN_CONSULTATION;
    }

    public boolean isSansRendezVous() {
        return idRendezVous == null;
    }

    /** Minutes d'attente : de l'arrivée à l'appel, ou jusqu'à maintenant s'il attend encore. */
    public long minutesDAttente(LocalDateTime maintenant) {
        LocalDateTime fin = appeleLe != null ? appeleLe : sortieLe != null ? sortieLe : maintenant;
        return Math.max(0, ChronoUnit.MINUTES.between(arriveeLe, fin));
    }

    public Integer getIdPassage() {
        return idPassage;
    }

    public void setIdPassage(Integer idPassage) {
        this.idPassage = idPassage;
    }

    public patient getPatient() {
        return patient;
    }

    public void setPatient(patient patient) {
        this.patient = patient;
    }

    public int getIdMedecin() {
        return idMedecin;
    }

    public void setIdMedecin(int idMedecin) {
        this.idMedecin = idMedecin;
    }

    public Integer getIdRendezVous() {
        return idRendezVous;
    }

    public void setIdRendezVous(Integer idRendezVous) {
        this.idRendezVous = idRendezVous;
    }

    public LocalTime getHeurePrevue() {
        return heurePrevue;
    }

    public void setHeurePrevue(LocalTime heurePrevue) {
        this.heurePrevue = heurePrevue;
    }

    public LocalDate getJour() {
        return jour;
    }

    public void setJour(LocalDate jour) {
        this.jour = jour;
    }

    public LocalDateTime getArriveeLe() {
        return arriveeLe;
    }

    public void setArriveeLe(LocalDateTime arriveeLe) {
        this.arriveeLe = arriveeLe;
    }

    public LocalDateTime getAppeleLe() {
        return appeleLe;
    }

    public void setAppeleLe(LocalDateTime appeleLe) {
        this.appeleLe = appeleLe;
    }

    public LocalDateTime getSortieLe() {
        return sortieLe;
    }

    public void setSortieLe(LocalDateTime sortieLe) {
        this.sortieLe = sortieLe;
    }

    public boolean isUrgence() {
        return urgence;
    }

    public void setUrgence(boolean urgence) {
        this.urgence = urgence;
    }

    public int getEtat() {
        return etat;
    }

    public void setEtat(int etat) {
        this.etat = etat;
    }

    public Integer getFrais() {
        return frais;
    }

    public void setFrais(Integer frais) {
        this.frais = frais;
    }

    public Integer getMontantRegle() {
        return montantRegle;
    }

    public void setMontantRegle(Integer montantRegle) {
        this.montantRegle = montantRegle;
    }

    public LocalDateTime getRegleLe() {
        return regleLe;
    }

    public void setRegleLe(LocalDateTime regleLe) {
        this.regleLe = regleLe;
    }
}
