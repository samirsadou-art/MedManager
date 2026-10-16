/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.TableModelPrincipale;

import My_application.vue.Icones;
import java.awt.Component;
import java.awt.Font;
import My_application.entities.Rendez_Vous;
import java.awt.Color;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;


/**
 *
 * @author GLOBAL INFO LAGA
 */
public class RendezJourTableRondu implements TableCellRenderer{
ImageIcon imageIcon;

  protected DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();




  
   
 
 

 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      
       JLabel renderer = (JLabel) defaultRenderer.getTableCellRendererComponent(table, value,isSelected, hasFocus, row, column);
        
       renderer.setHorizontalAlignment(JLabel.LEFT);
       renderer.setFont(new Font("Goudy Old Style",Font.BOLD,12));
        
        Rendez_Vous rendez=(Rendez_Vous)value;

        
        switch(column){
            
            case 0:imageIcon=Icones.patient(rendez.getPatient(), 32);
                   renderer.setHorizontalAlignment(JLabel.CENTER);
                   renderer.setIcon(imageIcon);
                   renderer.setText("");
                   break;
            case 1: renderer.setText(rendez.getPatient().getNom().toUpperCase()); renderer.setIcon(null);break;
            case 2: renderer.setText(rendez.getPatient().getPrenom().toUpperCase());break;
            case 3: renderer.setHorizontalAlignment(JLabel.CENTER);renderer.setText(rendez.getPatient().getStringAge()); break;
            case 4: renderer.setHorizontalAlignment(JLabel.CENTER); 
                    renderer.setText(rendez.getHeureString());
            
            
            
        }
        
        
     
       
        
        
             
        
         if (isSelected) {
            renderer.setBackground(table.getSelectionBackground());
            renderer.setForeground(table.getSelectionForeground());
        } else {
             
            if(rendez.isPassed()){
            renderer.setBackground(Color.gray.brighter());
            renderer.setForeground(Color.white);
            }else
            {
            renderer.setBackground(table.getBackground());
            renderer.setForeground(table.getForeground());
            }
        } 
        
     
      
             return renderer;     
    }
     

    
    
    
        
    }
