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
public class TableRonduResultatAnalyse extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected, boolean hasFocus, int ligne, int colonne) {
	Component composant =  super.getTableCellRendererComponent(table, obj, isSelected, hasFocus, ligne, colonne);

     if(colonne==2 && !isSelected){
         composant.setBackground(Color.getHSBColor(200,300,100));
     }
      
           
	return composant;
        
       
     
                
    }
}
     

   

    
    



