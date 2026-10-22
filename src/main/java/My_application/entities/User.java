package My_application.entities;

/**
 * Utilisateur de l'application (médecin, secrétaire…).
 * <p>
 * L'ancien constructeur codait en dur « SAHAB SAID, id 1 ». L'utilisateur
 * connecté est désormais fourni par {@code service.Session}.
 */
public class User {

    private int ident;
    private String nom;
    private String prenom;
    private String quailite;
    private String login;
    private boolean actif = true;
    /** Rôle du compte ; médecin par défaut, comme les comptes d'avant la migration V10. */
    private Role role = Role.MEDECIN;
    /** Médecin traitant de la session quand cet utilisateur se connecte (null : celui par défaut). */
    private Integer idMedecin;

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

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role == null ? Role.MEDECIN : role;
    }

    public Integer getIdMedecin() {
        return idMedecin;
    }

    public void setIdMedecin(Integer idMedecin) {
        this.idMedecin = idMedecin;
    }

    @Override
    public String toString() {
        return nom + " " + prenom;
    }
}
