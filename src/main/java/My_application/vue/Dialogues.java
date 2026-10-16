package My_application.vue;

import java.awt.Component;
import javax.swing.JOptionPane;

/** Implémentation Swing commune des messages d'une {@code Vue}. */
public final class Dialogues {

    private Dialogues() {
    }

    public static void erreur(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    public static void information(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
}
