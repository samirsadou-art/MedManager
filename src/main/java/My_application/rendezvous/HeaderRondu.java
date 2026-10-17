/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.rendezvous;

import My_application.vue.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class HeaderRondu implements TableCellRenderer{
  protected DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
 
            
       JLabel renderer = (JLabel) defaultRenderer.getTableCellRendererComponent(table, value,isSelected, hasFocus, row, column);
        
       renderer.setFont(Theme.policeGras());
       renderer.setHorizontalAlignment(JLabel.CENTER);
       renderer.setPreferredSize(new Dimension(0, 40));
       renderer.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 1, Theme.BORDURE));

       // Colonne d'aujourd'hui en couleur principale (comme RendezVousRondu).
       boolean aujourdhui = table.getModel() instanceof RendezVousTableModel
               && java.time.LocalDate.now().equals(((RendezVousTableModel) table.getModel()).jourDeColonne(column));
       if (aujourdhui) {
           renderer.setBackground(Theme.PRIMAIRE);
           renderer.setForeground(Color.WHITE);
       } else {
           renderer.setBackground(Theme.PRIMAIRE_CLAIR);
           renderer.setForeground(Theme.PRIMAIRE_FONCE);
       }
       return renderer;
            
        
    }
        
        
        
        
    
    
}
