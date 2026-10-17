/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.Chat;

import My_application.vue.Theme;
import java.awt.Component;
import javax.swing.BorderFactory;
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
       renderer.setFont(Theme.police());
       renderer.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        message mesg=(message)value;
        String aut="";
        SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm");

        if(mesg.getAuteur()==0)
        {
            aut="Serveur";
            renderer.setBackground(Theme.PRIMAIRE_CLAIR);
            renderer.setForeground(Theme.TEXTE);
        }
        else{
            aut="Client";
            renderer.setBackground(Theme.SURFACE);
            renderer.setForeground(Theme.TEXTE);
        }
        renderer.setText("<html><b>"+aut+"</b> <font color='#6B7C8F'>"+dt1.format(mesg.getDate())+"</font><br>"+mesg.getTexte()+"</html>");
        
        
        
       
        
        
             
        
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
