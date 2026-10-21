package My_application.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Rendez-vous d'un patient (couche Modèle).
 * <p>
 * {@code date}/{@code heure} : créneau réservé ; {@code dateFix}/{@code heureFix} :
 * moment où le rendez-vous a été pris. L'insertion et l'annulation passent
 * désormais par {@code RendezVousDao} / {@code RendezVousService}.
 */
public class Rendez_Vous {

    private User utilsateur;
    private patient Patient;
    private static final DateTimeFormatter FORMAT_JOUR = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private LocalDate dateFix;
    private LocalTime heureFix;
    private LocalDate date;
    private LocalTime heure;
    private int etat;

    public Rendez_Vous() {
    }

    /** Date et heure du rendez-vous réunies. */
    public LocalDateTime getDebut() {
        return date.atTime(heure);
    }

    /** Vrai si le rendez-vous est passé (ou a lieu à la minute même). */
    public boolean isPassed() {
        return isPassed(LocalDateTime.now());
    }

    public boolean isPassed(LocalDateTime maintenant) {
        LocalDateTime debut = getDebut().truncatedTo(ChronoUnit.MINUTES);
        return !debut.isAfter(maintenant.truncatedTo(ChronoUnit.MINUTES));
    }

    /** Nombre de jours calendaires entre aujourd'hui et le rendez-vous. */
    public int getNombreJoursRestant() {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    /** Vrai si les deux rendez-vous désignent le même patient au même créneau. */
    public boolean memeCreneauEtPatient(Rendez_Vous autre) {
        return autre != null
                && Patient != null && autre.Patient != null
                && Patient.getIdentPat() == autre.Patient.getIdentPat()
                && date.equals(autre.date)
                && heure.equals(autre.heure);
    }

    public String getDateString() {
        return FORMAT_JOUR.format(date);
    }

    public String getHeureString() {
        return FORMAT_HEURE.format(heure);
    }

    public String getDateFixString() {
        return FORMAT_JOUR.format(dateFix);
    }

    public String getHeureFixString() {
        return FORMAT_HEURE.format(heureFix);
    }

    @Override
    public String toString() {
        return "[" + getHeureString() + "]    : " + Patient.getNom() + " " + Patient.getPrenom();
    }

    public User getUtilsateur() {
        return utilsateur;
    }

    public void setUtilsateur(User utilsateur) {
        this.utilsateur = utilsateur;
    }

    public patient getPatient() {
        return Patient;
    }

    public void setPatient(patient Patient) {
        this.Patient = Patient;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getHeure() {
        return heure;
    }

    public void setHeure(LocalTime heure) {
        this.heure = heure;
    }

    public LocalDate getDateFix() {
        return dateFix;
    }

    public void setDateFix(LocalDate dateFix) {
        this.dateFix = dateFix;
    }

    public LocalTime getHeureFix() {
        return heureFix;
    }

    public void setHeureFix(LocalTime heureFix) {
        this.heureFix = heureFix;
    }

    public int getEtat() {
        return etat;
    }

    public void setEtat(int etat) {
        this.etat = etat;
    }
}
