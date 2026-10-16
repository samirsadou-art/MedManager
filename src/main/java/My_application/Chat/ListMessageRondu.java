/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.Chat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.text.SimpleDateFormat;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class ListMessageRondu implements ListCellRenderer{


  protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();




 @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
       renderer.setHorizontalAlignment(JLabel.LEFT);
       Font fnt=new Font("Goudy Old Style",Font.PLAIN,20);
      
       renderer.setFont(fnt);
        
        message mesg=(message)value;
        String aut="";
        SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm");
        
        if(mesg.getAuteur()==0)
        {
            aut="Serveur";
            renderer.setBackground(Color.green);
            renderer.setForeground(Color.red.darker());
        }
        else{
            aut="Client";
        }
        renderer.setText("<html><b><i>"+aut+"</i></b> <font size=3 color='red'>["+dt1.format(mesg.getDate())+"]</font>: "+mesg.getTexte()+"</html>");
        
        
        
       
        
        
             
        
       /*  if (isSelected) {
            renderer.setBackground(list.getSelectionBackground());
            renderer.setForeground(list.getSelectionForeground());
        } else {
            renderer.setBackground(list.getBackground());
            renderer.setForeground(list.getForeground());
        } 
        */
     
      
             return renderer;    
    }
     


    
    
        
    }
