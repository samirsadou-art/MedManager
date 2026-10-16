package My_application.vue;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

/**
 * Auto-complétion d'une liste déroulante éditable.
 * <p>
 * Ce code (≈ 40 lignes) était copié-collé quatre fois (symptômes,
 * pathologies, médicaments, analyses), avec une requête SQL dans chaque
 * copie. La source des suggestions est maintenant fournie par le contrôleur.
 */
public final class AutoCompletion {

    /** Fournit les suggestions pour un préfixe (appel au contrôleur). */
    public interface Source<T> {
        List<T> suggestions(String prefixe);
    }

    private AutoCompletion() {
    }

    public static <T> void installer(final JComboBox combo, final DefaultComboBoxModel modele,
            final boolean majuscules, final Source<T> source) {
        combo.setModel(modele);
        combo.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                boolean caractere = (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z)
                        || (code >= KeyEvent.VK_NUMPAD0 && code <= KeyEvent.VK_NUMPAD9)
                        || code == KeyEvent.VK_BACK_SPACE;
                if (!caractere) {
                    return;
                }
                String saisie = combo.getEditor().getItem().toString();
                if (majuscules) {
                    saisie = saisie.toUpperCase();
                }
                modele.removeAllElements();
                if (!saisie.isEmpty()) {
                    for (T t : source.suggestions(saisie)) {
                        modele.addElement(t);
                    }
                }
                combo.getEditor().setItem(saisie);
                if (modele.getSize() > 0) {
                    combo.showPopup();
                    if (code != KeyEvent.VK_BACK_SPACE) {
                        JTextComponent editeur = (JTextComponent) combo.getEditor().getEditorComponent();
                        editeur.select(saisie.length(), combo.getEditor().getItem().toString().length());
                    } else {
                        combo.getEditor().setItem(saisie);
                    }
                } else {
                    combo.hidePopup();
                }
            }
        });
    }

    /** Texte actuellement saisi dans l'éditeur de la liste. */
    public static String saisie(JComboBox combo) {
        Object o = combo.getEditor().getItem();
        return o == null ? "" : o.toString().trim();
    }

    /**
     * Élément choisi dans la liste, uniquement s'il correspond au texte saisi
     * (sinon l'utilisateur a tapé un libellé libre).
     */
    @SuppressWarnings("unchecked")
    public static <T> T selection(JComboBox combo, Class<T> type) {
        Object o = combo.getSelectedItem();
        if (type.isInstance(o) && o.toString().equalsIgnoreCase(saisie(combo))) {
            return (T) o;
        }
        return null;
    }
}
