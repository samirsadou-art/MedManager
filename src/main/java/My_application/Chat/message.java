/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.Chat;

import java.util.Date;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class message extends Object {
    
    String texte;
    int auteur=0;
    Date date;

    public String getTexte() {
        return texte;
    }

    public int getAuteur() {
        return auteur;
    }



    public Date getDate() {
        return date;
    }


    public void setAuteur(int auteur) {
        this.auteur = auteur;
    }




    public void setTexte(String texte) {
        this.texte = texte;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
  
    
    
    
}
