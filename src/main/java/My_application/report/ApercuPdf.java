package My_application.report;

import My_application.vue.Dialogues;
import My_application.vue.EnTetePage;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Aperçu d'un document PDF dans une fenêtre de l'application (ordonnance,
 * analyses, liste du jour, statistiques), avec « Imprimer », « Enregistrer
 * sous… », le zoom et l'ouverture dans la visionneuse du système en secours.
 * <p>
 * Les pages sont dessinées par PDFBox à la demande, au zoom courant.
 * L'impression passe par la boîte d'impression de Java : aucune visionneuse
 * PDF n'est nécessaire sur le poste.
 */
public final class ApercuPdf extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger("GestiMed");

    /** Points PDF (1/72 de pouce) → pixels d'écran à 100 % (96 ppp). */
    private static final float POINTS_EN_PIXELS = 96f / 72f;
    static final float ZOOM_MIN = 0.5f;
    static final float ZOOM_MAX = 3f;
    private static final float PAS_ZOOM = 1.25f;
    private static final int MARGE = 24;
    private static final int LARGEUR_COTE = 110;

    private final File fichier;
    private final transient PDDocument document;
    private final transient PDFRenderer rendu;
    private final List<Page> pages = new ArrayList<>();
    private final JPanel feuilles = new JPanel();
    private final JScrollPane defilement;
    private final JLabel libelleZoom = new JLabel("", JLabel.CENTER);
    private final JLabel libellePage = new JLabel();
    private final JButton boutonImprimer;
    private float zoom = 1f;

    private ApercuPdf(Window parent, File fichier, PDDocument document) {
        super(parent, fichier.getName(), Dialog.ModalityType.MODELESS);
        this.fichier = fichier;
        this.document = document;
        this.rendu = new PDFRenderer(document);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImages(Theme.iconesFenetre());

        feuilles.setLayout(new BoxLayout(feuilles, BoxLayout.Y_AXIS));
        feuilles.setBackground(Theme.FOND);
        feuilles.setBorder(BorderFactory.createEmptyBorder(MARGE, MARGE, MARGE, MARGE));
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            if (i > 0) {
                feuilles.add(Box.createVerticalStrut(MARGE));
            }
            Page p = new Page(i);
            pages.add(p);
            feuilles.add(p);
        }
        defilement = new JScrollPane(feuilles);
        defilement.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDURE));
        defilement.getViewport().setBackground(Theme.FOND);
        defilement.getVerticalScrollBar().setUnitIncrement(24);
        defilement.getViewport().addChangeListener(e -> majPageCourante());
        // Ctrl + molette : zoom, comme dans les visionneuses ; sans Ctrl, défilement normal.
        feuilles.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                zoomer(e.getWheelRotation() < 0 ? zoom * PAS_ZOOM : zoom / PAS_ZOOM);
            } else {
                defilement.dispatchEvent(SwingUtilities.convertMouseEvent(feuilles, e, defilement));
            }
        });

        JPanel contenu = new JPanel(new BorderLayout());
        contenu.setBackground(Theme.FOND);
        contenu.add(defilement, BorderLayout.CENTER);
        contenu.add(barreZoom(), BorderLayout.SOUTH);
        setContentPane(contenu);

        Theme.harmoniser(this);
        // Les feuilles blanches se détachent sur le fond gris de la fenêtre.
        feuilles.setBackground(Theme.FOND);
        defilement.getViewport().setBackground(Theme.FOND);
        int n = document.getNumberOfPages();
        EnTetePage entete = EnTetePage.installer(this, "file-text", "Aperçu du document",
                fichier.getName() + "  ·  " + n + (n > 1 ? " pages" : " page"));
        entete.ajouterBouton("Visionneuse", "external-link", this::ouvrirDansLaVisionneuse)
                .setToolTipText("Ouvrir dans la visionneuse PDF du poste");
        entete.ajouterBouton("Enregistrer sous…", "device-floppy", this::enregistrerSous);
        boutonImprimer = new JButton("Imprimer", Icones.svg("printer", 18, Color.WHITE));
        boutonImprimer.setBackground(Theme.PRIMAIRE);
        boutonImprimer.setForeground(Color.WHITE);
        boutonImprimer.setFont(Theme.policeGras());
        boutonImprimer.setToolTipText("Imprimer (Ctrl+P)");
        boutonImprimer.addActionListener(e -> imprimer());
        entete.ajouterAction(boutonImprimer);

        raccourci("ctrl P", "imprimer", this::imprimer);
        raccourci("ctrl S", "enregistrer", this::enregistrerSous);
        raccourci("ctrl ADD", "zoomPlus", () -> zoomer(zoom * PAS_ZOOM));
        raccourci("ctrl EQUALS", "zoomPlus2", () -> zoomer(zoom * PAS_ZOOM));
        raccourci("ctrl SUBTRACT", "zoomMoins", () -> zoomer(zoom / PAS_ZOOM));
        raccourci("ctrl MINUS", "zoomMoins2", () -> zoomer(zoom / PAS_ZOOM));
        raccourci("ctrl NUMPAD0", "zoom100", () -> zoomer(1f));
        raccourci("ctrl 0", "zoom1002", () -> zoomer(1f));
        raccourci("ESCAPE", "fermer", this::dispose);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                fermerDocument();
            }
        });

        dimensionner();
        zoomer(1f);
        setLocationRelativeTo(parent);
    }

    /**
     * Ouvre l'aperçu du fichier, non modal, au-dessus de la fenêtre active.
     *
     * @throws IOException si PDFBox ne peut pas lire le fichier
     */
    public static ApercuPdf afficher(Window parent, File pdf) throws IOException {
        PDDocument doc = Loader.loadPDF(pdf);
        try {
            ApercuPdf apercu = new ApercuPdf(parent, pdf, doc);
            apercu.setVisible(true);
            // Écran étroit : la page entière tient en largeur.
            apercu.zoomer(Math.min(1f, apercu.zoomAjuste(apercu.defilement.getViewport().getExtentSize().width)));
            return apercu;
        } catch (RuntimeException e) {
            doc.close();
            throw e;
        }
    }

    // ----- actions -----

    /**
     * Boîte d'impression du système, puis impression en arrière-plan (sur une
     * copie du document : PDFBox ne se partage pas entre deux fils d'exécution).
     */
    public void imprimer() {
        if (!boutonImprimer.isEnabled()) {
            return;
        }
        PrinterJob travail = PrinterJob.getPrinterJob();
        travail.setJobName(fichier.getName());
        if (!travail.printDialog()) {
            return;
        }
        boutonImprimer.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException, PrinterException {
                try (PDDocument copie = Loader.loadPDF(fichier)) {
                    travail.setPageable(new PDFPageable(copie));
                    travail.print();
                }
                return null;
            }

            @Override
            protected void done() {
                boutonImprimer.setEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    LOG.log(Level.WARNING, "Impression impossible : " + fichier, e.getCause());
                    Dialogues.erreur(ApercuPdf.this, "L'impression a échoué. Vérifiez que l'imprimante est "
                            + "allumée et connectée, puis réessayez.");
                }
            }
        }.execute();
    }

    private void enregistrerSous() {
        JFileChooser choix = new JFileChooser();
        choix.setDialogTitle("Enregistrer une copie du document");
        choix.setFileFilter(new FileNameExtensionFilter("Documents PDF", "pdf"));
        choix.setSelectedFile(new File(fichier.getName()));
        if (choix.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File cible = choix.getSelectedFile();
        if (!cible.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            cible = new File(cible.getParentFile(), cible.getName() + ".pdf");
        }
        if (cible.exists() && !Dialogues.confirmer(this, "Le fichier " + cible.getName()
                + " existe déjà. Le remplacer ?")) {
            return;
        }
        try {
            Files.copy(fichier.toPath(), cible.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Copie du document impossible vers " + cible, e);
            Dialogues.erreur(this, "Impossible d'enregistrer le document dans :\n" + cible.getAbsolutePath());
        }
    }

    private void ouvrirDansLaVisionneuse() {
        try {
            new AfficheurSysteme().ouvrir(fichier);
        } catch (IOException | UnsupportedOperationException e) {
            LOG.log(Level.WARNING, "Visionneuse du système indisponible pour " + fichier, e);
            Dialogues.erreur(this, "Aucune visionneuse PDF n'est installée sur ce poste.\nLe document est "
                    + "enregistré dans :\n" + fichier.getAbsolutePath());
        }
    }

    // ----- zoom et pages -----

    private JComponent barreZoom() {
        JPanel barre = new JPanel(new BorderLayout());
        barre.setBackground(Theme.SURFACE);
        barre.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        libellePage.setForeground(Theme.TEXTE_SECONDAIRE);
        libellePage.setPreferredSize(new Dimension(LARGEUR_COTE, 24));
        barre.add(libellePage, BorderLayout.WEST);

        JPanel zoomPanneau = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        zoomPanneau.setOpaque(false);
        zoomPanneau.add(boutonIcone("zoom-out", "Réduire (Ctrl -)", () -> zoomer(zoom / PAS_ZOOM)));
        libelleZoom.setPreferredSize(new Dimension(56, 24));
        libelleZoom.setForeground(Theme.TEXTE);
        zoomPanneau.add(libelleZoom);
        zoomPanneau.add(boutonIcone("zoom-in", "Agrandir (Ctrl +)", () -> zoomer(zoom * PAS_ZOOM)));
        JButton largeur = new JButton("Largeur", Icones.svg("arrows-horizontal", 16, Theme.PRIMAIRE));
        largeur.setToolTipText("Ajuster à la largeur de la fenêtre");
        largeur.addActionListener(e -> zoomer(zoomAjuste(defilement.getViewport().getExtentSize().width)));
        largeur.setFocusable(false);
        zoomPanneau.add(largeur);
        JButton reel = new JButton("100 %");
        reel.setToolTipText("Taille réelle (Ctrl 0)");
        reel.addActionListener(e -> zoomer(1f));
        reel.setFocusable(false);
        zoomPanneau.add(reel);
        barre.add(zoomPanneau, BorderLayout.CENTER);
        // Équilibre la colonne de gauche pour garder le zoom centré.
        barre.add(Box.createHorizontalStrut(LARGEUR_COTE), BorderLayout.EAST);
        return barre;
    }

    private static JButton boutonIcone(String icone, String aide, Runnable action) {
        JButton b = new JButton(Icones.svg(icone, 18, Theme.PRIMAIRE));
        b.setToolTipText(aide);
        b.setFocusable(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void zoomer(float demande) {
        float nouveau = borner(demande);
        // Garde le même endroit du document au centre de l'écran.
        Rectangle vue = defilement.getViewport().getViewRect();
        int hauteur = Math.max(1, feuilles.getPreferredSize().height);
        double centre = (vue.y + vue.height / 2.0) / hauteur;
        zoom = nouveau;
        libelleZoom.setText(Math.round(zoom * 100) + " %");
        for (Page p : pages) {
            p.revalidate();
        }
        feuilles.revalidate();
        feuilles.repaint();
        SwingUtilities.invokeLater(() -> {
            int h = feuilles.getPreferredSize().height;
            int y = (int) Math.round(centre * h - vue.height / 2.0);
            defilement.getViewport().setViewPosition(new java.awt.Point(
                    defilement.getViewport().getViewPosition().x, Math.max(0, Math.min(y, h - vue.height))));
            majPageCourante();
        });
    }

    static float borner(float zoom) {
        return Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom));
    }

    /** Zoom pour que la page la plus large tienne dans la largeur visible. */
    private float zoomAjuste(int largeurVisible) {
        float largeurPoints = 0;
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            largeurPoints = Math.max(largeurPoints, document.getPage(i).getMediaBox().getWidth());
        }
        return zoomPourLargeur(largeurVisible - 2 * MARGE - defilement.getVerticalScrollBar().getPreferredSize().width,
                largeurPoints);
    }

    static float zoomPourLargeur(int largeurPixels, float largeurPoints) {
        if (largeurPixels <= 0 || largeurPoints <= 0) {
            return 1f;
        }
        return borner(largeurPixels / (largeurPoints * POINTS_EN_PIXELS));
    }

    private void majPageCourante() {
        Rectangle vue = defilement.getViewport().getViewRect();
        int milieu = vue.y + vue.height / 2;
        int courante = 1;
        for (Page p : pages) {
            if (p.getY() <= milieu) {
                courante = p.indice + 1;
            }
        }
        libellePage.setText("Page " + courante + " / " + pages.size());
    }

    private void dimensionner() {
        Rectangle ecran = getGraphicsConfiguration() == null
                ? new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())
                : getGraphicsConfiguration().getBounds();
        int largeur = Math.min(Math.round(595 * POINTS_EN_PIXELS) + 2 * MARGE + 40, ecran.width - 80);
        int hauteur = Math.max(500, ecran.height - 120);
        setSize(Math.max(640, largeur), hauteur);
    }

    private void raccourci(String touche, String nom, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(touche), nom);
        getRootPane().getActionMap().put(nom, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void fermerDocument() {
        try {
            document.close();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Fermeture de l'aperçu", e);
        }
    }

    /**
     * Rendu d'une page à l'échelle donnée (1 = 72 ppp), sur fond blanc.
     * Séparé de l'affichage pour être testé sans écran.
     */
    static BufferedImage rendre(PDFRenderer rendu, int page, float echelle) throws IOException {
        return rendu.renderImage(page, echelle, ImageType.RGB);
    }

    /** Une feuille du document : ombre légère, image rendue au zoom courant. */
    private final class Page extends JComponent {

        private static final long serialVersionUID = 1L;
        private static final int OMBRE = 3;
        private final int indice;
        private transient BufferedImage image;
        private float echelleImage;

        Page(int indice) {
            this.indice = indice;
            setAlignmentX(CENTER_ALIGNMENT);
        }

        private Dimension taillePoints() {
            PDRectangle boite = document.getPage(indice).getCropBox();
            int rotation = document.getPage(indice).getRotation();
            boolean tournee = rotation == 90 || rotation == 270;
            return new Dimension(Math.round(tournee ? boite.getHeight() : boite.getWidth()),
                    Math.round(tournee ? boite.getWidth() : boite.getHeight()));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension pt = taillePoints();
            return new Dimension(Math.round(pt.width * zoom * POINTS_EN_PIXELS) + 2 + OMBRE,
                    Math.round(pt.height * zoom * POINTS_EN_PIXELS) + 2 + OMBRE);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                // Feuille : contour, ombre portée en bas à droite, puis la page.
                int l = getWidth() - 2 - OMBRE;
                int h = getHeight() - 2 - OMBRE;
                g2.setColor(new Color(0, 0, 0, 22));
                g2.fillRect(OMBRE + 1, OMBRE + 1, l + 1, h + 1);
                g2.setColor(Theme.BORDURE);
                g2.drawRect(0, 0, l + 1, h + 1);
                g2.translate(1, 1);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, l, h);
                // Écrans haute densité : on rend à la résolution réelle de l'écran.
                GraphicsConfiguration gc = getGraphicsConfiguration();
                double densite = gc == null ? 1 : gc.getDefaultTransform().getScaleX();
                float echelle = (float) (zoom * POINTS_EN_PIXELS * densite);
                if (image == null || Math.abs(echelle - echelleImage) > 0.001f) {
                    image = rendre(rendu, indice, echelle);
                    echelleImage = echelle;
                }
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(image, 0, 0, l, h, null);
            } catch (IOException | RuntimeException e) {
                LOG.log(Level.WARNING, "Rendu de la page " + (indice + 1) + " de " + fichier, e);
                g2.setColor(Theme.DANGER);
                g2.drawString("Page illisible : ouvrez le document dans la visionneuse.", 20, 30);
            } finally {
                g2.dispose();
            }
        }
    }
}
