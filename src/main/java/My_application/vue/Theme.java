package My_application.vue;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;

/**
 * Charte graphique de GestiMed : palette « bleu-vert médical », thème FlatLaf
 * et harmonisation automatique des fenêtres.
 * <p>
 * Les fenêtres conçues avec l'éditeur de NetBeans fixent des polices
 * (Tahoma, Goudy Old Style…), des couleurs et des icônes PNG dans
 * {@code initComponents()}. Plutôt que de réécrire ce code généré (et les
 * fichiers {@code .form}), {@link #harmoniser(Component)} parcourt la fenêtre
 * une fois construite et remplace ces réglages par ceux de la charte. Les
 * réglages venant du thème ({@link UIResource}) ne sont pas touchés.
 */
public final class Theme {

    // ----- palette -----
    public static final Color PRIMAIRE = new Color(0x0E7C86);
    public static final Color PRIMAIRE_FONCE = new Color(0x0A5C63);
    public static final Color PRIMAIRE_CLAIR = new Color(0xE3F2F3);
    public static final Color FOND = new Color(0xF5F8FA);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDURE = new Color(0xDDE3E8);
    public static final Color TEXTE = new Color(0x1F2D3D);
    public static final Color TEXTE_SECONDAIRE = new Color(0x6B7C8F);
    public static final Color LIGNE_ALTERNEE = new Color(0xF7FAFB);
    public static final Color DANGER = new Color(0xC62828);
    public static final Color DANGER_CLAIR = new Color(0xFDECEC);
    public static final Color SUCCES = new Color(0x2E7D32);
    public static final Color SUCCES_CLAIR = new Color(0xE8F5E9);
    public static final Color AVERTISSEMENT = new Color(0xB26A00);
    public static final Color AVERTISSEMENT_CLAIR = new Color(0xFFF4E0);
    /** Patient arrivé, en salle d'attente. */
    public static final Color INFO = new Color(0x6A3FB5);
    public static final Color INFO_CLAIR = new Color(0xF1EBFA);
    /** Patient en consultation. */
    public static final Color ACCENT = new Color(0x1565C0);
    public static final Color ACCENT_CLAIR = new Color(0xE6EFFB);

    /** Taille de base du texte (points). */
    public static final int TAILLE_BASE = 13;
    /** Les titres plus grands sont ramenés à cette taille. */
    private static final int TAILLE_TITRE_MAX = 26;
    /** Taille maximale des libellés sur fond clair. */
    private static final int TAILLE_MAX_FOND_CLAIR = 16;

    private static final String DEJA_HARMONISE = "gestimed.harmonise";
    /** Marque les panneaux devenus des cartes (voir {@link BordureCarte}). */
    private static final String CARTE = "gestimed.carte";

    /** Libellés des boutons d'action principale (mis en avant). */
    private static final java.util.Set<String> ACTIONS_PRINCIPALES = new java.util.HashSet<>(
            java.util.Arrays.asList("Enregistrer", "Valider", "Ouvrir Dossier", "Confirmer", "Ajouter"));

    private static List<Image> iconesFenetre;

    private Theme() {
    }

    /** À appeler au démarrage, avant de créer la première fenêtre. */
    public static void installer() {
        FlatLaf.registerCustomDefaultsSource("My_application.theme");
        FlatLightLaf.setup();
        // Filet de sécurité : toute fenêtre ouverte sans avoir été harmonisée
        // dans son constructeur l'est à son ouverture.
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e.getID() == WindowEvent.WINDOW_OPENED && e.getSource() instanceof Window) {
                harmoniser((Window) e.getSource());
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    // ----- polices -----

    /** Police de l'application avec le style et la taille demandés. */
    public static Font police(int style, float taille) {
        Font base = UIManager.getFont("defaultFont");
        if (base == null) {
            base = new Font(Font.SANS_SERIF, Font.PLAIN, TAILLE_BASE);
        }
        return base.deriveFont(style, taille);
    }

    public static Font police() {
        return police(Font.PLAIN, TAILLE_BASE);
    }

    public static Font policeGras() {
        return police(Font.BOLD, TAILLE_BASE);
    }

    // ----- cellules de tableaux et de listes -----

    /**
     * Style commun d'une cellule : police de l'application, sélection du
     * thème, lignes alternées.
     */
    public static void cellule(JComponent cellule, JTable table, boolean selectionne, int ligne) {
        cellule.setFont(police());
        if (selectionne) {
            cellule.setBackground(table.getSelectionBackground());
            cellule.setForeground(table.getSelectionForeground());
        } else {
            cellule.setBackground(ligne % 2 == 0 ? SURFACE : LIGNE_ALTERNEE);
            cellule.setForeground(TEXTE);
        }
    }

    // ----- harmonisation des fenêtres -----

    /**
     * Applique la charte à un composant et à tout son contenu. Sans effet si
     * c'est déjà fait. Une fenêtre est agrandie si son contenu ne tient plus.
     */
    public static void harmoniser(Component racine) {
        JComponent marqueur = marqueur(racine);
        if (marqueur != null) {
            if (Boolean.TRUE.equals(marqueur.getClientProperty(DEJA_HARMONISE))) {
                return;
            }
            marqueur.putClientProperty(DEJA_HARMONISE, Boolean.TRUE);
        }
        parcourir(racine, false);
        if (racine instanceof Window) {
            Window w = (Window) racine;
            w.setIconImages(iconesFenetre());
            ajusterTaille(w);
        }
        racine.revalidate();
        racine.repaint();
    }

    private static JComponent marqueur(Component c) {
        if (c instanceof JComponent) {
            return (JComponent) c;
        }
        if (c instanceof javax.swing.RootPaneContainer) {
            return ((javax.swing.RootPaneContainer) c).getRootPane();
        }
        return null;
    }

    /**
     * @param dansCarte vrai à l'intérieur d'une carte : les panneaux y
     *        prennent le fond blanc de la carte au lieu du fond de la fenêtre.
     */
    private static void parcourir(Component c, boolean dansCarte) {
        appliquer(c);
        boolean carte = estCarte(c);
        if (dansCarte && !carte) {
            surface(c);
        }
        boolean enfantsDansCarte = dansCarte || carte;
        if (c instanceof JMenu) {
            for (Component e : ((JMenu) c).getMenuComponents()) {
                parcourir(e, false);
            }
        }
        if (c instanceof Container) {
            for (Component e : ((Container) c).getComponents()) {
                parcourir(e, enfantsDansCarte);
            }
        }
    }

    static boolean estCarte(Component c) {
        return c instanceof JComponent && Boolean.TRUE.equals(((JComponent) c).getClientProperty(CARTE));
    }

    /**
     * Fait d'un panneau une carte : fond blanc à coins arrondis, contour
     * discret et titre en gras (l'aspect des panneaux de l'agenda).
     */
    public static void carte(JComponent panneau, String titre) {
        panneau.setBorder(new BordureCarte(titre));
        panneau.setBackground(SURFACE);
        if (panneau instanceof JPanel) {
            panneau.putClientProperty("FlatLaf.style", "arc: " + BordureCarte.ARC);
        }
        panneau.putClientProperty(CARTE, Boolean.TRUE);
    }

    /** Dans une carte, les conteneurs au fond de fenêtre prennent le blanc de la carte. */
    private static void surface(Component c) {
        boolean conteneur = c instanceof JPanel || c instanceof JSplitPane || c instanceof JToolBar
                || c instanceof JCheckBox || c instanceof JRadioButton;
        if (conteneur && (c.getBackground() instanceof UIResource || FOND.equals(c.getBackground()))) {
            c.setBackground(SURFACE);
        }
    }

    private static void appliquer(Component c) {
        if (c.isBackgroundSet() && !(c.getBackground() instanceof UIResource)) {
            c.setBackground(fond(c.getBackground(), estZoneDeContenu(c)));
        }
        boolean donnees = c instanceof JTable || c instanceof JList || c instanceof JTree;
        if (c.isForegroundSet() && !(c.getForeground() instanceof UIResource)) {
            c.setForeground(donnees ? TEXTE : texte(c.getForeground(), surFondSombre(c)));
        }
        if (c.isFontSet() && !(c.getFont() instanceof UIResource)) {
            // Données (tableaux, listes, arbres) : texte courant, sans emphase.
            c.setFont(donnees ? police() : police(c.getFont(), surFondSombre(c)));
        }
        if (c instanceof JComponent) {
            JComponent j = (JComponent) c;
            Border b = j.getBorder();
            if (b instanceof TitledBorder && (j instanceof JPanel || j instanceof JScrollPane)) {
                // Groupe titré des formulaires → carte.
                carte(j, ((TitledBorder) b).getTitle());
            } else if (b != null && !(b instanceof UIResource)) {
                j.setBorder(bordure(b));
            }
        }
        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            l.setIcon(icone(l.getIcon(), surFondSombre(l) ? Color.WHITE : PRIMAIRE));
            l.setDisabledIcon(null);
        } else if (c instanceof AbstractButton) {
            boutons((AbstractButton) c);
        } else if (c instanceof JTabbedPane) {
            JTabbedPane t = (JTabbedPane) c;
            for (int i = 0; i < t.getTabCount(); i++) {
                t.setIconAt(i, icone(t.getIconAt(i), PRIMAIRE));
            }
        } else if (c instanceof JTable) {
            JTable t = (JTable) c;
            t.setRowHeight(Math.max(t.getRowHeight(), 32));
            t.setShowVerticalLines(false);
            t.setIntercellSpacing(new Dimension(0, 1));
            t.setGridColor(UIManager.getColor("Table.gridColor"));
        }
    }

    private static void boutons(AbstractButton b) {
        Color couleur = PRIMAIRE;
        b.setIcon(icone(b.getIcon(), couleur));
        b.setPressedIcon(icone(b.getPressedIcon(), couleur));
        b.setRolloverIcon(icone(b.getRolloverIcon(), couleur));
        b.setSelectedIcon(icone(b.getSelectedIcon(), couleur));
        // L'icône désactivée est recalculée par le thème (grisée).
        b.setDisabledIcon(null);
        b.setDisabledSelectedIcon(null);
        String texte = b.getText();
        if (b instanceof JButton && texte != null && ACTIONS_PRINCIPALES.contains(texte.trim())) {
            // Action principale de la fenêtre : bouton plein, couleur de la charte.
            b.putClientProperty("FlatLaf.style", "background: #0E7C86; foreground: #FFFFFF;"
                    + " borderColor: #0E7C86; hoverBackground: #0B6C75; pressedBackground: #0A5C63;"
                    + " font: bold");
        }
        if (b instanceof JButton && b.getIcon() != null && (texte == null || texte.trim().isEmpty())) {
            // Bouton icône seule : plat, surligné au survol.
            b.putClientProperty("JButton.buttonType", "toolBarButton");
            b.setFocusable(false);
        }
    }

    /** Une icône PNG connue devient son équivalent SVG ; les autres sont conservées. */
    private static Icon icone(Icon icone, Color couleur) {
        if (!(icone instanceof ImageIcon)) {
            return icone;
        }
        String description = ((ImageIcon) icone).getDescription();
        Icon svg = Icones.remplacement(description, icone.getIconWidth(), couleur);
        return svg != null ? svg : icone;
    }

    // ----- correspondances de couleurs -----

    private static boolean estZoneDeContenu(Component c) {
        return c instanceof JTable || c instanceof JList || c instanceof JTree
                || c instanceof JTextArea || c instanceof JTextComponent
                || c instanceof javax.swing.JViewport;
    }

    /** Couleur de fond d'origine → couleur de la charte. */
    static Color fond(Color c, boolean zoneDeContenu) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        double l = luminance(c);
        if (l < 0.2) {
            return PRIMAIRE;                       // bandeaux sombres, afficheurs noirs
        }
        if (hsb[1] > 0.5 && l < 0.55) {
            return PRIMAIRE;                       // aplats vifs (barre verte, etc.)
        }
        if (l > 0.95 && hsb[1] < 0.06) {
            return SURFACE;                        // blanc
        }
        return zoneDeContenu ? SURFACE : FOND;     // gris, pastels roses / verts / jaunes
    }

    /** Couleur de texte d'origine → couleur de la charte. */
    static Color texte(Color c, boolean surFondSombre) {
        if (surFondSombre) {
            return Color.WHITE;
        }
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        double l = luminance(c);
        if (hsb[1] > 0.45) {
            boolean rouge = hsb[0] < 0.05f || hsb[0] > 0.95f;
            if (rouge && hsb[2] > 0.9f) {
                return DANGER;                     // rouge vif : alerte
            }
            return PRIMAIRE_FONCE;                 // bordeaux, bleus : mise en valeur
        }
        if (l > 0.8) {
            return TEXTE;                          // blanc sur fond désormais clair
        }
        if (l > 0.12) {
            return TEXTE_SECONDAIRE;               // gris
        }
        return TEXTE;
    }

    /**
     * Vrai si le fond peint sous le composant est sombre. Les champs et
     * boutons FlatLaf ne sont pas opaques mais peignent eux-mêmes leur fond.
     */
    private static boolean surFondSombre(Component c) {
        if (c.isBackgroundSet() && (c instanceof JTextComponent || c instanceof AbstractButton)) {
            return luminance(c.getBackground()) < 0.45;
        }
        for (Component p = c; p != null; p = p.getParent()) {
            if (p.isOpaque() || p instanceof Window) {
                return luminance(p.getBackground()) < 0.45;
            }
        }
        return false;
    }

    private static double luminance(Color c) {
        if (c == null) {
            return 1;
        }
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
    }

    // ----- polices et bordures -----

    private static Font police(Font f, boolean surFondSombre) {
        // Les grands titres sont réservés aux bandeaux (fond sombre).
        int max = surFondSombre ? TAILLE_TITRE_MAX : TAILLE_MAX_FOND_CLAIR;
        int taille = f.getSize() < TAILLE_BASE ? TAILLE_BASE : Math.min(f.getSize(), max);
        // L'italique décoratif (Goudy Old Style) nuit à la lisibilité.
        return police(f.getStyle() & Font.BOLD, taille);
    }

    private static Border bordure(Border b) {
        if (b instanceof TitledBorder) {
            return new BordureCarte(((TitledBorder) b).getTitle());
        }
        if (b instanceof CompoundBorder) {
            CompoundBorder cb = (CompoundBorder) b;
            return new CompoundBorder(
                    cb.getOutsideBorder() == null ? null : bordure(cb.getOutsideBorder()),
                    cb.getInsideBorder() == null ? null : bordure(cb.getInsideBorder()));
        }
        if (b instanceof BevelBorder || b instanceof EtchedBorder || b instanceof LineBorder) {
            return new FlatLineBorder(new Insets(2, 2, 2, 2), BORDURE, 1f, 8);
        }
        if (b instanceof MatteBorder && ((MatteBorder) b).getTileIcon() == null) {
            MatteBorder m = (MatteBorder) b;
            Insets i = m.getBorderInsets();
            return new MatteBorder(i.top, i.left, i.bottom, i.right, BORDURE);
        }
        return b;
    }

    // ----- fenêtres -----

    private static void ajusterTaille(Window w) {
        if (w instanceof Frame && (((Frame) w).getExtendedState() & Frame.MAXIMIZED_BOTH) != 0) {
            return;
        }
        Dimension voulue = w.getPreferredSize();
        Dimension actuelle = w.getSize();
        if (actuelle.width == 0 || actuelle.height == 0) {
            return;                                // pas encore dimensionnée
        }
        if (voulue.width > actuelle.width || voulue.height > actuelle.height) {
            w.setSize(Math.max(voulue.width, actuelle.width), Math.max(voulue.height, actuelle.height));
        }
    }

    /** Icône de l'application : stéthoscope blanc sur carré arrondi bleu-vert. */
    public static synchronized List<Image> iconesFenetre() {
        if (iconesFenetre == null) {
            iconesFenetre = new ArrayList<>();
            for (int taille : new int[]{16, 24, 32, 48, 64, 128}) {
                BufferedImage img = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PRIMAIRE);
                int arc = Math.max(4, taille / 4);
                g.fillRoundRect(0, 0, taille, taille, arc, arc);
                int marge = Math.max(2, taille / 6);
                Icon i = Icones.svg("stethoscope", taille - 2 * marge, Color.WHITE);
                i.paintIcon(null, g, marge, marge);
                g.dispose();
                iconesFenetre.add(img);
            }
        }
        return iconesFenetre;
    }
}
