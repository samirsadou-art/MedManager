/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.modelTableau;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class HistoriqueRondu  implements TreeCellRenderer {
        private JLabel label;
        DefaultTreeCellRenderer render;

       public  HistoriqueRondu() {
            label = new JLabel();
            render=new DefaultTreeCellRenderer();
        }


    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      
          DefaultMutableTreeNode noeud= (DefaultMutableTreeNode) value;
          NoeudHistorique  NH=(NoeudHistorique) noeud.getUserObject();
          
          if(NH.getItem().equals("Consultations")){
                 
                URL imageUrl = getClass().getResource("/res/icons/consult48.png");
                if (imageUrl != null) {
                    label.setIcon(new ImageIcon(imageUrl));
                }
                label.setText(NH.getItem());
              
                label.setFont(new Font("Arial", Font.BOLD, 14));
                return label;
           
              
          }
          
          if(NH.getItem().equals("Rendez-Vous")){
                 
                URL imageUrl = getClass().getResource("/res/icons/rendez48.png");
                if (imageUrl != null) {
                    label.setIcon(new ImageIcon(imageUrl));
                }
                label.setText(NH.getItem());
                 label.setFont(new Font("Arial", Font.BOLD, 14));
                return label;
           
          }     
          
         
          if(noeud.isLeaf())
            if (NH.getType()==0) {
       
                URL imageUrl = getClass().getResource("/res/icons/date1_16.png");
                if (imageUrl != null) {
                    label.setIcon(new ImageIcon(imageUrl));
                }
                label.setText(NH.getItem());
            
                label.setFont(new Font("Goudy Old Style", Font.ITALIC, 20));
                label.setIconTextGap(5);
             
 
                return label;
        
              
        
        
          }
          
            else if(NH.getIndiceConsult()==1){
                
              URL imageUrl = getClass().getResource("/res/icons/jour16.png");
                if (imageUrl != null) {
                    label.setIcon(new ImageIcon(imageUrl));
                }
                label.setText(NH.getItem());
                return label;
        
                
                
                
            }
          
        
        return  render.getTreeCellRendererComponent(tree, value, leaf, expanded, leaf, row, hasFocus);
    }
}