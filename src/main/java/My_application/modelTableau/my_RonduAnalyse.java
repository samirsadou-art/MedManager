/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.modelTableau;

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
   switch(c){
       case 1:composant.setBackground(Color.GREEN);break;
       case 2:composant.setBackground(Color.ORANGE);break;
       case 3:composant.setBackground(Color.RED);break;
       default:composant.setBackground(Color.white);
   }
   
  
      
           
	return composant;
        
       
     
                
    }
     }