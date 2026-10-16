/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.rendezvous;

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
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class RendezVousRondu implements TableCellRenderer{
    
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
      
    Border raisedBorder = BorderFactory.createRaisedSoftBevelBorder();
 
    lab.setBorder(raisedBorder);   
        
        if(column==0){
          
   

         lab.setBackground(new Color(238,232,170));
         lab.setFont(new Font("Thoma",Font.BOLD,18) );
         lab.setForeground(Color.BLACK);
         lab.setHorizontalAlignment(JLabel.CENTER);



         return lab;
         

        
    
        }
        else
        {
            
       
         lab.setBackground(new Color(255,222,173));
           
           
           // Surligne la colonne d'aujourd'hui (avant : le même jour de la
           // semaine était surligné quelle que soit la semaine affichée).
           if (table.getModel() instanceof RendezVousTableModel
                   && java.time.LocalDate.now().equals(((RendezVousTableModel) table.getModel()).jourDeColonne(column))) {
               lab.setBackground(new Color(255,228,225));
           }
            if(value!=null){
            
            DefaultListModel listmodel=(DefaultListModel) value;
            if(listmodel.size()>0){
            
            JList list=new JList();
            list.setFont(new Font("Arial",Font.PLAIN,9));
            JScrollPane span=new JScrollPane(list);
            Border lowerBorder = BorderFactory.createLineBorder(Color.gray,2);
 
            span.setBorder(lowerBorder);   
            
            
            
            list.setModel(listmodel);
            list.setCellRenderer(new listRendezVousRondu(0,malade));
            if(list.getModel().getSize()==NBH)
    
            list.setBackground(new Color(255,40,10));
      
          
            
            
           
            return span;
            }

           
            }
            
            
 
      
      
            
            
            
            
            
        }
    
   return lab;
    }
}
