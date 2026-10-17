/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.rendezvous;

import My_application.vue.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import My_application.entities.patient;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class RendezVousRondu implements TableCellRenderer{

    /** Fond de la colonne du jour : teinte très claire de la couleur principale. */
    private static final Color AUJOURDHUI = new Color(0xF0F8F9);

    patient malade=null;
    int NBH=4;

    public RendezVousRondu(patient malade,int nbrH) {
        this.malade=malade;
        NBH=nbrH;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(); 
     JLabel lab = (JLabel) renderer.getTableCellRendererComponent(table,value, isSelected, hasFocus, row, column);  
      
    lab.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDURE));

        if(column==0){
         // Colonne des heures
         lab.setBackground(Theme.FOND);
         lab.setFont(Theme.policeGras());
         lab.setForeground(Theme.TEXTE_SECONDAIRE);
         lab.setHorizontalAlignment(JLabel.CENTER);



         return lab;
         

        
    
        }
        else
        {
            
       
         lab.setBackground(Theme.SURFACE);


           // Surligne la colonne d'aujourd'hui (avant : le même jour de la
           // semaine était surligné quelle que soit la semaine affichée).
           if (table.getModel() instanceof RendezVousTableModel
                   && java.time.LocalDate.now().equals(((RendezVousTableModel) table.getModel()).jourDeColonne(column))) {
               lab.setBackground(AUJOURDHUI);
           }
            if(value!=null){

            DefaultListModel listmodel=(DefaultListModel) value;
            if(listmodel.size()>0){

            JList list=new JList();
            list.setFont(Theme.police(Font.PLAIN, 12));
            list.setBackground(lab.getBackground());
            JScrollPane span=new JScrollPane(list);
            span.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDURE));



            list.setModel(listmodel);
            list.setCellRenderer(new listRendezVousRondu(0,malade));
            // Créneau horaire complet
            if(list.getModel().getSize()==NBH)
            list.setBackground(Theme.DANGER_CLAIR);
      
          
            
            
           
            return span;
            }

           
            }
            
            
 
      
      
            
            
            
            
            
        }
    
   return lab;
    }
}
