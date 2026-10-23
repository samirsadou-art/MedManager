package My_application.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dossier médical d'un patient : ses consultations, triées chronologiquement,
 * et une position courante pour la navigation (première, précédente, suivante,
 * dernière).
 * <p>
 * Le chargement ({@code ovrirDossier}) et l'enregistrement ({@code update})
 * sont passés dans {@code DossierService} ; la vue montre les visites dans
 * l'onglet « Consultations » ({@code dossier.OngletsDossier}).
 */
public class DossierPatient {

    private final List<consultation> listConsultation = new ArrayList<>();
    private patient malade;
    private medecin medTraitant;
    private int position;

    public DossierPatient(patient malade, medecin medcin) {
        this.malade = malade;
        this.medTraitant = medcin;
    }

    /** Remplace les consultations (utilisé au chargement du dossier). */
    public void setConsultations(List<consultation> consultations) {
        listConsultation.clear();
        listConsultation.addAll(consultations);
        position = Math.max(0, listConsultation.size() - 1);
    }

    public List<consultation> getConsultations() {
        return Collections.unmodifiableList(listConsultation);
    }

    public void ajouterConsultation(consultation consult) {
        listConsultation.add(consult);
    }

    /** La consultation en cours de saisie (non encore enregistrée), s'il y en a une. */
    public consultation getConsultationNonEnregistree() {
        for (consultation c : listConsultation) {
            if (c.estNouvelle()) {
                return c;
            }
        }
        return null;
    }

    // ----- navigation -----

    public int getConsultPosition() {
        return position;
    }

    public void SetPosition(int pos) {
        if (pos < 0 || pos >= listConsultation.size()) {
            throw new IndexOutOfBoundsException("Position " + pos);
        }
        position = pos;
    }

    public consultation FirstConsultation() {
        if (listConsultation.isEmpty()) {
            return null;
        }
        position = 0;
        return listConsultation.get(position);
    }

    public consultation LastConsultation() {
        if (listConsultation.isEmpty()) {
            return null;
        }
        position = listConsultation.size() - 1;
        return listConsultation.get(position);
    }

    public consultation NextConsultation() {
        if (HasNext()) {
            position++;
        }
        return listConsultation.get(position);
    }

    public consultation PreviousConsultation() {
        if (HasPrevious()) {
            position--;
        }
        return listConsultation.get(position);
    }

    public Boolean HasNext() {
        return position + 1 < listConsultation.size();
    }

    public Boolean HasPrevious() {
        return position > 0;
    }

    public int getNombreConsultation() {
        return listConsultation.size();
    }

    public consultation getCurrentConsultation() {
        return listConsultation.get(position);
    }

    public consultation getConsultationAt(int index) {
        return listConsultation.get(index);
    }

    // ----- accesseurs -----

    public patient getMalade() {
        return malade;
    }

    public void setMalade(patient malade) {
        this.malade = malade;
    }

    public medecin getMedTraitant() {
        return medTraitant;
    }

    public void setMedTraitant(medecin medTraitant) {
        this.medTraitant = medTraitant;
    }
}
