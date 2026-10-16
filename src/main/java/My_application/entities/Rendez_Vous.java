package My_application.entities;

import java.sql.Date;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Rendez-vous d'un patient (couche Modèle).
 * <p>
 * {@code date}/{@code heure} : créneau réservé ; {@code dateFix}/{@code heureFix} :
 * moment où le rendez-vous a été pris. L'insertion et l'annulation passent
 * désormais par {@code RendezVousDao} / {@code RendezVousService}.
 */
public class Rendez_Vous {

    User utilsateur;
    patient Patient;
    Date dateFix;
    Time heureFix;
    Date date;
    Time heure;
    int etat;

    public Rendez_Vous() {
    }

    /** Date et heure du rendez-vous réunies. */
    public LocalDateTime getDebut() {
        return date.toLocalDate().atTime(heure.toLocalTime());
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
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), date.toLocalDate());
    }

    /** Vrai si les deux rendez-vous désignent le même patient au même créneau. */
    public boolean memeCreneauEtPatient(Rendez_Vous autre) {
        return autre != null
                && Patient != null && autre.Patient != null
                && Patient.getIdentPat() == autre.Patient.getIdentPat()
                && date.toLocalDate().equals(autre.date.toLocalDate())
                && heure.toLocalTime().equals(autre.heure.toLocalTime());
    }

    public String getDateString() {
        return new SimpleDateFormat("EEEE, d MMMM yyyy").format(date);
    }

    public String getHeureString() {
        return new SimpleDateFormat("HH:mm").format(heure);
    }

    public String getDateFixString() {
        return new SimpleDateFormat("EEEE, d MMMM yyyy").format(dateFix);
    }

    public String getHeureFixString() {
        return new SimpleDateFormat("HH:mm").format(heureFix);
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getHeure() {
        return heure;
    }

    public void setHeure(Time heure) {
        this.heure = heure;
    }

    public Date getDateFix() {
        return dateFix;
    }

    public void setDateFix(Date dateFix) {
        this.dateFix = dateFix;
    }

    public Time getHeureFix() {
        return heureFix;
    }

    public void setHeureFix(Time heureFix) {
        this.heureFix = heureFix;
    }

    public int getEtat() {
        return etat;
    }

    public void setEtat(int etat) {
        this.etat = etat;
    }
}
