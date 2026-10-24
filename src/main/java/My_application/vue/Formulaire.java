package My_application.vue;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Petite boîte de saisie modale : un titre, une explication, des lignes
 * « libellé : champ » et deux boutons. {@link #afficher} rend la main quand
 * la validation a réussi (vrai) ou que l'utilisateur a annulé (faux) : en
 * cas de refus, le contrôleur a déjà affiché le message et la boîte reste
 * ouverte avec ce qui a été saisi.
 */
public final class Formulaire {

    private final JDialog dialogue;
    private final JPanel champs = new JPanel(new GridBagLayout());
    private final JButton valider;
    private int lignes;
    private boolean valide;

    public Formulaire(Component parent, String titre, String icone, String explication, String libelleValider) {
        Window fenetre = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (parent instanceof Window) {
            fenetre = (Window) parent;
        }
        dialogue = new JDialog(fenetre, titre, Dialog.ModalityType.APPLICATION_MODAL);
        dialogue.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel contenu = new JPanel(new BorderLayout(0, 14));
        contenu.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        contenu.setBackground(Theme.SURFACE);

        JLabel entete = new JLabel(titre, Icones.svg(icone, 24, Theme.PRIMAIRE), JLabel.LEFT);
        entete.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE + 3));
        entete.setForeground(Theme.TEXTE);
        entete.setIconTextGap(10);
        JPanel haut = new JPanel(new BorderLayout(0, 6));
        haut.setOpaque(false);
        haut.add(entete, BorderLayout.NORTH);
        if (explication != null) {
            JLabel aide = new JLabel("<html><div style='width:340px'>" + explication + "</div></html>");
            aide.setForeground(Theme.TEXTE_SECONDAIRE);
            haut.add(aide, BorderLayout.CENTER);
        }
        contenu.add(haut, BorderLayout.NORTH);

        champs.setOpaque(false);
        contenu.add(champs, BorderLayout.CENTER);

        valider = new JButton(libelleValider);
        valider.setBackground(Theme.PRIMAIRE);
        valider.setForeground(java.awt.Color.WHITE);
        valider.setFont(Theme.policeGras());
        JButton annuler = new JButton("Annuler");
        annuler.addActionListener(e -> dialogue.dispose());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        boutons.setOpaque(false);
        boutons.add(annuler);
        boutons.add(valider);
        contenu.add(boutons, BorderLayout.SOUTH);

        dialogue.getRootPane().setDefaultButton(valider);
        dialogue.setContentPane(contenu);
    }

    /** Ajoute une ligne « libellé : champ » ; un libellé null donne une ligne pleine largeur. */
    public Formulaire ligne(String libelle, JComponent champ) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = lignes++;
        c.insets = new Insets(4, 0, 4, 12);
        c.anchor = GridBagConstraints.LINE_START;
        if (libelle != null) {
            JLabel l = new JLabel(libelle);
            l.setForeground(Theme.TEXTE);
            champs.add(l, c);
            c.gridx = 1;
        } else {
            c.gridwidth = 2;
        }
        c.insets = new Insets(4, 0, 4, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        champs.add(champ, c);
        return this;
    }

    /** Pour les messages d'erreur du contrôleur, affichés au-dessus de la boîte. */
    public JDialog getDialogue() {
        return dialogue;
    }

    /**
     * Affiche la boîte et attend.
     *
     * @param validation appelée à chaque clic sur le bouton de validation ;
     *                   vraie si la saisie est acceptée (la boîte se ferme)
     * @return vrai si la saisie a été acceptée, faux si l'utilisateur a annulé
     */
    public boolean afficher(BooleanSupplier validation) {
        valider.addActionListener(e -> {
            if (validation.getAsBoolean()) {
                valide = true;
                dialogue.dispose();
            }
        });
        Theme.harmoniser(dialogue);
        dialogue.pack();
        dialogue.setResizable(false);
        dialogue.setLocationRelativeTo(dialogue.getOwner());
        dialogue.setVisible(true);
        return valide;
    }
}
