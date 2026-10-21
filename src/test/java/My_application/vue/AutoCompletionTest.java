package My_application.vue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;
import org.junit.Before;
import org.junit.Test;

public class AutoCompletionTest {

    private final List<String> recherches = new ArrayList<>();
    private final JComboBox<String> combo = new JComboBox<>();
    private final DefaultComboBoxModel<String> modele = new DefaultComboBoxModel<>();
    private AutoCompletion.Ecouteur<String> ecouteur;

    @Before
    public void installer() {
        combo.setEditable(true);
        ecouteur = AutoCompletion.installer(combo, modele, true, new AutoCompletion.Source<String>() {
            @Override
            public List<String> suggestions(String prefixe) {
                recherches.add(prefixe);
                List<String> r = new ArrayList<>();
                for (String s : Arrays.asList("FIÈVRE", "FATIGUE", "2E MOLAIRE")) {
                    if (s.startsWith(prefixe)) {
                        r.add(s);
                    }
                }
                return r;
            }
        }, 10_000);
    }

    private JTextComponent editeur() {
        return (JTextComponent) combo.getEditor().getEditorComponent();
    }

    @Test
    public void uneSeuleRechercheApresPlusieursFrappes() {
        editeur().setText("f");
        editeur().setText("fi");
        editeur().setText("fiè");
        assertTrue(recherches.isEmpty());
        assertTrue("le minuteur est relancé à chaque frappe", ecouteur.minuteur.isRunning());
        ecouteur.actionPerformed(null);
        assertEquals(Arrays.asList("FIÈ"), recherches);
        assertEquals(1, modele.getSize());
        assertEquals("FIÈVRE", modele.getElementAt(0));
    }

    @Test
    public void lesAccentsEtLesChiffresDeclenchentLaRecherche() {
        editeur().setText("2");
        assertTrue(ecouteur.minuteur.isRunning());
        ecouteur.actionPerformed(null);
        assertEquals("2E MOLAIRE", modele.getElementAt(0));
    }

    @Test
    public void laSuiteProposeeEstSurligneeEtNeRelancePasDeRecherche() {
        ecouteur.minuteur.stop();
        editeur().setText("fa");
        ecouteur.actionPerformed(null);
        ecouteur.minuteur.stop();
        assertEquals("FATIGUE", editeur().getText());
        assertEquals("TIGUE", editeur().getSelectedText());
        editeur().setText("FAT");
        assertTrue(ecouteur.minuteur.isRunning());
        assertEquals(1, recherches.size());
    }

    @Test
    public void champVideSansSuggestion() {
        editeur().setText("x");
        editeur().setText("");
        ecouteur.actionPerformed(null);
        assertEquals(0, modele.getSize());
        assertFalse(recherches.contains(""));
    }
}
