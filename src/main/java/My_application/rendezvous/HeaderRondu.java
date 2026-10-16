/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.rendezvous;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class HeaderRondu implements TableCellRenderer{
  protected DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
 
            
       JLabel renderer = (JLabel) defaultRenderer.getTableCellRendererComponent(table, value,isSelected, hasFocus, row, column);
        
       renderer.setHorizontalAlignment(JLabel.LEFT);
       renderer.setFont(new Font("Thoma",Font.BOLD,15));
        
       
          JTableHeader th = table.getTableHeader();
           TableColumnModel tcm = th.getColumnModel();
           TableColumn tc = tcm.getColumn(column);
           String title=(String) tc.getHeaderValue();
           String[] chaines= title.split(" ");
           
           Calendar today=Calendar.getInstance();
           SimpleDateFormat    formater = null;
           formater = new SimpleDateFormat("EEEE");
           String date=formater.format(today.getTime());

          renderer.setHorizontalAlignment(JLabel.CENTER); 
          renderer.setPreferredSize(new Dimension(0, 45));
           Border raisedBorder = BorderFactory.createRaisedBevelBorder();
        
           renderer.setBorder(raisedBorder);
      
           
           if(chaines.length==2)
           if(date.toUpperCase().equals(chaines[0].toUpperCase())){
             
               renderer.setBackground(new Color(0,191,255));
               renderer.setForeground(Color.white);

               
    
                
           }
           else{
               renderer.setBackground(Color.cyan.darker());
               renderer.setForeground(Color.white);

               
           }
           return renderer;
            
        
    }
        
        
        
        
    
    
}
