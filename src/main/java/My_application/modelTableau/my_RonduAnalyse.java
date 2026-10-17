/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.modelTableau;

import My_application.vue.Theme;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class my_RonduAnalyse extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected, boolean hasFocus, int ligne, int colonne) {
	Component composant =  super.getTableCellRendererComponent(table, obj, isSelected, hasFocus, ligne, colonne);
        
        
     

     int c=(int)obj;   

   this.setText("");
   // Note d'interprétation : couleurs de statut de la charte.
   switch(c){
       case 1:composant.setBackground(Theme.SUCCES);break;
       case 2:composant.setBackground(new Color(0xEF9F27));break;
       case 3:composant.setBackground(Theme.DANGER);break;
       default:composant.setBackground(isSelected ? table.getSelectionBackground() : Theme.SURFACE);
   }
   
  
      
           
	return composant;
        
       
     
                
    }
     }