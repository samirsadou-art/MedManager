/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.Patient;

import java.util.Locale;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.Component;
import My_application.entities.patient;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;


/**
 *
 * @author GLOBAL INFO LAGA
 */
public class ListePatientRondu implements TableCellRenderer{
ImageIcon imageIcon;

  protected DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();




  
   
 
 

 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      
       JLabel renderer = (JLabel) defaultRenderer.getTableCellRendererComponent(table, value,isSelected, hasFocus, row, column);
        
       renderer.setHorizontalAlignment(JLabel.LEFT);
       Theme.cellule(renderer, table, isSelected, row);

        patient malade=(patient)value;


        switch(column){

            case 0:imageIcon=Icones.patient(malade, 32);
                   renderer.setHorizontalAlignment(JLabel.CENTER);
                   renderer.setIcon(imageIcon);
                   renderer.setText("");
                   break;
            case 1: renderer.setText(malade.getNom().toUpperCase(Locale.FRENCH));renderer.setIcon(null);
                    renderer.setFont(Theme.policeGras());break;
            case 2: renderer.setText(malade.getPrenom().toUpperCase(Locale.FRENCH));break;
            case 3: renderer.setText(malade.getStringAge()); 
                    
            
            
            
        }
        
        
        
       
        
        
             
        
     
      
             return renderer;     
    }
     

    
    
    
        
    }
