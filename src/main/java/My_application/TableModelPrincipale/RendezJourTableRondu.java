/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.TableModelPrincipale;

import java.util.Locale;
import java.time.LocalDateTime;
import My_application.rendezvous.Pastille;
import My_application.rendezvous.StatutRendezVous;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.Component;
import My_application.entities.Rendez_Vous;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;


/**
 *
 * @author GLOBAL INFO LAGA
 */
public class RendezJourTableRondu implements TableCellRenderer{
Icon imageIcon;

  protected DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();




  
   
 
 

 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      
       JLabel renderer = (JLabel) defaultRenderer.getTableCellRendererComponent(table, value,isSelected, hasFocus, row, column);
        
       renderer.setHorizontalAlignment(JLabel.LEFT);
       renderer.setIcon(null);
       Theme.cellule(renderer, table, isSelected, row);

        Rendez_Vous rendez=(Rendez_Vous)value;

        
        switch(column){
            
            case 0:imageIcon=Icones.patient(rendez.getPatient(), 32);
                   renderer.setHorizontalAlignment(JLabel.CENTER);
                   renderer.setIcon(imageIcon);
                   renderer.setText("");
                   break;
            case 1: renderer.setText(rendez.getPatient().getNom().toUpperCase(Locale.FRENCH)); renderer.setIcon(null);
                    renderer.setFont(Theme.policeGras());break;
            case 2: renderer.setText(rendez.getPatient().getPrenom().toUpperCase(Locale.FRENCH));break;
            case 3: renderer.setHorizontalAlignment(JLabel.CENTER);renderer.setText(rendez.getPatient().getStringAge()); break;
            case 4: renderer.setHorizontalAlignment(JLabel.CENTER); 
                    renderer.setText(rendez.getHeureString());
                    break;
            case 5: StatutRendezVous statut = StatutRendezVous.de(rendez, null, LocalDateTime.now());
                    renderer.setText(statut.getLibelle());
                    renderer.setIcon(new Pastille(statut.getCouleur(), 10));
                    renderer.setIconTextGap(6);
                    if (!isSelected) {
                        renderer.setForeground(statut.getCouleur());
                    }
                    return renderer;
            
            
            
        }
        
        
     
       
        
        
             
        
        // Rendez-vous passé : grisé.
        if (!isSelected && rendez.isPassed()) {
            renderer.setForeground(Theme.TEXTE_SECONDAIRE);
            renderer.setBackground(Theme.FOND);
        }
        
     
      
             return renderer;     
    }
     

    
    
    
        
    }
