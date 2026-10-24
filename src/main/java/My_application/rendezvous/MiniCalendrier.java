package My_application.rendezvous;

import My_application.service.RendezVousService;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Petit calendrier mensuel de l'agenda : la semaine affichée est surlignée,
 * aujourd'hui est entouré, et un clic sur un jour affiche sa semaine.
 */
public class MiniCalendrier extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Jour choisi dans le calendrier. */
    public interface Ecouteur {
        void jourChoisi(LocalDate jour);
    }

    private static final String[] JOURS = {"Di", "Lu", "Ma", "Me", "Je", "Ve", "Sa"};
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final int CASE_L = 36;
    private static final int CASE_H = 30;

    private final JLabel titre = new JLabel("", SwingConstants.CENTER);
    private final Grille grille = new Grille();
    private YearMonth mois;
    private LocalDate aujourdhui;
    private LocalDate dimancheSelectionne;
    private Ecouteur ecouteur;

    public MiniCalendrier(LocalDate aujourdhui) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        this.aujourdhui = aujourdhui;
        this.mois = YearMonth.from(aujourdhui);
        titre.setFont(Theme.police(Font.BOLD, 15));
        titre.setForeground(Theme.TEXTE);
        JPanel haut = new JPanel(new BorderLayout());
        haut.setOpaque(false);
        haut.add(bouton("chevron-left", "Mois précédent", -1), BorderLayout.WEST);
        haut.add(titre, BorderLayout.CENTER);
        haut.add(bouton("chevron-right", "Mois suivant", 1), BorderLayout.EAST);
        add(haut, BorderLayout.NORTH);
        add(grille, BorderLayout.CENTER);
        majTitre();
    }

    private JButton bouton(String icone, String aide, final int decalage) {
        JButton b = new JButton(Icones.svg(icone, 16, Theme.TEXTE_SECONDAIRE));
        b.setToolTipText(aide);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        b.addActionListener(e -> {
            mois = mois.plusMonths(decalage);
            majTitre();
        });
        return b;
    }

    private void majTitre() {
        titre.setText(AgendaSemaine.capitaliser(MOIS.format(mois)));
        grille.repaint();
    }

    public void setEcouteur(Ecouteur ecouteur) {
        this.ecouteur = ecouteur;
    }

    /** Surligne la semaine affichée par l'agenda et montre son mois. */
    public void setSemaine(LocalDate dimanche) {
        this.dimancheSelectionne = dimanche;
        YearMonth m = YearMonth.from(dimanche.plusDays(3));
        if (!m.equals(mois) && !YearMonth.from(dimanche).equals(mois)
                && !YearMonth.from(dimanche.plusDays(6)).equals(mois)) {
            mois = m;
        }
        majTitre();
    }

    public void setAujourdhui(LocalDate aujourdhui) {
        this.aujourdhui = aujourdhui;
        grille.repaint();
    }

    /** Premier jour affiché (dimanche de la première ligne). */
    private LocalDate premierJour() {
        return RendezVousService.dimancheDeLaSemaine(mois.atDay(1));
    }

    private class Grille extends JComponent {

        private static final long serialVersionUID = 1L;
        private Point survol;

        Grille() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter souris = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    survol = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    survol = null;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    LocalDate j = jourA(e.getPoint());
                    if (j != null && ecouteur != null) {
                        ecouteur.jourChoisi(j);
                    }
                }
            };
            addMouseListener(souris);
            addMouseMotionListener(souris);
        }

        private int largeurCase() {
            return Math.max(CASE_L - 8, getWidth() / 7);
        }

        private LocalDate jourA(Point p) {
            int col = p.x / largeurCase();
            int ligne = p.y / CASE_H - 1;
            if (col < 0 || col > 6 || ligne < 0 || ligne > 5) {
                return null;
            }
            return premierJour().plusDays(ligne * 7L + col);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(7 * CASE_L, 7 * CASE_H);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int lc = largeurCase();
            g2.setFont(Theme.police(Font.PLAIN, 11));
            g2.setColor(Theme.TEXTE_SECONDAIRE);
            FontMetrics fm = g2.getFontMetrics();
            for (int c = 0; c < 7; c++) {
                g2.drawString(JOURS[c], c * lc + (lc - fm.stringWidth(JOURS[c])) / 2, 19);
            }
            LocalDate premier = premierJour();
            LocalDate jourSurvole = survol == null ? null : jourA(survol);
            for (int ligne = 0; ligne < 6; ligne++) {
                LocalDate debutLigne = premier.plusDays(ligne * 7L);
                int y = (ligne + 1) * CASE_H;
                if (debutLigne.equals(dimancheSelectionne)) {
                    g2.setColor(Theme.PRIMAIRE_CLAIR);
                    g2.fillRoundRect(2, y + 2, 7 * lc - 4, CASE_H - 4, CASE_H - 4, CASE_H - 4);
                }
                for (int c = 0; c < 7; c++) {
                    LocalDate j = debutLigne.plusDays(c);
                    int x = c * lc;
                    int d = Math.min(lc, CASE_H) - 4;
                    int cx = x + (lc - d) / 2;
                    int cy = y + (CASE_H - d) / 2;
                    Color texte = YearMonth.from(j).equals(mois) ? Theme.TEXTE : Theme.BORDURE.darker();
                    boolean gras = false;
                    if (j.equals(aujourdhui)) {
                        g2.setColor(Theme.PRIMAIRE);
                        g2.fillOval(cx, cy, d, d);
                        texte = Color.WHITE;
                        gras = true;
                    } else if (j.equals(jourSurvole)) {
                        g2.setColor(Theme.PRIMAIRE);
                        g2.drawOval(cx, cy, d - 1, d - 1);
                    }
                    g2.setFont(Theme.police(gras ? Font.BOLD : Font.PLAIN, 12));
                    fm = g2.getFontMetrics();
                    String s = String.valueOf(j.getDayOfMonth());
                    g2.setColor(texte);
                    g2.drawString(s, x + (lc - fm.stringWidth(s)) / 2, y + (CASE_H + fm.getAscent() - fm.getDescent()) / 2);
                }
            }
            g2.dispose();
        }
    }
}
