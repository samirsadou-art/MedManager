package My_application.entities;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Consultation (couche Modèle).
 * <p>
 * Changements par rapport à l'ancienne version :
 * <ul>
 * <li>plus de SQL ni de {@code PreparedStatement} en attributs : l'insertion
 * et le chargement sont faits par {@code ConsultationDao}, orchestrés par
 * {@code DossierService} dans une transaction ;</li>
 * <li>les symptômes et pathologies sont des {@code List} et non plus des
 * {@code DefaultListModel} Swing : la vue construit son propre modèle
 * d'affichage.</li>
 * </ul>
 * Les constantes vitales valent -1 tant qu'elles ne sont pas renseignées.
 */
public class consultation {

    public static final int NON_RENSEIGNE = -1;

    private int idConsul;
    private LocalDate date_cons;
    private LocalTime heure_cons;
    private double tarif;

    private double poids = NON_RENSEIGNE;
    private double temperature = NON_RENSEIGNE;
    private String pressionA = "";
    private int tauxRespir = NON_RENSEIGNE;
    private int pul_Cardiaque = NON_RENSEIGNE;

    private patient malade;
    private medecin medTraitant;
    private ordonnance OrdoPat;
    private analyseBio AnaPat;

    private final List<symptome> symptomes = new ArrayList<>();
    private final List<pathologie> pathologies = new ArrayList<>();

    public consultation() {
    }

    public consultation(patient pat, medecin med) {
        this.malade = pat;
        this.medTraitant = med;
        this.OrdoPat = new ordonnance();
        this.AnaPat = new analyseBio();
    }

    /** Une consultation non encore enregistrée n'a pas d'identifiant. */
    public boolean estNouvelle() {
        return idConsul == 0;
    }

    // ----- symptômes / pathologies -----

    /** Ajoute le symptôme s'il n'est pas déjà présent (même désignation). */
    public boolean addSymptome(symptome symp) {
        if (symp == null) {
            return false;
        }
        for (symptome s : symptomes) {
            if (s.getDesignation().equalsIgnoreCase(symp.getDesignation())) {
                return false;
            }
        }
        symptomes.add(symp);
        return true;
    }

    public boolean addPathologie(pathologie path) {
        if (path == null) {
            return false;
        }
        for (pathologie p : pathologies) {
            if (p.getDesignation().equalsIgnoreCase(path.getDesignation())) {
                return false;
            }
        }
        pathologies.add(path);
        return true;
    }

    public void retirerSymptome(int index) {
        symptomes.remove(index);
    }

    public void retirerPathologie(int index) {
        pathologies.remove(index);
    }

    public List<symptome> getSymptomes() {
        return Collections.unmodifiableList(symptomes);
    }

    public List<pathologie> getPathologies() {
        return Collections.unmodifiableList(pathologies);
    }

    public int getNombreSymptomes() {
        return symptomes.size();
    }

    public int getNombrePathologies() {
        return pathologies.size();
    }

    // ----- ordonnance / analyses -----

    public void addOrdonnance() {
        OrdoPat = new ordonnance();
    }

    public void addAnalyse() {
        AnaPat = new analyseBio();
    }

    public boolean hasOrdonnance() {
        return OrdoPat != null && !OrdoPat.estVide();
    }

    public boolean hasAanlyse() {
        return AnaPat != null && !AnaPat.estVide();
    }

    /** Vrai si des résultats d'analyses ont été modifiés après l'enregistrement. */
    public boolean isChange() {
        return AnaPat != null && AnaPat.isChange();
    }

    public String getHeureString() {
        return heure_cons == null ? "" : heure_cons.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // ----- accesseurs -----

    public int getIdConsul() {
        return idConsul;
    }

    public void setIdConsul(int idConsul) {
        this.idConsul = idConsul;
    }

    public LocalDate getDate_cons() {
        return date_cons;
    }

    public void setDate_cons(LocalDate date_cons) {
        this.date_cons = date_cons;
    }

    public LocalTime getHeure_cons() {
        return heure_cons;
    }

    public void setHeure_cons(LocalTime heure_cons) {
        this.heure_cons = heure_cons;
    }

    public double getTarif() {
        return tarif;
    }

    public void setTarif(double tarif) {
        this.tarif = tarif;
    }

    public double getPoids() {
        return poids;
    }

    public void setPoids(double poids) {
        this.poids = poids;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getPressionA() {
        return pressionA;
    }

    public void setPressionA(String pressionA) {
        this.pressionA = pressionA == null ? "" : pressionA;
    }

    public int getTauxRespir() {
        return tauxRespir;
    }

    public void setTauxRespir(int tauxRespir) {
        this.tauxRespir = tauxRespir;
    }

    public int getPul_Cardiaque() {
        return pul_Cardiaque;
    }

    public void setPul_Cardiaque(int pul_Cardiaque) {
        this.pul_Cardiaque = pul_Cardiaque;
    }

    public patient getMalade() {
        return malade;
    }

    public void setMalade(patient malade) {
        this.malade = malade;
    }

    public patient getPatient() {
        return malade;
    }

    public medecin getMedTraitant() {
        return medTraitant;
    }

    public medecin getMedecin() {
        return medTraitant;
    }

    public void setMedTraitant(medecin medTraitant) {
        this.medTraitant = medTraitant;
    }

    public ordonnance getOrdoPat() {
        return OrdoPat;
    }

    public void setOrdoPat(ordonnance OrdoPat) {
        this.OrdoPat = OrdoPat;
    }

    public analyseBio getAnaPat() {
        return AnaPat;
    }

    public void setAnaPat(analyseBio AnaPat) {
        this.AnaPat = AnaPat;
    }
}
