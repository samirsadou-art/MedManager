package My_application.admin;

import My_application.service.StatistiquesService.Barre;
import My_application.vue.Theme;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;

/**
 * Histogramme vertical d'une seule série (consultations par jour, semaine ou
 * mois), peint à la main : barres fines à bout arrondi posées sur la ligne de
 * base, grille discrète, libellés éclaircis quand ils se chevaucheraient, et
 * info-bulle sur la barre survolée.
 */
public class GraphiqueBarres extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final int MARGE_GAUCHE = 44;
    private static final int MARGE_BAS = 26;
    private static final int MARGE_HAUT = 12;
    private static final int ARRONDI = 4;

    private List<Barre> barres = Collections.emptyList();
    private String unite = "consultations";
    private int survolee = -1;

    public GraphiqueBarres() {
        setOpaque(false);
        setPreferredSize(new Dimension(600, 240));
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter souris = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = indexA(e.getX());
                if (i != survolee) {
                    survolee = i;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                survolee = -1;
                repaint();
            }
        };
        addMouseListener(souris);
        addMouseMotionListener(souris);
    }

    public void setBarres(List<Barre> barres, String unite) {
        this.barres = new ArrayList<>(barres);
        this.unite = unite;
        survolee = -1;
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = indexA(e.getX());
        if (i < 0) {
            return null;
        }
        Barre b = barres.get(i);
        return "<html><b>" + b.getDetail() + "</b><br>" + b.getValeur() + " " + unite + "</html>";
    }

    /** Barre sous l'abscisse x (toute la hauteur de la colonne compte), ou -1. */
    int indexA(int x) {
        if (barres.isEmpty()) {
            return -1;
        }
        double pas = pas();
        int i = (int) Math.floor((x - MARGE_GAUCHE) / pas);
        return i >= 0 && i < barres.size() ? i : -1;
    }

    private double pas() {
        return (double) (getWidth() - MARGE_GAUCHE - 8) / Math.max(1, barres.size());
    }

    /** Graduation « ronde » au-dessus du maximum (1, 2, 5 × 10ⁿ), quatre intervalles au plus. */
    static int graduation(int max) {
        if (max <= 4) {
            return 1;
        }
        double brut = max / 4.0;
        double puissance = Math.pow(10, Math.floor(Math.log10(brut)));
        for (int f : new int[] {1, 2, 5, 10}) {
            if (f * puissance >= brut) {
                return (int) Math.max(1, f * puissance);
            }
        }
        return (int) (10 * puissance);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(Theme.police(Font.PLAIN, 11f));
        FontMetrics fm = g.getFontMetrics();
        int bas = getHeight() - MARGE_BAS;
        int hauteur = bas - MARGE_HAUT;

        int max = 0;
        for (Barre b : barres) {
            max = Math.max(max, b.getValeur());
        }
        if (max == 0) {
            g.setColor(Theme.TEXTE_SECONDAIRE);
            String vide = "Aucune consultation sur cette période";
            g.drawString(vide, (getWidth() - fm.stringWidth(vide)) / 2, getHeight() / 2);
            g.dispose();
            return;
        }
        int pasGraduation = graduation(max);
        int haut = ((max + pasGraduation - 1) / pasGraduation) * pasGraduation;

        // Grille horizontale discrète et valeurs à gauche.
        g.setStroke(new BasicStroke(1f));
        for (int v = 0; v <= haut; v += pasGraduation) {
            int y = bas - (int) Math.round((double) v / haut * hauteur);
            g.setColor(v == 0 ? Theme.BORDURE.darker() : Theme.BORDURE);
            g.drawLine(MARGE_GAUCHE, y, getWidth() - 8, y);
            g.setColor(Theme.TEXTE_SECONDAIRE);
            String t = String.valueOf(v);
            g.drawString(t, MARGE_GAUCHE - 8 - fm.stringWidth(t), y + fm.getAscent() / 2 - 1);
        }

        double pas = pas();
        double largeur = Math.max(2, Math.min(36, pas * 0.62));
        // Un libellé sur n pour qu'ils ne se chevauchent pas.
        int plusLarge = 0;
        for (Barre b : barres) {
            plusLarge = Math.max(plusLarge, fm.stringWidth(b.getLibelle()));
        }
        int un = Math.max(1, (int) Math.ceil((plusLarge + 10) / pas));

        for (int i = 0; i < barres.size(); i++) {
            Barre b = barres.get(i);
            double centre = MARGE_GAUCHE + pas * i + pas / 2;
            if (b.getValeur() > 0) {
                double h = Math.max(2, (double) b.getValeur() / haut * hauteur);
                g.setColor(i == survolee ? Theme.PRIMAIRE_FONCE : Theme.PRIMAIRE);
                g.fill(barreArrondie(centre - largeur / 2, bas - h, largeur, h));
            }
            if (i % un == 0) {
                g.setColor(Theme.TEXTE_SECONDAIRE);
                g.drawString(b.getLibelle(), (int) (centre - fm.stringWidth(b.getLibelle()) / 2.0),
                        bas + fm.getAscent() + 6);
            }
        }
        if (survolee >= 0) {
            // Valeur de la barre survolée, au-dessus d'elle (étiquette sélective).
            Barre b = barres.get(survolee);
            String t = String.valueOf(b.getValeur());
            double centre = MARGE_GAUCHE + pas * survolee + pas / 2;
            int y = bas - (int) Math.round((double) b.getValeur() / haut * hauteur) - 5;
            g.setFont(Theme.police(Font.BOLD, 11f));
            g.setColor(Theme.TEXTE);
            g.drawString(t, (int) (centre - g.getFontMetrics().stringWidth(t) / 2.0), Math.max(fm.getAscent(), y));
        }
        g.dispose();
    }

    /** Rectangle arrondi en haut seulement : la barre reste posée sur la ligne de base. */
    private static Path2D barreArrondie(double x, double y, double l, double h) {
        double r = Math.min(ARRONDI, Math.min(l / 2, h));
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + h);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.lineTo(x + l - r, y);
        p.quadTo(x + l, y, x + l, y + r);
        p.lineTo(x + l, y + h);
        p.closePath();
        return p;
    }
}
