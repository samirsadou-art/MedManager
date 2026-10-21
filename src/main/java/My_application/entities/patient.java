package My_application.entities;

import java.time.LocalDate;
import java.time.Period;

/**
 * Entité Patient (couche Modèle).
 * <p>
 * POJO pur : aucune requête SQL (déplacées dans {@code PatientDao}), aucune
 * dépendance Swing (l'icône est calculée par {@code vue.Icones}).
 */
public class patient {

    private int identPat;
    private String nom;
    private String prenom;
    private LocalDate dat_nais;
    Boolean sexe;            // true = masculin
    private String adresse;
    String num_soc;          // numéro de sécurité sociale
    private String num_id;
    private String fonction;
    private String situatMarit;
    private String groupSang;
    private String pays;
    private String wilaya;
    private String ville;
    private String localite;
    private String telephone;
    private String email;

    public patient() {
    }

    public patient(int identPat, String nom, String prenom, LocalDate dat_nais, Boolean sexe, String adresse,
            String num_soc, String num_id, String fonction) {
        this.identPat = identPat;
        this.nom = nom;
        this.prenom = prenom;
        this.dat_nais = dat_nais;
        this.sexe = sexe;
        this.adresse = adresse;
        this.num_soc = num_soc;
        this.num_id = num_id;
        this.fonction = fonction;
    }

    /**
     * Âge en années révolues ; pour un nourrisson de moins d'un an, renvoie le
     * nombre de mois en négatif (convention historique utilisée par les vues).
     * L'ancien calcul divisait par 365 et 30 jours, ce qui se trompait autour
     * des anniversaires.
     */
    public int getAge() {
        return getAge(LocalDate.now());
    }

    int getAge(LocalDate aujourdhui) {
        if (dat_nais == null) {
            return 0;
        }
        LocalDate naissance = dat_nais;
        if (naissance.isAfter(aujourdhui)) {
            return 0;
        }
        Period p = Period.between(naissance, aujourdhui);
        if (p.getYears() > 0) {
            return p.getYears();
        }
        return -p.getMonths();
    }

    public String getStringAge() {
        int age = getAge();
        if (age > 0) {
            return age + " ans";
        }
        return -age + " mois";
    }

    public boolean isMasculin() {
        return sexe != null && sexe;
    }

    @Override
    public String toString() {
        return getNom() + "  " + getPrenom();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof patient)) {
            return false;
        }
        patient autre = (patient) o;
        return identPat != 0 && identPat == autre.identPat;
    }

    @Override
    public int hashCode() {
        return identPat;
    }

    // ----- accesseurs -----

    public int getIdentPat() {
        return identPat;
    }

    public void setIdentPat(int identPat) {
        this.identPat = identPat;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public LocalDate getDat_nais() {
        return dat_nais;
    }

    public void setDat_nais(LocalDate dat_nais) {
        this.dat_nais = dat_nais;
    }

    public Boolean getSexe() {
        return sexe;
    }

    public void setSexe(Boolean sexe) {
        this.sexe = sexe;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getNum_soc() {
        return num_soc;
    }

    public void setNum_soc(String num_soc) {
        this.num_soc = num_soc;
    }

    public String getNum_id() {
        return num_id;
    }

    public void setNum_id(String num_id) {
        this.num_id = num_id;
    }

    public String getFonction() {
        return fonction;
    }

    public void setFonction(String fonction) {
        this.fonction = fonction;
    }

    public String getSituatMarit() {
        return situatMarit;
    }

    public void setSituatMarit(String situatMarit) {
        this.situatMarit = situatMarit;
    }

    public String getGroupSang() {
        return groupSang;
    }

    public void setGroupSang(String groupSang) {
        this.groupSang = groupSang;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public String getWilaya() {
        return wilaya;
    }

    public void setWilaya(String wilaya) {
        this.wilaya = wilaya;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getLocalite() {
        return localite;
    }

    public void setLocalite(String localite) {
        this.localite = localite;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
