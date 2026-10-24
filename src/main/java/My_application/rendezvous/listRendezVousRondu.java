/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.rendezvous;

import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.Component;
import My_application.entities.Rendez_Vous;
import My_application.entities.patient;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class listRendezVousRondu implements ListCellRenderer {
Icon imageIcon;
int mode;
patient malade=null;
  protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

   public listRendezVousRondu (int mode, patient malade) {
       
     this.mode=mode;  
     this.malade=malade;
    } 

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      
       JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,isSelected, cellHasFocus);
        
        
        
        Rendez_Vous rendez=(Rendez_Vous)value;
        
        String patient = rendez.getHeureString()+"  "+rendez.getPatient().getNom()+" "+rendez.getPatient().getPrenom();
        renderer.setFont(list.getFont());
        renderer.setIconTextGap(6);
        
       
        
        
        
        imageIcon=getIcon(rendez);
        
        renderer.setIcon(imageIcon);
        renderer.setText(patient); 

        
        
        
         if (isSelected) {
            renderer.setBackground(list.getSelectionBackground());
            renderer.setForeground(list.getSelectionForeground());
        } else {
            renderer.setBackground(list.getBackground());
            renderer.setForeground(Theme.TEXTE);
        }

      // Rendez-vous du patient pour lequel on prend rendez-vous
      if(malade!=null)
      if(rendez.getPatient().getIdentPat()==malade.getIdentPat() && !isSelected){
          renderer.setBackground(Theme.SUCCES_CLAIR);
          renderer.setForeground(Theme.SUCCES);
        
         
      }
     
      
             return renderer; 
        
        
    }
   
 
    Icon getIcon(Rendez_Vous rendez){
     if(mode==0) 
         return Icones.patient(rendez.getPatient(), 16);
     else
         return Icones.patient(rendez.getPatient(), 72);
        
    }
        
        
    }
    


