package My_application.entities;

/**
 * Utilisateur de l'application (médecin, secrétaire…).
 * <p>
 * L'ancien constructeur codait en dur « SAHAB SAID, id 1 ». L'utilisateur
 * connecté est désormais fourni par {@code service.Session}.
 */
public class User {

    int ident;
    String nom;
    String prenom;
    String quailite;

    public User() {
    }

    public User(int ident, String nom, String prenom, String qualite) {
        this.ident = ident;
        this.nom = nom;
        this.prenom = prenom;
        this.quailite = qualite;
    }

    public int getIdent() {
        return ident;
    }

    public void setIdent(int ident) {
        this.ident = ident;
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

    public String getQuailite() {
        return quailite;
    }

    public void setQuailite(String quailite) {
        this.quailite = quailite;
    }

    @Override
    public String toString() {
        return nom + " " + prenom;
    }
}
