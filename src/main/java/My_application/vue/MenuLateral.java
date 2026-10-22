package My_application.vue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Menu de navigation vertical de la fenêtre principale (Accueil, Patients,
 * Rendez-vous…) : pictogramme et libellé, entrée active sur fond de la
 * couleur principale.
 */
public class MenuLateral extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int LARGEUR = 220;

    private final JPanel entrees = new JPanel();
    /** Bas du menu : entrée « Se déconnecter » au-dessus du pied. */
    private final JPanel bas = new JPanel(new BorderLayout(0, 12));
    private final List<Entree> liste = new ArrayList<>();

    public MenuLateral() {
        super(new BorderLayout());
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDURE),
                BorderFactory.createEmptyBorder(16, 12, 16, 12)));
        entrees.setOpaque(false);
        entrees.setLayout(new BoxLayout(entrees, BoxLayout.Y_AXIS));
        add(entrees, BorderLayout.NORTH);
        bas.setOpaque(false);
        add(bas, BorderLayout.SOUTH);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(LARGEUR, super.getPreferredSize().height);
    }

    /**
     * Ajoute une entrée. Si {@code active} est vrai, un clic la sélectionne
     * (une page de la fenêtre) ; sinon elle ouvre seulement une autre fenêtre.
     */
    public void ajouter(String icone, String texte, boolean active, Runnable action) {
        Entree e = new Entree(icone, texte, active, action);
        liste.add(e);
        entrees.add(e);
        entrees.add(Box.createVerticalStrut(4));
    }

    /**
     * Entrée posée en bas du menu, à l'écart des pages (« Se déconnecter ») :
     * même présentation que les autres, sans jamais rester sélectionnée.
     */
    public void ajouterEnBas(String icone, String texte, Runnable action) {
        Entree e = new Entree(icone, texte, false, action);
        liste.add(e);
        bas.add(e, BorderLayout.NORTH);
    }

    /** Sélectionne l'entrée portant ce libellé. */
    public void selectionner(String texte) {
        for (Entree e : liste) {
            e.choisie = e.getText().equals(texte);
            e.peindre();
        }
    }

    /** Pastille (nombre de messages non lus…) sur l'entrée portant ce libellé ; 0 l'enlève. */
    public void setBadge(String texte, int nombre) {
        for (Entree e : liste) {
            if (e.getText().equals(texte)) {
                e.badge = nombre;
                e.repaint();
            }
        }
    }

    /** Texte discret en bas du menu (version, éditeur…). */
    public void setPied(String html) {
        JLabel pied = new JLabel("<html>" + html + "</html>", Icones.svg("heartbeat", 28, Theme.PRIMAIRE), JLabel.LEFT);
        pied.setIconTextGap(8);
        pied.setFont(Theme.police(Font.PLAIN, 11));
        pied.setForeground(Theme.TEXTE_SECONDAIRE);
        pied.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDURE),
                BorderFactory.createEmptyBorder(12, 6, 0, 0)));
        bas.add(pied, BorderLayout.SOUTH);
    }

    private final class Entree extends JLabel {

        private static final long serialVersionUID = 1L;
        private final String icone;
        private boolean choisie;
        private boolean survolee;
        private int badge;

        Entree(String icone, String texte, final boolean active, final Runnable action) {
            super(texte);
            this.icone = icone;
            setFont(Theme.police(Font.BOLD, 14));
            setIconTextGap(14);
            setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    survolee = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    survolee = false;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (contains(e.getPoint())) {
                        if (active) {
                            selectionner(getText());
                        }
                        action.run();
                    }
                }
            });
            peindre();
        }

        void peindre() {
            Color c = choisie ? Color.WHITE : Theme.TEXTE;
            setForeground(c);
            setIcon(Icones.svg(icone, 22, choisie ? Color.WHITE : Theme.PRIMAIRE));
            repaint();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (choisie || survolee) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(choisie ? Theme.PRIMAIRE : Theme.PRIMAIRE_CLAIR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
            super.paintComponent(g);
            if (badge > 0) {
                peindreBadge(g);
            }
        }

        private void peindreBadge(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            String texte = badge > 99 ? "99+" : String.valueOf(badge);
            g2.setFont(Theme.police(Font.BOLD, 11));
            int largeur = Math.max(20, g2.getFontMetrics().stringWidth(texte) + 12);
            int hauteur = 20;
            int x = getWidth() - largeur - 10;
            int y = (getHeight() - hauteur) / 2;
            g2.setColor(choisie ? Color.WHITE : Theme.DANGER);
            g2.fillRoundRect(x, y, largeur, hauteur, hauteur, hauteur);
            g2.setColor(choisie ? Theme.DANGER : Color.WHITE);
            int tx = x + (largeur - g2.getFontMetrics().stringWidth(texte)) / 2;
            int ty = y + (hauteur + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
            g2.drawString(texte, tx, ty);
            g2.dispose();
        }
    }
}
