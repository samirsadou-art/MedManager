/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.TableModelPrincipale;

import My_application.vue.Icones;
import My_application.entities.consultation;
import java.awt.Component;
import java.awt.Font;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class ConsultJourTableRondu implements TableCellRenderer{
ImageIcon imageIcon;

  protected DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();




  
   
 
 

 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        
        Component comp=defaultRenderer.getTableCellRendererComponent(table, value,isSelected, hasFocus, row, column);
      
       JLabel renderer = (JLabel)comp; 
        
       renderer.setHorizontalAlignment(JLabel.LEFT);
       renderer.setFont(new Font("Goudy Old Style",Font.BOLD,12));
        
        consultation consult=(consultation)value;

        
        switch(column){
            
            case 0:imageIcon=Icones.patient(consult.getMalade(), 32);
                   renderer.setHorizontalAlignment(JLabel.CENTER);
                   renderer.setIcon(imageIcon);
                   renderer.setText("");
                   break;
            case 1: renderer.setText(consult.getPatient().getNom().toUpperCase());renderer.setIcon(null);break;
            case 2: renderer.setText(consult.getPatient().getPrenom().toUpperCase());break;
            case 3: renderer.setHorizontalAlignment(JLabel.CENTER);renderer.setText(consult.getPatient().getStringAge()); break;
            case 4: renderer.setHorizontalAlignment(JLabel.CENTER);renderer.setText(consult.getHeureString());
            
            
            
        }
        
        
        
       
        
        
             
        
         if (isSelected) {
            renderer.setBackground(table.getSelectionBackground());
            renderer.setForeground(table.getSelectionForeground());
        } else {
            renderer.setBackground(table.getBackground());
            renderer.setForeground(table.getForeground());
        } 
        
     
      
             return renderer;     
    }
     

    
}
    
        
    

