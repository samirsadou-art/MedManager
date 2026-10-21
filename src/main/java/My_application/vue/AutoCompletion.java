package My_application.vue;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * Auto-complétion d'une liste déroulante éditable.
 * <p>
 * Ce code (≈ 40 lignes) était copié-collé quatre fois (symptômes,
 * pathologies, médicaments, analyses), avec une requête SQL dans chaque
 * copie. La source des suggestions est maintenant fournie par le contrôleur.
 * <p>
 * La recherche suit les modifications du texte (et non plus les codes de
 * touches : les lettres accentuées, les chiffres du haut du clavier et le
 * collage sont pris en compte). Elle est temporisée : une seule recherche
 * part {@value #DELAI_MS} ms après la dernière frappe, au lieu d'une requête
 * par touche.
 */
public final class AutoCompletion {

    /** Pause de frappe après laquelle les suggestions sont recherchées. */
    public static final int DELAI_MS = 250;

    /** Fournit les suggestions pour un préfixe (appel au contrôleur). */
    public interface Source<T> {
        List<T> suggestions(String prefixe);
    }

    private AutoCompletion() {
    }

    public static <T> void installer(JComboBox combo, DefaultComboBoxModel modele,
            boolean majuscules, Source<T> source) {
        installer(combo, modele, majuscules, source, DELAI_MS);
    }

    static <T> Ecouteur<T> installer(JComboBox combo, DefaultComboBoxModel modele,
            boolean majuscules, Source<T> source, int delaiMs) {
        combo.setModel(modele);
        Ecouteur<T> ecouteur = new Ecouteur<>(combo, modele, majuscules, source, delaiMs);
        editeur(combo).getDocument().addDocumentListener(ecouteur);
        return ecouteur;
    }

    private static JTextComponent editeur(JComboBox combo) {
        return (JTextComponent) combo.getEditor().getEditorComponent();
    }

    /** Relance le minuteur à chaque modification, puis met à jour la liste. */
    static final class Ecouteur<T> implements DocumentListener, ActionListener {

        private final JComboBox combo;
        private final DefaultComboBoxModel modele;
        private final boolean majuscules;
        private final Source<T> source;
        final Timer minuteur;
        /** Vrai pendant que l'on réécrit soi-même le texte (pas de nouvelle recherche). */
        private boolean miseAJour;
        /** Dernière modification : un ajout (on propose la suite) ou un effacement. */
        private boolean ajout;

        Ecouteur(JComboBox combo, DefaultComboBoxModel modele, boolean majuscules, Source<T> source,
                int delaiMs) {
            this.combo = combo;
            this.modele = modele;
            this.majuscules = majuscules;
            this.source = source;
            this.minuteur = new Timer(delaiMs, this);
            this.minuteur.setRepeats(false);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            modifie(true);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            modifie(false);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            // Attributs de style seulement : rien à faire.
        }

        private void modifie(boolean insertion) {
            if (miseAJour) {
                return;
            }
            ajout = insertion;
            minuteur.restart();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String saisie = editeur(combo).getText();
            if (majuscules) {
                saisie = saisie.toUpperCase();
            }
            miseAJour = true;
            try {
                modele.removeAllElements();
                if (!saisie.trim().isEmpty()) {
                    for (T t : source.suggestions(saisie)) {
                        modele.addElement(t);
                    }
                }
                combo.getEditor().setItem(saisie);
                if (modele.getSize() == 0) {
                    combo.hidePopup();
                    return;
                }
                if (combo.isShowing()) {
                    combo.showPopup();
                }
                if (ajout) {
                    // removeAllElements/addElement a sélectionné la première suggestion :
                    // on l'affiche et on surligne la partie proposée.
                    Object premiere = modele.getElementAt(0);
                    String texte = premiere.toString();
                    if (texte.toUpperCase().startsWith(saisie.toUpperCase())) {
                        combo.getEditor().setItem(saisie + texte.substring(saisie.length()));
                        editeur(combo).select(saisie.length(), texte.length());
                    } else {
                        combo.getEditor().setItem(saisie);
                    }
                } else {
                    combo.getEditor().setItem(saisie);
                }
            } finally {
                miseAJour = false;
            }
        }
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
