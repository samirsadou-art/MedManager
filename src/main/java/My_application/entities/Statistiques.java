package My_application.entities;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/**
 * Activité du cabinet sur une période (compte administrateur) : uniquement
 * des nombres agrégés, aucun nom de patient. {@code idMedecin} null : tout
 * le cabinet.
 */
public class Statistiques {

    private final LocalDate debut;
    private final LocalDate fin;
    private final Integer idMedecin;
    private Map<LocalDate, Integer> consultationsParJour = new TreeMap<>();
    private Map<Integer, Integer> consultationsParMedecin = new TreeMap<>();
    private Map<Integer, Integer> rendezVousParEtat = new TreeMap<>();
    private int patientsDistincts;
    private int nouveauxPatients;
    private SalleAttente salle = new SalleAttente();

    public Statistiques(LocalDate debut, LocalDate fin, Integer idMedecin) {
        this.debut = debut;
        this.fin = fin;
        this.idMedecin = idMedecin;
    }

    /** Passages en salle d'attente sur la période. */
    public static class SalleAttente {
        private int passages;
        private int sansRendezVous;
        private int urgences;
        private int partis;
        /** Minutes entre l'arrivée et l'appel, en moyenne ; null sans patient appelé. */
        private Double attenteMoyenne;

        public int getPassages() {
            return passages;
        }

        public void setPassages(int passages) {
            this.passages = passages;
        }

        public int getSansRendezVous() {
            return sansRendezVous;
        }

        public void setSansRendezVous(int sansRendezVous) {
            this.sansRendezVous = sansRendezVous;
        }

        public int getUrgences() {
            return urgences;
        }

        public void setUrgences(int urgences) {
            this.urgences = urgences;
        }

        public int getPartis() {
            return partis;
        }

        public void setPartis(int partis) {
            this.partis = partis;
        }

        public Double getAttenteMoyenne() {
            return attenteMoyenne;
        }

        public void setAttenteMoyenne(Double attenteMoyenne) {
            this.attenteMoyenne = attenteMoyenne;
        }
    }

    public LocalDate getDebut() {
        return debut;
    }

    public LocalDate getFin() {
        return fin;
    }

    public Integer getIdMedecin() {
        return idMedecin;
    }

    public int getConsultations() {
        int n = 0;
        for (int v : consultationsParJour.values()) {
            n += v;
        }
        return n;
    }

    /** Jours avec au moins une consultation, dans l'ordre. */
    public Map<LocalDate, Integer> getConsultationsParJour() {
        return consultationsParJour;
    }

    public void setConsultationsParJour(Map<LocalDate, Integer> parJour) {
        this.consultationsParJour = new TreeMap<>(parJour);
    }

    public Map<Integer, Integer> getConsultationsParMedecin() {
        return consultationsParMedecin;
    }

    public void setConsultationsParMedecin(Map<Integer, Integer> parMedecin) {
        this.consultationsParMedecin = new TreeMap<>(parMedecin);
    }

    /** Nombre de rendez-vous par état ({@code Rendez_Vous.ETAT_*}). */
    public Map<Integer, Integer> getRendezVousParEtat() {
        return rendezVousParEtat;
    }

    public void setRendezVousParEtat(Map<Integer, Integer> parEtat) {
        this.rendezVousParEtat = new TreeMap<>(parEtat);
    }

    public int rendezVous(int etat) {
        Integer n = rendezVousParEtat.get(etat);
        return n == null ? 0 : n;
    }

    public int getRendezVous() {
        int n = 0;
        for (int v : rendezVousParEtat.values()) {
            n += v;
        }
        return n;
    }

    /** Patients venus (arrivés, en consultation ou vus). */
    public int getRendezVousHonores() {
        return rendezVous(Rendez_Vous.ETAT_ARRIVE) + rendezVous(Rendez_Vous.ETAT_EN_CONSULTATION)
                + rendezVous(Rendez_Vous.ETAT_TERMINE);
    }

    /** Absents parmi les rendez-vous venus ou manqués, en % ; null s'il n'y en a aucun. */
    public Double getTauxAbsence() {
        int absents = rendezVous(Rendez_Vous.ETAT_ABSENT);
        int base = absents + getRendezVousHonores();
        return base == 0 ? null : 100.0 * absents / base;
    }

    public int getPatientsDistincts() {
        return patientsDistincts;
    }

    public void setPatientsDistincts(int patientsDistincts) {
        this.patientsDistincts = patientsDistincts;
    }

    /** Patients dont la toute première consultation au cabinet tombe dans la période. */
    public int getNouveauxPatients() {
        return nouveauxPatients;
    }

    public void setNouveauxPatients(int nouveauxPatients) {
        this.nouveauxPatients = nouveauxPatients;
    }

    public SalleAttente getSalle() {
        return salle;
    }

    public void setSalle(SalleAttente salle) {
        this.salle = salle;
    }
}
