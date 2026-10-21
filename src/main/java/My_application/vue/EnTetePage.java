package My_application.vue;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;

/**
 * En-tête de page commun à toutes les fenêtres : pictogramme, titre,
 * sous-titre et, à droite, les actions de la page (comme l'agenda des
 * rendez-vous).
 * <p>
 * {@link #installer} l'ajoute au-dessus du contenu d'une fenêtre existante
 * (formulaires NetBeans compris) sans toucher au code généré. À appeler après
 * {@link Theme#harmoniser}, qui ne doit pas retoucher ses polices.
 */
public class EnTetePage extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel titre = new JLabel();
    private final JLabel sousTitre = new JLabel();
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

    public EnTetePage(String icone, String titre, String sousTitre) {
        super(new BorderLayout(16, 0));
        setOpaque(false);
        this.titre.setText(titre);
        this.titre.setFont(Theme.police(Font.BOLD, 22));
        this.titre.setForeground(Theme.TEXTE);
        this.sousTitre.setFont(Theme.police(Font.PLAIN, 13));
        this.sousTitre.setForeground(Theme.TEXTE_SECONDAIRE);
        setSousTitre(sousTitre);

        JPanel textes = new JPanel();
        textes.setOpaque(false);
        textes.setLayout(new BoxLayout(textes, BoxLayout.Y_AXIS));
        textes.add(this.titre);
        textes.add(this.sousTitre);
        JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        gauche.setOpaque(false);
        if (icone != null) {
            gauche.add(new JLabel(Icones.svg(icone, 36, Theme.PRIMAIRE)));
        }
        gauche.add(textes);
        add(gauche, BorderLayout.WEST);
        actions.setOpaque(false);
        add(actions, BorderLayout.EAST);
    }

    public void setTitre(String texte) {
        titre.setText(texte);
    }

    public final void setSousTitre(String texte) {
        sousTitre.setText(texte == null ? "" : texte);
        sousTitre.setVisible(texte != null && !texte.isEmpty());
    }

    /** Ajoute un composant (bouton, badge…) à droite de l'en-tête. */
    public <T extends JComponent> T ajouterAction(T composant) {
        if (composant instanceof JButton) {
            JButton b = (JButton) composant;
            b.setPreferredSize(new Dimension(b.getPreferredSize().width + 8, 38));
            // Pas d'anneau de focus sur le premier bouton à l'ouverture.
            b.setFocusable(false);
        }
        actions.add(composant);
        return composant;
    }

    /** Bouton d'action de l'en-tête, avec son pictogramme. */
    public JButton ajouterBouton(String texte, String icone, Runnable action) {
        JButton b = new JButton(texte, icone == null ? null : Icones.svg(icone, 18, Theme.PRIMAIRE));
        b.addActionListener(e -> action.run());
        return ajouterAction(b);
    }

    /**
     * Place un en-tête au-dessus du contenu de la fenêtre. Une fenêtre déjà
     * dimensionnée est agrandie de la hauteur de l'en-tête.
     */
    public static EnTetePage installer(RootPaneContainer fenetre, String icone, String titre, String sousTitre) {
        EnTetePage entete = new EnTetePage(icone, titre, sousTitre);
        entete.setBorder(BorderFactory.createEmptyBorder(14, 20, 12, 20));
        Container ancien = fenetre.getContentPane();
        JPanel racine = new JPanel(new BorderLayout());
        racine.setBackground(Theme.FOND);
        racine.add(entete, BorderLayout.NORTH);
        racine.add(ancien, BorderLayout.CENTER);
        fenetre.setContentPane(racine);
        if (fenetre instanceof Window) {
            Window w = (Window) fenetre;
            if (w.getWidth() > 0 && w.getHeight() > 0) {
                w.setSize(Math.max(w.getWidth(), entete.getPreferredSize().width + 40),
                        w.getHeight() + entete.getPreferredSize().height);
            }
            w.validate();
        }
        return entete;
    }
}
