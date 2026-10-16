/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.modelTableau;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class my_RonduOrdonnance extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected, boolean hasFocus, int ligne, int colonne) {
	Component composant =  super.getTableCellRendererComponent(table, obj, isSelected, hasFocus, ligne, colonne);
    
        Integer val=(Integer)obj;
     
         switch(colonne){
                case 1:  setText(val+" U");break;
                case 2:  setText(val+" fois/jours");break;
                case 3:   if(val==0)
                          setText("le soir");
                          else
                          if (val==1)
                          setText("le matin");
                          else
                          setText(""); 
                          break;
                case 4:        if(val==0)
                                setText("");
                                else
                                setText(val+" jours");
                
                
                      }
        
        
 
           
	return composant;
        
                     
    }
     }