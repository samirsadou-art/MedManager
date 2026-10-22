package My_application.entities;

import java.time.LocalDateTime;

/**
 * Message de la messagerie interne : privé entre deux membres, ou pour tout
 * le cabinet ({@code idDestinataire} null). Sans expéditeur, c'est un
 * message du système. Il peut citer un patient (lien vers sa fiche).
 */
public class MessageInterne {

    private Integer idMessage;
    private Integer idExpediteur;
    /** « NOM Prénom » de l'expéditeur, lu avec le message. */
    private String nomExpediteur;
    private Integer idDestinataire;
    private String texte;
    /** Patient cité (identifiant, nom et prénom seulement), ou null. */
    private patient patient;
    private LocalDateTime envoyeLe;
    /** Message privé déjà lu par son destinataire. */
    private boolean luParDestinataire;

    public boolean isSysteme() {
        return idExpediteur == null;
    }

    public boolean isPourTous() {
        return idDestinataire == null;
    }

    public Integer getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(Integer idMessage) {
        this.idMessage = idMessage;
    }

    public Integer getIdExpediteur() {
        return idExpediteur;
    }

    public void setIdExpediteur(Integer idExpediteur) {
        this.idExpediteur = idExpediteur;
    }

    public String getNomExpediteur() {
        return nomExpediteur;
    }

    public void setNomExpediteur(String nomExpediteur) {
        this.nomExpediteur = nomExpediteur;
    }

    public Integer getIdDestinataire() {
        return idDestinataire;
    }

    public void setIdDestinataire(Integer idDestinataire) {
        this.idDestinataire = idDestinataire;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public patient getPatient() {
        return patient;
    }

    public void setPatient(patient patient) {
        this.patient = patient;
    }

    public LocalDateTime getEnvoyeLe() {
        return envoyeLe;
    }

    public void setEnvoyeLe(LocalDateTime envoyeLe) {
        this.envoyeLe = envoyeLe;
    }

    public boolean isLuParDestinataire() {
        return luParDestinataire;
    }

    public void setLuParDestinataire(boolean luParDestinataire) {
        this.luParDestinataire = luParDestinataire;
    }
}
