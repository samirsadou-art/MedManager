package My_application.vue;

import java.util.regex.Pattern;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * Limite ce qu'un champ peut contenir : caractères permis et longueur.
 * <p>
 * Remplace les filtres {@code keyTyped} des formulaires, qu'un simple collage
 * (Ctrl+V) ou un glisser-déposer contournait : un {@link DocumentFilter}
 * contrôle le texte qui en résulterait, quelle que soit sa provenance.
 * La validation métier (bornes des constantes vitales) reste dans le service.
 */
public final class FiltreSaisie extends DocumentFilter {

    private final Pattern permis;
    private final int longueurMax;

    /**
     * @param caracteres classe de caractères permis, au format d'une expression
     *                   régulière (ex. {@code "0-9.,"})
     */
    public FiltreSaisie(String caracteres, int longueurMax) {
        this.permis = Pattern.compile("[" + caracteres + "]*");
        this.longueurMax = longueurMax;
    }

    /** Chiffres seulement. */
    public static FiltreSaisie entier(int longueurMax) {
        return new FiltreSaisie("0-9", longueurMax);
    }

    /** Chiffres et séparateur décimal (point ou virgule). */
    public static FiltreSaisie decimal(int longueurMax) {
        return new FiltreSaisie("0-9.,", longueurMax);
    }

    public static void installer(JTextComponent champ, FiltreSaisie filtre) {
        ((AbstractDocument) champ.getDocument()).setDocumentFilter(filtre);
    }

    /**
     * Affiche une valeur lue en base sans la filtrer : une ancienne valeur hors
     * format (saisie avant le filtre) reste visible au lieu de disparaître, ce
     * qui l'effacerait à l'enregistrement suivant.
     */
    public static void afficher(JTextComponent champ, String texte) {
        AbstractDocument document = (AbstractDocument) champ.getDocument();
        DocumentFilter filtre = document.getDocumentFilter();
        document.setDocumentFilter(null);
        try {
            champ.setText(texte);
        } finally {
            document.setDocumentFilter(filtre);
        }
    }

    /** Vrai si le texte complet obtenu après la modification est acceptable. */
    boolean accepte(String resultat) {
        return resultat.length() <= longueurMax && permis.matcher(resultat).matches();
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String texte, AttributeSet attr)
            throws BadLocationException {
        replace(fb, offset, 0, texte, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int longueur, String texte, AttributeSet attr)
            throws BadLocationException {
        String actuel = fb.getDocument().getText(0, fb.getDocument().getLength());
        String ajout = texte == null ? "" : texte;
        String resultat = actuel.substring(0, offset) + ajout + actuel.substring(offset + longueur);
        if (accepte(resultat)) {
            fb.replace(offset, longueur, ajout, attr);
        }
    }
}
