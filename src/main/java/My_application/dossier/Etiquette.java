package My_application.dossier;

import My_application.service.SyntheseDossier.Interpretation;
import My_application.service.SyntheseDossier.NiveauTension;
import My_application.vue.Theme;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/** Petite étiquette arrondie colorée : « Normal », « En attente », « 4 fois »… */
public class Etiquette extends JLabel {

    private static final long serialVersionUID = 1L;

    private final Color fond;

    public Etiquette(String texte, Color couleur, Color fond) {
        super(texte);
        this.fond = fond;
        setForeground(couleur);
        setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE - 1));
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        setOpaque(false);
    }

    /** Couleurs de la note d'interprétation, celles de la saisie des résultats. */
    public static Etiquette de(Interpretation i) {
        return new Etiquette(i.getLibelle(), couleur(i), fond(i));
    }

    public static Color couleur(Interpretation i) {
        switch (i) {
            case NORMAL:
                return Theme.SUCCES;
            case A_SURVEILLER:
                return Theme.AVERTISSEMENT;
            case ANORMAL:
                return Theme.DANGER;
            default:
                return Theme.TEXTE_SECONDAIRE;
        }
    }

    public static Color fond(Interpretation i) {
        switch (i) {
            case NORMAL:
                return Theme.SUCCES_CLAIR;
            case A_SURVEILLER:
                return Theme.AVERTISSEMENT_CLAIR;
            case ANORMAL:
                return Theme.DANGER_CLAIR;
            default:
                return Theme.FOND;
        }
    }

    /** Couleur du texte d'une tension : rouge si élevée, orange si limite. */
    public static Color couleur(NiveauTension n) {
        if (n == NiveauTension.ELEVEE) {
            return Theme.DANGER;
        }
        return n == NiveauTension.LIMITE ? Theme.AVERTISSEMENT : Theme.TEXTE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fond);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
