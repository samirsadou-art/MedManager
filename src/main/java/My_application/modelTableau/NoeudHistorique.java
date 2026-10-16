/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.modelTableau;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class NoeudHistorique {

    public NoeudHistorique(int indiceConsult, String item, int type) {
        this.indiceConsult = indiceConsult;
        this.item = item;
        this.type=type;
    }

    public int getIndiceConsult() {
        return indiceConsult;
    }

    public String getItem() {
        return item;
    }

    public void setIndiceConsult(int indiceConsult) {
        this.indiceConsult = indiceConsult;
    }

    public void setItem(String item) {
        this.item = item;
    }
    
    public String toString(){
        return item;
    }
    
    
    
    int indiceConsult;

    public int getType() {
        return type;
    }
    String item;
    int type;
    
}
