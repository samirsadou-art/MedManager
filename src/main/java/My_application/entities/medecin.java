package My_application.entities;

/**
 * Entité Médecin (couche Modèle) — POJO sans accès base de données.
 * La lecture se fait via {@code MedecinDao}.
 */
public class medecin {

    private int idmedecin;
    private String ordmedcin;
    private String nom;
    private String prenom;
    private String titre;
    private String specialite;
    private String adresse;
    private String telephone1;
    private String telephone2;
    private String email;
    private String nom_cabinet;
    /** Faux pour un médecin qui a quitté le cabinet : il n'est plus proposé dans l'agenda. */
    private boolean actif = true;

    public medecin() {
    }

    /** « Dr NOM Prénom » tel qu'affiché dans le dossier patient. */
    public String getNomComplet() {
        return nonNul(titre) + " " + nonNul(nom) + " " + nonNul(prenom);
    }

    private static String nonNul(String s) {
        return s == null ? "" : s;
    }

    @Override
    public String toString() {
        return getNomComplet().trim();
    }

    public int getIdmedecin() {
        return idmedecin;
    }

    public void setIdmedecin(int idmedecin) {
        this.idmedecin = idmedecin;
    }

    public String getOrdmedcin() {
        return ordmedcin;
    }

    public void setOrdmedcin(String ordmedcin) {
        this.ordmedcin = ordmedcin;
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

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTelephone1() {
        return telephone1;
    }

    public void setTelephone1(String telephone1) {
        this.telephone1 = telephone1;
    }

    public String getTelephone2() {
        return telephone2;
    }

    public void setTelephone2(String telephone2) {
        this.telephone2 = telephone2;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNom_cabinet() {
        return nom_cabinet;
    }

    public void setNom_cabinet(String nom_cabinet) {
        this.nom_cabinet = nom_cabinet;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
