/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.modelTableau;

import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class TextAreaEditeur  extends DefaultCellEditor {
   protected JScrollPane scrollpane;
   protected JTextArea textarea;
  
   public TextAreaEditeur() {
      super(new JCheckBox());
      scrollpane = new JScrollPane();
      textarea = new JTextArea(); 
      textarea.setLineWrap(true);
      textarea.setWrapStyleWord(true);
      scrollpane.getViewport().add(textarea);
   }
  
   public Component getTableCellEditorComponent(JTable table, Object value,
                                   boolean isSelected, int row, int column) {
      textarea.setText((String) value);
  
      return scrollpane;
   }
  
   public Object getCellEditorValue() {
      return textarea.getText();
   }
}