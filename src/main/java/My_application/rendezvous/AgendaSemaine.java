package My_application.rendezvous;

import My_application.entities.Rendez_Vous;
import My_application.entities.patient;
import My_application.vue.Theme;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

/**
 * Agenda d'une semaine (dimanche → samedi), dessiné comme un planning : une
 * colonne par jour, une ligne par créneau ({@code 60 / maxParHeure} minutes),
 * et chaque rendez-vous sous forme de carte colorée selon son
 * {@link StatutRendezVous}.
 * <p>
 * Le composant ne lit pas la base : {@code calandar} lui donne les
 * rendez-vous de la semaine et reçoit les clics par un {@link Ecouteur}.
 * L'en-tête des jours ({@link #getEnTete()}) et la colonne des heures
 * ({@link #getColonneHeures()}) se placent dans le {@code JScrollPane}.
 */
public class AgendaSemaine extends JComponent implements Scrollable {

    private static final long serialVersionUID = 1L;

    /** Réactions de la fenêtre aux clics dans l'agenda. */
    public interface Ecouteur {

        /** Clic sur la carte d'un rendez-vous. */
        void rendezVousChoisi(Rendez_Vous r);

        /** Clic sur une case libre : l'heure (pleine) de ce jour. */
        void creneauChoisi(LocalDate jour, int heure, boolean doubleClic);
    }

    static final int HAUTEUR_CRENEAU = 40;
    static final int LARGEUR_HEURES = 64;
    static final int HAUTEUR_EN_TETE = 54;
    private static final int LARGEUR_MIN_JOUR = 120;
    private static final String[] JOURS_COURTS = {"Dim.", "Lun.", "Mar.", "Mer.", "Jeu.", "Ven.", "Sam."};
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH);
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_LONGUE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private final int heureOuverture;
    private final int nombreHeures;
    private final int maxParHeure;
    private final int pas;
    private final patient courant;
    private final EnTete enTete = new EnTete();
    private final ColonneHeures colonneHeures = new ColonneHeures();

    private Clock horloge = Clock.systemDefaultZone();
    private LocalDate dimanche;
    private List<Rendez_Vous> rendezVous = new ArrayList<>();
    private Rendez_Vous selection;
    private Rendez_Vous propose;
    private String filtre = "";
    private Point survol;
    private Ecouteur ecouteur;

    public AgendaSemaine(int heureOuverture, int nombreHeures, int maxParHeure, patient courant) {
        this.heureOuverture = heureOuverture;
        this.nombreHeures = nombreHeures;
        this.maxParHeure = maxParHeure;
        this.pas = 60 / maxParHeure;
        this.courant = courant;
        setOpaque(true);
        setBackground(Theme.SURFACE);
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter souris = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                survol = e.getPoint();
                setCursor(Cursor.getPredefinedCursor(cliquable(survol) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                survol = null;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    clic(e.getPoint(), e.getClickCount() >= 2);
                }
            }
        };
        addMouseListener(souris);
        addMouseMotionListener(souris);
        // L'en-tête suit la largeur de la grille (colonnes alignées).
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                enTete.revalidate();
                enTete.repaint();
            }
        });
    }

    // ----- données -----

    /** Affiche la semaine commençant au dimanche donné. */
    public void afficherSemaine(LocalDate dimancheDeLaSemaine, List<Rendez_Vous> rdvs) {
        this.dimanche = dimancheDeLaSemaine;
        this.rendezVous = new ArrayList<>(rdvs);
        selection = selection == null ? null : trouver(selection);
        enTete.repaint();
        repaint();
    }

    /** Le rendez-vous affiché qui désigne le même patient au même créneau, ou null. */
    public Rendez_Vous trouver(Rendez_Vous r) {
        for (Rendez_Vous x : rendezVous) {
            if (x.memeCreneauEtPatient(r)) {
                return x;
            }
        }
        return null;
    }

    public LocalDate getDimanche() {
        return dimanche;
    }

    public List<Rendez_Vous> getRendezVous() {
        return rendezVous;
    }

    /** Nombre de rendez-vous de la semaine qui correspondent à la recherche. */
    public int nombreVisibles() {
        int n = 0;
        for (Rendez_Vous r : rendezVous) {
            if (correspond(r, filtre)) {
                n++;
            }
        }
        return n;
    }

    public void setSelection(Rendez_Vous r) {
        selection = r;
        repaint();
    }

    public Rendez_Vous getSelection() {
        return selection;
    }

    /** Créneau proposé (pas encore enregistré), dessiné en pointillés. */
    public void setPropose(Rendez_Vous r) {
        propose = r;
        repaint();
    }

    public Rendez_Vous getPropose() {
        return propose;
    }

    /** Recherche d'un patient : les autres cartes sont estompées. */
    public void setFiltre(String texte) {
        filtre = texte == null ? "" : texte.trim();
        repaint();
    }

    public void setEcouteur(Ecouteur ecouteur) {
        this.ecouteur = ecouteur;
    }

    /** Pour les tests : heure de référence (passé, aujourd'hui). */
    void setHorloge(Clock horloge) {
        this.horloge = horloge;
    }

    LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }

    public StatutRendezVous statut(Rendez_Vous r) {
        return StatutRendezVous.de(r, courant, maintenant());
    }

    /** Vrai si le nom ou le prénom du patient contient le texte (sans tenir compte des accents). */
    static boolean correspond(Rendez_Vous r, String texte) {
        if (texte == null || texte.trim().isEmpty()) {
            return true;
        }
        patient p = r.getPatient();
        String nom = sansAccents(p.getNom() + " " + p.getPrenom() + " " + p.getNom());
        return nom.contains(sansAccents(texte.trim()));
    }

    private static String sansAccents(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.FRENCH);
    }

    /** « Semaine du 18 au 24 octobre 2026 » (mois et année répétés seulement s'ils changent). */
    public static String libelleSemaine(LocalDate dimanche) {
        LocalDate samedi = dimanche.plusDays(6);
        DateTimeFormatter complet = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
        String debut;
        if (dimanche.getYear() != samedi.getYear()) {
            debut = complet.format(dimanche);
        } else if (dimanche.getMonth() != samedi.getMonth()) {
            debut = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH).format(dimanche);
        } else {
            debut = String.valueOf(dimanche.getDayOfMonth());
        }
        return "Semaine du " + debut + " au " + complet.format(samedi);
    }

    // ----- géométrie -----

    private int nombreCreneaux() {
        return nombreHeures * maxParHeure;
    }

    private double largeurJour() {
        return Math.max(1, getWidth()) / 7.0;
    }

    private int xJour(int j) {
        return (int) Math.round(j * largeurJour());
    }

    /** Jour sous l'abscisse x, ou null. */
    public LocalDate jourA(int x) {
        if (dimanche == null || x < 0 || x >= getWidth()) {
            return null;
        }
        int j = Math.min(6, (int) (x / largeurJour()));
        return dimanche.plusDays(j);
    }

    /** Heure pleine sous l'ordonnée y, ou -1 en dehors des heures d'ouverture. */
    public int heureA(int y) {
        int i = y / HAUTEUR_CRENEAU;
        return y < 0 || i >= nombreCreneaux() ? -1 : heureOuverture + i / maxParHeure;
    }

    /** Case occupée par la carte d'un rendez-vous, ou null s'il n'est pas dans la semaine affichée. */
    public Rectangle rectangleDe(Rendez_Vous r) {
        if (dimanche == null) {
            return null;
        }
        int j = (int) ChronoUnit.DAYS.between(dimanche, r.getDate().toLocalDate());
        LocalTime t = r.getHeure().toLocalTime();
        int i = (t.getHour() - heureOuverture) * maxParHeure + t.getMinute() / pas;
        if (j < 0 || j > 6 || t.getHour() < heureOuverture || i >= nombreCreneaux()) {
            return null;
        }
        int x = xJour(j);
        return new Rectangle(x, i * HAUTEUR_CRENEAU, xJour(j + 1) - x, HAUTEUR_CRENEAU);
    }

    /** Rendez-vous dont la carte est sous le point, ou null. */
    public Rendez_Vous rendezVousA(Point p) {
        for (Rendez_Vous r : rendezVous) {
            Rectangle c = rectangleDe(r);
            if (c != null && c.contains(p)) {
                return r;
            }
        }
        return null;
    }

    /** Ordonnée de l'heure donnée dans la grille. */
    public int yDe(LocalTime t) {
        long minutes = ChronoUnit.MINUTES.between(LocalTime.of(heureOuverture, 0), t);
        return (int) (minutes * HAUTEUR_CRENEAU / pas);
    }

    private Rectangle rectangleHeure(LocalDate jour, int heure) {
        int j = (int) ChronoUnit.DAYS.between(dimanche, jour);
        int x = xJour(j);
        int y = (heure - heureOuverture) * maxParHeure * HAUTEUR_CRENEAU;
        return new Rectangle(x, y, xJour(j + 1) - x, maxParHeure * HAUTEUR_CRENEAU);
    }

    private boolean heureFuture(LocalDate jour, int heure) {
        return jour.atTime(heure, 0).plusHours(1).isAfter(maintenant());
    }

    private boolean cliquable(Point p) {
        if (rendezVousA(p) != null) {
            return true;
        }
        LocalDate jour = jourA(p.x);
        int heure = heureA(p.y);
        return courant != null && jour != null && heure >= 0 && heureFuture(jour, heure);
    }

    private int nombreDansLHeure(LocalDate jour, int heure) {
        int n = 0;
        for (Rendez_Vous r : rendezVous) {
            if (r.getDate().toLocalDate().equals(jour) && r.getHeure().toLocalTime().getHour() == heure) {
                n++;
            }
        }
        return n;
    }

    private int nombreDuJour(LocalDate jour) {
        int n = 0;
        for (Rendez_Vous r : rendezVous) {
            if (r.getDate().toLocalDate().equals(jour)) {
                n++;
            }
        }
        return n;
    }

    private void clic(Point p, boolean doubleClic) {
        if (ecouteur == null) {
            return;
        }
        Rendez_Vous r = rendezVousA(p);
        if (r != null) {
            setSelection(r);
            ecouteur.rendezVousChoisi(r);
            return;
        }
        LocalDate jour = jourA(p.x);
        int heure = heureA(p.y);
        if (jour != null && heure >= 0) {
            ecouteur.creneauChoisi(jour, heure, doubleClic);
        }
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Rendez_Vous r = rendezVousA(e.getPoint());
        if (r != null) {
            LocalTime debut = r.getHeure().toLocalTime();
            return "<html><b>" + r.getPatient().getNom() + " " + r.getPatient().getPrenom() + "</b><br>"
                    + DATE_LONGUE.format(r.getDate().toLocalDate()) + "<br>"
                    + HEURE.format(debut) + " – " + HEURE.format(debut.plusMinutes(pas))
                    + "<br>" + statut(r).getLibelle() + "</html>";
        }
        LocalDate jour = jourA(e.getX());
        int heure = heureA(e.getY());
        if (jour == null || heure < 0) {
            return null;
        }
        int n = nombreDansLHeure(jour, heure);
        String places = n + " / " + maxParHeure + " places prises de " + heure + " h à " + (heure + 1) + " h";
        if (!heureFuture(jour, heure)) {
            return places;
        }
        return "<html>" + places + (n < maxParHeure && courant != null
                ? "<br>Cliquez pour proposer un créneau, double-cliquez pour le réserver." : "") + "</html>";
    }

    // ----- Scrollable -----

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(7 * LARGEUR_MIN_JOUR, nombreCreneaux() * HAUTEUR_CRENEAU);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(7 * LARGEUR_MIN_JOUR, 12 * HAUTEUR_CRENEAU);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? HAUTEUR_CRENEAU / 2 : 20;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visible.height - HAUTEUR_CRENEAU : visible.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() != null && getParent().getWidth() >= 7 * LARGEUR_MIN_JOUR;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() > getPreferredSize().height;
    }

    // ----- dessin -----

    private static Color transparence(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private static Graphics2D lisse(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g2;
    }

    /** Texte coupé avec « … » s'il dépasse la largeur. */
    private static String couper(String texte, FontMetrics fm, int largeur) {
        if (fm.stringWidth(texte) <= largeur) {
            return texte;
        }
        String s = texte;
        while (s.length() > 1 && fm.stringWidth(s + "…") > largeur) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = lisse(g);
        g2.setColor(Theme.SURFACE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (dimanche == null) {
            g2.dispose();
            return;
        }
        LocalDateTime maintenant = maintenant();
        int hauteur = nombreCreneaux() * HAUTEUR_CRENEAU;

        // Fonds : jour courant teinté, créneaux passés grisés.
        for (int j = 0; j < 7; j++) {
            LocalDate jour = dimanche.plusDays(j);
            int x = xJour(j);
            int l = xJour(j + 1) - x;
            if (jour.equals(maintenant.toLocalDate())) {
                g2.setColor(transparence(Theme.PRIMAIRE_CLAIR, 110));
                g2.fillRect(x, 0, l, hauteur);
            }
            for (int i = 0; i < nombreCreneaux(); i++) {
                LocalDateTime fin = jour.atTime(heureOuverture, 0).plusMinutes((long) (i + 1) * pas);
                if (!fin.isAfter(maintenant)) {
                    g2.setColor(Theme.FOND);
                    g2.fillRect(x, i * HAUTEUR_CRENEAU, l, HAUTEUR_CRENEAU);
                }
            }
        }

        // Survol d'une heure libre où l'on peut prendre rendez-vous.
        if (survol != null && courant != null && rendezVousA(survol) == null) {
            LocalDate jour = jourA(survol.x);
            int heure = heureA(survol.y);
            if (jour != null && heure >= 0 && heureFuture(jour, heure)
                    && nombreDansLHeure(jour, heure) < maxParHeure) {
                Rectangle h = rectangleHeure(jour, heure);
                g2.setColor(transparence(Theme.PRIMAIRE_CLAIR, 200));
                g2.fillRect(h.x, h.y, h.width, h.height);
                int i = survol.y / HAUTEUR_CRENEAU;
                g2.setFont(Theme.police(Font.PLAIN, 11));
                g2.setColor(Theme.PRIMAIRE);
                g2.drawString(couper("+ Proposer un créneau", g2.getFontMetrics(), h.width - 14),
                        h.x + 8, i * HAUTEUR_CRENEAU + 24);
            }
        }

        // Grille.
        Color ligneQuart = transparence(Theme.BORDURE, 110);
        for (int i = 0; i <= nombreCreneaux(); i++) {
            g2.setColor(i % maxParHeure == 0 ? Theme.BORDURE : ligneQuart);
            g2.drawLine(0, i * HAUTEUR_CRENEAU, getWidth(), i * HAUTEUR_CRENEAU);
        }
        g2.setColor(Theme.BORDURE);
        for (int j = 1; j < 7; j++) {
            g2.drawLine(xJour(j), 0, xJour(j), hauteur);
        }

        // Cartes.
        for (Rendez_Vous r : rendezVous) {
            Rectangle c = rectangleDe(r);
            if (c != null) {
                dessinerCarte(g2, r, c);
            }
        }
        if (propose != null) {
            Rectangle c = rectangleDe(propose);
            if (c != null) {
                dessinerProposition(g2, propose, c);
            }
        }

        // Heure actuelle.
        int jAujourdhui = (int) ChronoUnit.DAYS.between(dimanche, maintenant.toLocalDate());
        LocalTime t = maintenant.toLocalTime();
        if (jAujourdhui >= 0 && jAujourdhui < 7 && t.getHour() >= heureOuverture
                && t.getHour() < heureOuverture + nombreHeures) {
            int y = yDe(t);
            int x = xJour(jAujourdhui);
            g2.setColor(Theme.DANGER);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(x, y, xJour(jAujourdhui + 1), y);
            g2.fillOval(x - 1, y - 4, 8, 8);
        }
        g2.dispose();
    }

    private void dessinerCarte(Graphics2D g, Rendez_Vous r, Rectangle c) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (!correspond(r, filtre)) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        }
        StatutRendezVous s = statut(r);
        int x = c.x + 4;
        int y = c.y + 2;
        int l = c.width - 8;
        int h = c.height - 4;
        boolean choisi = r == selection;
        boolean survole = survol != null && c.contains(survol);
        g2.setColor(s.getFond());
        g2.fillRoundRect(x, y, l, h, 10, 10);
        g2.setColor(choisi ? Theme.PRIMAIRE_FONCE : transparence(s.getCouleur(), survole ? 200 : 110));
        g2.setStroke(new BasicStroke(choisi ? 2f : 1f));
        g2.drawRoundRect(x, y, l - 1, h - 1, 10, 10);

        g2.setColor(s.getCouleur());
        g2.fillOval(x + 8, y + 7, 7, 7);
        LocalTime debut = r.getHeure().toLocalTime();
        g2.setFont(Theme.police(Font.BOLD, 10.5f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(couper(HEURE.format(debut) + " – " + HEURE.format(debut.plusMinutes(pas)), fm, l - 26),
                x + 20, y + 14);
        g2.setFont(Theme.police(Font.BOLD, 12));
        fm = g2.getFontMetrics();
        g2.setColor(s == StatutRendezVous.PASSE ? Theme.TEXTE_SECONDAIRE : Theme.TEXTE);
        String nom = r.getPatient().getNom().toUpperCase(Locale.FRENCH) + " " + r.getPatient().getPrenom();
        g2.drawString(couper(nom, fm, l - 16), x + 8, y + h - 7);
        g2.dispose();
    }

    private void dessinerProposition(Graphics2D g, Rendez_Vous r, Rectangle c) {
        Graphics2D g2 = (Graphics2D) g.create();
        int x = c.x + 4;
        int y = c.y + 2;
        int l = c.width - 8;
        int h = c.height - 4;
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(x, y, l, h, 10, 10);
        g2.setColor(Theme.PRIMAIRE);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 3f}, 0f));
        g2.drawRoundRect(x, y, l - 1, h - 1, 10, 10);
        LocalTime debut = r.getHeure().toLocalTime();
        g2.setFont(Theme.police(Font.BOLD, 10.5f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(couper(HEURE.format(debut) + " · proposé", fm, l - 16), x + 8, y + 14);
        g2.setFont(Theme.police(Font.PLAIN, 12));
        fm = g2.getFontMetrics();
        g2.setColor(Theme.PRIMAIRE_FONCE);
        String nom = r.getPatient().getNom().toUpperCase(Locale.FRENCH) + " " + r.getPatient().getPrenom();
        g2.drawString(couper(nom, fm, l - 16), x + 8, y + h - 7);
        g2.dispose();
    }

    public JComponent getEnTete() {
        return enTete;
    }

    public JComponent getColonneHeures() {
        return colonneHeures;
    }

    /** Jours de la semaine, au-dessus de la grille (en-tête de colonnes du JScrollPane). */
    private class EnTete extends JComponent {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Math.max(AgendaSemaine.this.getWidth(), AgendaSemaine.this.getPreferredSize().width),
                    HAUTEUR_EN_TETE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = lisse(g);
            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.BORDURE);
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            if (dimanche == null) {
                g2.dispose();
                return;
            }
            LocalDate aujourdhui = maintenant().toLocalDate();
            for (int j = 0; j < 7; j++) {
                LocalDate jour = dimanche.plusDays(j);
                int x = xJour(j);
                int l = xJour(j + 1) - x;
                if (j > 0) {
                    g2.setColor(Theme.BORDURE);
                    g2.drawLine(x, 0, x, getHeight());
                }
                String titre = JOURS_COURTS[j] + " " + jour.getDayOfMonth();
                g2.setFont(Theme.police(Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int lt = fm.stringWidth(titre);
                if (jour.equals(aujourdhui)) {
                    g2.setColor(Theme.PRIMAIRE);
                    g2.fillRoundRect(x + (l - lt) / 2 - 10, 7, lt + 20, 22, 22, 22);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(Theme.TEXTE);
                }
                g2.drawString(titre, x + (l - lt) / 2, 23);
                int n = nombreDuJour(jour);
                String sousTitre = capitaliser(MOIS.format(jour)) + (n > 0 ? " · " + n + " rdv" : "");
                g2.setFont(Theme.police(Font.PLAIN, 11));
                fm = g2.getFontMetrics();
                g2.setColor(Theme.TEXTE_SECONDAIRE);
                sousTitre = couper(sousTitre, fm, l - 8);
                g2.drawString(sousTitre, x + (l - fm.stringWidth(sousTitre)) / 2, 45);
            }
            g2.dispose();
        }
    }

    static String capitaliser(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Heures des créneaux, à gauche de la grille (en-tête de lignes du JScrollPane). */
    private class ColonneHeures extends JComponent {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(LARGEUR_HEURES, AgendaSemaine.this.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = lisse(g);
            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            for (int i = 0; i < nombreCreneaux(); i++) {
                LocalTime t = LocalTime.of(heureOuverture, 0).plusMinutes((long) i * pas);
                boolean pleine = i % maxParHeure == 0;
                g2.setFont(pleine ? Theme.police(Font.BOLD, 12) : Theme.police(Font.PLAIN, 11));
                g2.setColor(pleine ? Theme.TEXTE : Theme.TEXTE_SECONDAIRE);
                String texte = HEURE.format(t);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(texte, getWidth() - 12 - fm.stringWidth(texte), i * HAUTEUR_CRENEAU + 16);
                if (pleine) {
                    g2.setColor(Theme.BORDURE);
                    g2.drawLine(0, i * HAUTEUR_CRENEAU, getWidth(), i * HAUTEUR_CRENEAU);
                }
            }
            g2.setColor(Theme.BORDURE);
            g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            g2.dispose();
        }
    }
}
