package My_application.dossier;

import My_application.vue.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;

/**
 * Courbe d'évolution d'une constante (ou de deux, pour la tension) d'une
 * consultation à l'autre, avec un seuil facultatif en pointillés. Les points
 * sont espacés régulièrement : une consultation par point.
 */
public class CourbeConstantes extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MM/yy", Locale.FRENCH);
    private static final int MARGE_GAUCHE = 40;
    private static final int MARGE_BAS = 22;
    private static final int MARGE = 12;

    /** Une série de valeurs (NaN : non mesurée ce jour-là). */
    private static final class Serie {
        final String nom;
        final Color couleur;
        final double[] valeurs;

        Serie(String nom, Color couleur, double[] valeurs) {
            this.nom = nom;
            this.couleur = couleur;
            this.valeurs = valeurs;
        }
    }

    private final transient List<LocalDate> jours = new ArrayList<>();
    private final transient List<Serie> series = new ArrayList<>();
    private Double seuil;

    public CourbeConstantes() {
        setPreferredSize(new Dimension(420, 170));
        setFont(Theme.police(java.awt.Font.PLAIN, Theme.TAILLE_BASE - 2));
    }

    /** Remet la courbe à zéro avec les jours (axe horizontal), dans l'ordre chronologique. */
    public void setJours(List<LocalDate> jours) {
        this.jours.clear();
        this.jours.addAll(jours);
        series.clear();
        seuil = null;
        repaint();
    }

    public void ajouterSerie(String nom, Color couleur, double[] valeurs) {
        series.add(new Serie(nom, couleur, valeurs));
        repaint();
    }

    public void setSeuil(Double seuil) {
        this.seuil = seuil;
        repaint();
    }

    /** Vrai s'il y a au moins une valeur à tracer. */
    public boolean aDesValeurs() {
        for (Serie s : series) {
            for (double v : s.valeurs) {
                if (!Double.isNaN(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int largeur = getWidth() - MARGE_GAUCHE - MARGE;
        int hauteur = getHeight() - MARGE_BAS - MARGE;
        if (!aDesValeurs() || largeur <= 0 || hauteur <= 0) {
            g2.setColor(Theme.TEXTE_SECONDAIRE);
            String vide = "Aucune mesure enregistrée";
            g2.drawString(vide, (getWidth() - fm.stringWidth(vide)) / 2, getHeight() / 2);
            g2.dispose();
            return;
        }
        double[] bornes = bornes();
        double min = bornes[0];
        double max = bornes[1];

        // Grille : 4 graduations.
        for (int i = 0; i <= 3; i++) {
            double v = min + (max - min) * i / 3;
            int y = y(v, min, max, hauteur);
            g2.setColor(Theme.BORDURE);
            g2.drawLine(MARGE_GAUCHE, y, MARGE_GAUCHE + largeur, y);
            g2.setColor(Theme.TEXTE_SECONDAIRE);
            String t = My_application.service.SyntheseDossier.nombre(Math.round(v * 10) / 10.0);
            g2.drawString(t, MARGE_GAUCHE - 6 - fm.stringWidth(t), y + fm.getAscent() / 2 - 1);
        }
        if (seuil != null && seuil > min && seuil < max) {
            int y = y(seuil, min, max, hauteur);
            g2.setColor(Theme.AVERTISSEMENT);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
            g2.drawLine(MARGE_GAUCHE, y, MARGE_GAUCHE + largeur, y);
        }

        // Dates : au plus une étiquette tous les 50 pixels.
        int n = jours.size();
        int pas = Math.max(1, (int) Math.ceil(n * 50.0 / Math.max(1, largeur)));
        g2.setColor(Theme.TEXTE_SECONDAIRE);
        for (int i = 0; i < n; i += pas) {
            String t = jours.get(i).format(MOIS);
            g2.drawString(t, x(i, n, largeur) - fm.stringWidth(t) / 2, getHeight() - 6);
        }

        for (Serie s : series) {
            g2.setColor(s.couleur);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D trace = new Path2D.Double();
            boolean commence = false;
            for (int i = 0; i < s.valeurs.length && i < n; i++) {
                if (Double.isNaN(s.valeurs[i])) {
                    continue;
                }
                int x = x(i, n, largeur);
                int y = y(s.valeurs[i], min, max, hauteur);
                if (commence) {
                    trace.lineTo(x, y);
                } else {
                    trace.moveTo(x, y);
                    commence = true;
                }
            }
            g2.draw(trace);
            for (int i = 0; i < s.valeurs.length && i < n; i++) {
                if (!Double.isNaN(s.valeurs[i])) {
                    g2.fillOval(x(i, n, largeur) - 4, y(s.valeurs[i], min, max, hauteur) - 4, 8, 8);
                }
            }
        }
        g2.dispose();
    }

    /** Bornes de l'axe vertical, un peu élargies pour ne pas coller les points aux bords. */
    private double[] bornes() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (Serie s : series) {
            for (double v : s.valeurs) {
                if (!Double.isNaN(v)) {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
        }
        if (seuil != null) {
            min = Math.min(min, seuil);
            max = Math.max(max, seuil);
        }
        double marge = Math.max(1, (max - min) * 0.15);
        return new double[]{Math.floor(min - marge), Math.ceil(max + marge)};
    }

    private int x(int i, int n, int largeur) {
        return n <= 1 ? MARGE_GAUCHE + largeur / 2 : MARGE_GAUCHE + 10 + (largeur - 20) * i / (n - 1);
    }

    private int y(double v, double min, double max, int hauteur) {
        return MARGE + (int) Math.round(hauteur * (max - v) / (max - min));
    }

    /** Noms et couleurs des séries, pour la légende. */
    public List<String> getNomsSeries() {
        List<String> l = new ArrayList<>();
        for (Serie s : series) {
            l.add(s.nom);
        }
        return l;
    }

    public Color getCouleurSerie(int i) {
        return series.get(i).couleur;
    }
}
