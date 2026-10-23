/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.modelTableau;

import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

/**
 * Arbre de l'historique des visites : la catégorie des consultations puis
 * une ligne par visite.
 *
 * @author GLOBAL INFO LAGA
 */
public class HistoriqueRondu  implements TreeCellRenderer {

    private static final Icon CONSULTATIONS = Icones.svg("stethoscope", 22, Theme.PRIMAIRE);
    private static final Icon CONSULTATION = Icones.svg("history", 16, Theme.TEXTE_SECONDAIRE);

        private final JLabel label;
        private final DefaultTreeCellRenderer render;

       public  HistoriqueRondu() {
            label = new JLabel();
            label.setOpaque(true);
            label.setIconTextGap(8);
            label.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 8));
            render=new DefaultTreeCellRenderer();
        }


    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

          DefaultMutableTreeNode noeud= (DefaultMutableTreeNode) value;
          NoeudHistorique  NH=(NoeudHistorique) noeud.getUserObject();

          Icon icone = null;
          Font police = Theme.police();
          if (NH.getItem().equals("Consultations")) {
              icone = CONSULTATIONS;
              police = Theme.police(Font.BOLD, Theme.TAILLE_BASE + 1);
          } else if (noeud.isLeaf() && NH.getType() == 0) {
              icone = CONSULTATION;
          }
          if (icone == null) {
              return render.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
          }

          label.setIcon(icone);
          label.setText(NH.getItem());
          label.setFont(police);
          label.setBackground(selected ? Theme.PRIMAIRE_CLAIR : tree.getBackground());
          label.setForeground(selected ? Theme.PRIMAIRE_FONCE : Theme.TEXTE);
          return label;
    }
}
