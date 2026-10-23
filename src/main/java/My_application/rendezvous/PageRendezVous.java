package My_application.rendezvous;

import My_application.controller.RendezVousController;
import My_application.controller.Vue;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.medecin;
import My_application.entities.patient;
import My_application.report.RapportService;
import My_application.service.RendezVousService;
import My_application.vue.Dialogues;
import My_application.vue.Icones;
import My_application.vue.Theme;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Page « Rendez-vous » du panneau central de la fenêtre principale (elle
 * remplace l'ancienne fenêtre séparée {@code calandar}) : l'agenda (Vue).
 * <p>
 * Présentation en planning : la semaine sous forme de cartes colorées par
 * statut ({@link AgendaSemaine}), et à droite un mini-calendrier, le détail du
 * rendez-vous choisi et la légende des couleurs. Quand la page est ouverte
 * pour un patient (liste des patients ou dossier), un clic sur une heure libre
 * propose le premier créneau disponible pour lui ; on le réserve avec
 * « Confirmer » ou d'un double-clic. Les règles (créneau complet, un
 * rendez-vous par heure et par patient, pas de rendez-vous dans le passé)
 * restent dans {@link RendezVousService}, via {@link RendezVousController}.
 * <p>
 * Dans un cabinet à plusieurs médecins, une liste déroulante n'affiche que
 * les rendez-vous du médecin choisi ; les créneaux proposés le sont alors
 * avec ce médecin (sinon avec celui du poste ; un poste sans médecin, celui de
 * la secrétaire, doit le choisir).
 * <p>
 * Le panneau de détails propose les gestes du quotidien : déplacer (puis
 * cliquer sur la nouvelle heure), marquer absent, annuler. La recherche
 * retrouve les rendez-vous d'un patient dans tout l'agenda, et « Liste du
 * jour » produit le PDF des rendez-vous d'une journée par médecin.
 * <p>
 * Page écrite à la main : ses composants utilisent déjà la charte.
 */
public class PageRendezVous extends JPanel implements Vue {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_LONGUE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final transient RendezVousController controleur;
    /** Patient pour qui l'on prend rendez-vous ; null = agenda seul (gestes du quotidien). */
    private transient patient malade;
    private final transient Runnable choisirPatient;
    private final int dureeCreneau;
    private final AgendaSemaine agenda;
    private final MiniCalendrier miniCalendrier;
    private final JScrollPane defilement;
    private final JLabel libelleSemaine = new JLabel();
    private final JLabel compteur = new JLabel();
    private final JTextField recherche = new JTextField();
    private final JComboBox<ChoixMedecin> choixMedecin = new JComboBox<>();
    /** Médecins du cabinet, par identifiant (ordre alphabétique). */
    private final transient Map<Integer, medecin> medecins = new LinkedHashMap<>();
    private final JPanel details = new JPanel();
    /** Bandeau « Patient : … » (ou bouton « Prendre un rendez-vous… ») au-dessus du planning. */
    private final JPanel pourPatient = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final Timer minute;
    private LocalDate dimanche;
    private boolean premierAffichage = true;

    /**
     * @param utilisateur   membre connecté (auteur des rendez-vous pris)
     * @param choisirPatient montre la liste des patients, d'où l'on revient
     *                      ici pour le patient choisi (peut être null)
     */
    public PageRendezVous(User utilisateur, RendezVousService service, RapportService rapports,
            Runnable choisirPatient) {
        super(new BorderLayout(16, 16));
        this.choisirPatient = choisirPatient;
        controleur = new RendezVousController(this, service, rapports, null, utilisateur);
        dureeCreneau = 60 / service.getMaxParHeure();
        agenda = new AgendaSemaine(service.getHeureOuverture(), service.getNombreHeures(),
                service.getMaxParHeure(), null);
        miniCalendrier = new MiniCalendrier(service.aujourdhui());
        defilement = new JScrollPane(agenda);
        chargerMedecins();

        setBackground(Theme.FOND);
        setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        add(planning(), BorderLayout.CENTER);
        add(colonneDroite(), BorderLayout.EAST);

        agenda.setEcouteur(new AgendaSemaine.Ecouteur() {
            @Override
            public void rendezVousChoisi(Rendez_Vous r) {
                agenda.setPropose(null);
                agenda.setEnDeplacement(null);
                afficherDetails(r);
            }

            @Override
            public void creneauChoisi(LocalDate jour, int heure, boolean doubleClic) {
                choisirCreneau(jour, heure, doubleClic);
            }
        });
        miniCalendrier.setEcouteur(jour -> afficherSemaine(jour));
        registerKeyboardAction(e -> quitterDeplacement(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // L'heure actuelle et les créneaux passés avancent avec le temps, tant
        // que la page est à l'écran.
        minute = new Timer(60_000, e -> {
            miniCalendrier.setAujourdhui(controleur.getService().aujourdhui());
            agenda.repaint();
            agenda.getEnTete().repaint();
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                minute.start();
            } else {
                minute.stop();
            }
        });

        majPatient();
        afficherAucunDetail();
        afficherSemaine(service.aujourdhui());
    }

    /**
     * Montre la page (menu « Rendez-vous », liste des patients ou dossier) :
     * pour un patient, les heures libres lui proposent un créneau ; sans
     * patient, l'agenda sert aux gestes du quotidien. La semaine affichée est
     * gardée, la grille revient sur l'heure actuelle.
     */
    public void afficher(patient p) {
        quitterDeplacement();
        malade = p;
        controleur.setPatient(p);
        agenda.setPatient(p);
        agenda.setPropose(null);
        majPatient();
        premierAffichage = true;
        rafraichir();
        if (agenda.getSelection() == null) {
            afficherAucunDetail();
        }
        SwingUtilities.invokeLater(recherche::requestFocusInWindow);
    }

    /** Patient de la page (null sans patient). */
    public patient getPatient() {
        return malade;
    }

    /** Bandeau au-dessus du planning : patient choisi, ou accès à la liste des patients. */
    private void majPatient() {
        pourPatient.removeAll();
        if (malade != null) {
            JLabel nom = new JLabel("Patient : " + nomComplet(malade),
                    Icones.svg("user", 18, Theme.SUCCES), JLabel.LEFT);
            nom.setFont(Theme.policeGras());
            nom.setForeground(Theme.SUCCES);
            nom.setOpaque(true);
            nom.setBackground(Theme.SUCCES_CLAIR);
            nom.setBorder(BorderFactory.createCompoundBorder(
                    new FlatLineBorder(new Insets(0, 0, 0, 0), Theme.SUCCES_CLAIR.darker(), 1f, 18),
                    BorderFactory.createEmptyBorder(6, 12, 6, 14)));
            nom.setIconTextGap(8);
            pourPatient.add(nom);
            JButton fin = new JButton(Icones.svg("x", 16, Theme.TEXTE_SECONDAIRE));
            fin.setToolTipText("Terminer : ne plus prendre de rendez-vous pour ce patient");
            fin.addActionListener(e -> afficher(null));
            pourPatient.add(fin);
        } else if (choisirPatient != null) {
            JButton choisir = new JButton("Prendre un rendez-vous…", Icones.svg("user-plus", 16, Theme.PRIMAIRE));
            choisir.setToolTipText("Choisir le patient dans la liste des patients");
            choisir.addActionListener(e -> choisirPatient.run());
            pourPatient.add(choisir);
        }
        pourPatient.revalidate();
        pourPatient.repaint();
    }

    // ----- construction -----

    private JComponent planning() {
        JPanel carte = carte(new BorderLayout());

        JPanel barre = new JPanel(new BorderLayout(12, 0));
        barre.setOpaque(false);
        barre.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JButton precedente = new JButton(Icones.svg("chevron-left", 18, Theme.TEXTE));
        precedente.setToolTipText("Semaine précédente");
        precedente.addActionListener(e -> afficherSemaine(dimanche.minusWeeks(1)));
        JButton suivante = new JButton(Icones.svg("chevron-right", 18, Theme.TEXTE));
        suivante.setToolTipText("Semaine suivante");
        suivante.addActionListener(e -> afficherSemaine(dimanche.plusWeeks(1)));
        libelleSemaine.setFont(Theme.police(Font.BOLD, 16));
        libelleSemaine.setForeground(Theme.TEXTE);
        libelleSemaine.setHorizontalAlignment(JLabel.CENTER);

        recherche.putClientProperty("JTextField.placeholderText", "Rechercher un patient… (Entrée : tout l'agenda)");
        recherche.setToolTipText("Filtre la semaine affichée ; Entrée cherche les rendez-vous du patient "
                + "dans tout l'agenda (nom ou téléphone)");
        recherche.addActionListener(e -> rechercherPartout());
        recherche.putClientProperty("JTextField.leadingIcon", Icones.svg("search", 16, Theme.TEXTE_SECONDAIRE));
        recherche.putClientProperty("JTextField.showClearButton", Boolean.TRUE);
        recherche.setPreferredSize(new Dimension(270, 34));
        recherche.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrer();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrer();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrer();
            }
        });
        // À gauche le patient choisi ; « Aujourd'hui » et « Liste du jour »
        // sont dans l'en-tête de la fenêtre principale.
        pourPatient.setOpaque(false);
        barre.add(pourPatient, BorderLayout.WEST);
        JPanel filtres = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filtres.setOpaque(false);
        // Un seul médecin : la liste n'apporterait rien.
        if (medecins.size() > 1) {
            filtres.add(choixMedecin);
        }
        filtres.add(recherche);
        barre.add(filtres, BorderLayout.CENTER);
        carte.add(barre, BorderLayout.NORTH);

        defilement.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDURE));
        defilement.setColumnHeaderView(agenda.getEnTete());
        defilement.setRowHeaderView(agenda.getColonneHeures());
        JPanel coin = new JPanel();
        coin.setBackground(Theme.SURFACE);
        coin.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDURE));
        defilement.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, coin);
        defilement.getViewport().setBackground(Theme.SURFACE);
        defilement.getVerticalScrollBar().setUnitIncrement(AgendaSemaine.HAUTEUR_CRENEAU / 2);
        carte.add(defilement, BorderLayout.CENTER);

        // En bas : semaine précédente à gauche, libellé de la semaine et
        // compteur au centre, semaine suivante à droite.
        compteur.setIcon(Icones.svg("calendar", 16, Theme.TEXTE_SECONDAIRE));
        compteur.setFont(Theme.police(Font.PLAIN, 12));
        compteur.setForeground(Theme.TEXTE_SECONDAIRE);
        compteur.setHorizontalAlignment(JLabel.CENTER);
        compteur.setIconTextGap(8);
        JPanel semaine = new JPanel(new BorderLayout(0, 2));
        semaine.setOpaque(false);
        semaine.add(libelleSemaine, BorderLayout.CENTER);
        semaine.add(compteur, BorderLayout.SOUTH);
        JPanel bas = new JPanel(new BorderLayout(12, 0));
        bas.setOpaque(false);
        bas.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        bas.add(centrerVerticalement(precedente), BorderLayout.WEST);
        bas.add(semaine, BorderLayout.CENTER);
        bas.add(centrerVerticalement(suivante), BorderLayout.EAST);
        carte.add(bas, BorderLayout.SOUTH);
        return carte;
    }

    /** Le bouton garde sa taille et reste au milieu de la hauteur de la barre. */
    private static JComponent centrerVerticalement(JComponent c) {
        JPanel p = new JPanel(new java.awt.GridBagLayout());
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private JComponent colonneDroite() {
        JPanel colonne = new ColonneDefilante();
        colonne.setOpaque(false);
        colonne.setLayout(new BoxLayout(colonne, BoxLayout.Y_AXIS));

        JPanel cal = carte(new BorderLayout());
        cal.setBorder(BorderFactory.createCompoundBorder(cal.getBorder(), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        cal.add(miniCalendrier);
        colonne.add(limiter(cal));
        colonne.add(Box.createVerticalStrut(14));

        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        JPanel carteDetails = carte(new BorderLayout());
        carteDetails.setBorder(BorderFactory.createCompoundBorder(carteDetails.getBorder(),
                BorderFactory.createEmptyBorder(14, 16, 16, 16)));
        carteDetails.add(details, BorderLayout.NORTH);
        colonne.add(limiter(carteDetails));
        colonne.add(Box.createVerticalStrut(14));

        JPanel legende = carte(null);
        legende.setLayout(new BoxLayout(legende, BoxLayout.Y_AXIS));
        legende.setBorder(BorderFactory.createCompoundBorder(legende.getBorder(),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        legende.add(titreSection("Légende des statuts"));
        legende.add(Box.createVerticalStrut(6));
        for (StatutRendezVous s : StatutRendezVous.values()) {
            if (!s.dansLAgenda()) {
                continue;
            }
            legende.add(ligneLegende(new Pastille(s.getCouleur(), 10), s.getLibelle()));
        }
        legende.add(ligneLegende(new Pastille(Theme.PRIMAIRE_CLAIR.darker(), 10), "Créneau proposé (pointillés)"));
        legende.add(ligneLegende(new Pastille(Theme.DANGER, 10), "Heure actuelle"));
        colonne.add(limiter(legende));
        colonne.add(Box.createVerticalGlue());
        // Sur un petit écran, la colonne défile au lieu d'écraser ses panneaux.
        JScrollPane defilementColonne = new JScrollPane(colonne,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        defilementColonne.setBorder(BorderFactory.createEmptyBorder());
        defilementColonne.setOpaque(false);
        defilementColonne.getViewport().setOpaque(false);
        defilementColonne.getVerticalScrollBar().setUnitIncrement(16);
        defilementColonne.setPreferredSize(new Dimension(310, 0));
        return defilementColonne;
    }

    /** Colonne qui suit la largeur de son JScrollPane (les textes longs y passent à la ligne). */
    private static final class ColonneDefilante extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Entrée de la liste des médecins ; un identifiant null désigne tous les médecins. */
    private static final class ChoixMedecin {

        private final Integer id;
        private final String libelle;

        ChoixMedecin(Integer id, String libelle) {
            this.id = id;
            this.libelle = libelle;
        }

        @Override
        public String toString() {
            return libelle;
        }
    }

    /** Remplit la liste « Tous les médecins / Dr … » et donne leurs noms à l'agenda. */
    private void chargerMedecins() {
        choixMedecin.addItem(new ChoixMedecin(null, "Tous les médecins"));
        List<medecin> liste = controleur.medecins();
        for (medecin m : liste) {
            medecins.put(m.getIdmedecin(), m);
            choixMedecin.addItem(new ChoixMedecin(m.getIdmedecin(), m.toString()));
        }
        agenda.setMedecins(liste);
        agenda.setMedecinReservation(controleur.getIdMedecinReservation());
        choixMedecin.setToolTipText("N'afficher que les rendez-vous d'un médecin");
        choixMedecin.setPreferredSize(new Dimension(210, 34));
        choixMedecin.setMaximumRowCount(12);
        choixMedecin.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                boolean tous = value instanceof ChoixMedecin && ((ChoixMedecin) value).id == null;
                setIcon(Icones.svg(tous ? "users" : "stethoscope", 16,
                        isSelected && index >= 0 ? getForeground() : Theme.PRIMAIRE));
                setIconTextGap(8);
                return this;
            }
        });
        choixMedecin.addActionListener(e -> choisirMedecin());
    }

    /** Médecin choisi dans la liste, ou null pour tous. */
    private Integer medecinFiltre() {
        ChoixMedecin c = (ChoixMedecin) choixMedecin.getSelectedItem();
        return c == null ? null : c.id;
    }

    private void choisirMedecin() {
        Integer id = medecinFiltre();
        controleur.setMedecin(id);
        agenda.setMedecinReservation(controleur.getIdMedecinReservation());
        agenda.setFiltreMedecin(id);
        // Un créneau proposé l'était avec l'ancien médecin.
        agenda.setPropose(null);
        if (agenda.getSelection() == null) {
            afficherAucunDetail();
        }
        majCompteur();
    }

    /** Nom complet du médecin (« Dr SAHAB Said »), ou null s'il est inconnu. */
    private String nomMedecin(Integer id) {
        medecin m = id == null ? null : medecins.get(id);
        return m == null ? null : m.toString();
    }

    /** Panneau blanc à coins arrondis. */
    private static JPanel carte(java.awt.LayoutManager layout) {
        JPanel p = layout == null ? new JPanel() : new JPanel(layout);
        p.setBackground(Theme.SURFACE);
        p.setBorder(new FlatLineBorder(new Insets(1, 1, 1, 1), Theme.BORDURE, 1f, 14));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /** Dans une colonne BoxLayout, le panneau garde sa hauteur naturelle. */
    private static JComponent limiter(final JComponent c) {
        JPanel p = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, getPreferredSize().height);
            }
        };
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(c);
        return p;
    }

    private static JLabel titreSection(String texte) {
        JLabel l = new JLabel(texte);
        l.setFont(Theme.police(Font.BOLD, 14));
        l.setForeground(Theme.TEXTE);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel ligneLegende(Icon icone, String texte) {
        JLabel l = new JLabel(texte, icone, JLabel.LEFT);
        l.setFont(Theme.police(Font.PLAIN, 12));
        l.setForeground(Theme.TEXTE);
        l.setIconTextGap(10);
        l.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel ligneDetail(String icone, String texte) {
        JLabel l = new JLabel(texte, Icones.svg(icone, 16, Theme.TEXTE_SECONDAIRE), JLabel.LEFT);
        l.setFont(Theme.police(Font.PLAIN, 13));
        l.setForeground(Theme.TEXTE);
        l.setIconTextGap(10);
        l.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel badge(String texte, Color couleur, Color fond) {
        JLabel l = new JLabel(texte, new Pastille(couleur, 9), JLabel.LEFT);
        l.setFont(Theme.police(Font.BOLD, 12));
        l.setForeground(couleur);
        l.setOpaque(true);
        l.setBackground(fond);
        l.setIconTextGap(8);
        l.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height));
        return l;
    }

    private static JButton boutonPlein(String texte, String icone, Color couleur, Color survol) {
        JButton b = new JButton(texte, Icones.svg(icone, 16, Color.WHITE));
        b.putClientProperty("FlatLaf.style", "background: " + hex(couleur) + "; foreground: #FFFFFF;"
                + " borderColor: " + hex(couleur) + "; hoverBackground: " + hex(survol)
                + "; pressedBackground: " + hex(survol.darker()) + "; font: bold");
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        b.setPreferredSize(new Dimension(200, 36));
        return b;
    }

    private static String hex(Color c) {
        return String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }

    private static String nomComplet(patient p) {
        return p.getNom().toUpperCase(Locale.FRENCH) + " " + p.getPrenom();
    }

    // ----- navigation -----

    /** En-tête « Aujourd'hui » : revient à la semaine en cours. */
    public void allerAujourdhui() {
        afficherSemaine(controleur.getService().aujourdhui());
    }

    /** Affiche la semaine contenant ce jour. */
    private void afficherSemaine(LocalDate jour) {
        dimanche = RendezVousService.dimancheDeLaSemaine(jour);
        rafraichir();
    }

    /** Recharge la semaine affichée depuis la base. */
    public void rafraichir() {
        agenda.afficherSemaine(dimanche, controleur.semaine(dimanche));
        libelleSemaine.setText(AgendaSemaine.libelleSemaine(dimanche));
        miniCalendrier.setSemaine(dimanche);
        Rendez_Vous propose = agenda.getPropose();
        if (propose != null && !dansLaSemaine(propose.getDate())) {
            agenda.setPropose(null);
        }
        if (agenda.getSelection() == null && agenda.getPropose() == null) {
            afficherAucunDetail();
        }
        majCompteur();
        defilerVersMaintenant();
    }

    private boolean dansLaSemaine(LocalDate jour) {
        return !jour.isBefore(dimanche) && jour.isBefore(dimanche.plusDays(7));
    }

    private void filtrer() {
        agenda.setFiltre(recherche.getText());
        majCompteur();
    }

    private void majCompteur() {
        String medecin = nomMedecin(medecinFiltre());
        String texte = agenda.nombreAffiches() + " rendez-vous sur cette semaine"
                + (medecin == null ? "" : " avec " + medecin);
        if (!recherche.getText().trim().isEmpty()) {
            texte += " · " + agenda.nombreVisibles() + " pour « " + recherche.getText().trim() + " »";
        }
        compteur.setText(texte);
    }

    /** À l'ouverture, la grille montre l'heure actuelle (ou le haut de la journée). */
    private void defilerVersMaintenant() {
        if (!premierAffichage) {
            return;
        }
        premierAffichage = false;
        RendezVousService service = controleur.getService();
        LocalTime t = LocalTime.now();
        if (t.getHour() < service.getHeureOuverture()
                || t.getHour() >= service.getHeureOuverture() + service.getNombreHeures()) {
            return; // cabinet fermé : on reste en haut de la journée
        }
        final int y = Math.max(0, agenda.yDe(t) - 2 * AgendaSemaine.HAUTEUR_CRENEAU);
        SwingUtilities.invokeLater(() -> {
            agenda.scrollRectToVisible(new Rectangle(0, y, 1, defilement.getViewport().getHeight()));
        });
    }

    // ----- prise et détail des rendez-vous -----

    /** Motifs proposés ; la liste reste modifiable (texte libre, 80 caractères). */
    private static final String[] MOTIFS = {"", "Contrôle", "Première visite", "Résultats d'analyses",
        "Renouvellement d'ordonnance", "Certificat médical", "Urgence"};

    private static JComboBox<String> champMotif(String valeur) {
        JComboBox<String> c = new JComboBox<>(MOTIFS);
        c.setEditable(true);
        c.setSelectedItem(valeur == null ? "" : valeur);
        c.setToolTipText("Motif court, facultatif (" + RendezVousService.LONGUEUR_MOTIF + " caractères au plus)");
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return c;
    }

    private static String motifDe(JComboBox<String> c) {
        Object v = c.getEditor().getItem();
        return v == null ? null : v.toString();
    }

    private void choisirCreneau(LocalDate jour, int heure, boolean doubleClic) {
        if (agenda.getEnDeplacement() != null) {
            deplacerVers(agenda.getEnDeplacement(), jour, heure);
            return;
        }
        if (malade == null) {
            return; // agenda ouvert sans patient : consultation et gestes du quotidien seulement
        }
        if (!medecinChoisi()) {
            return;
        }
        agenda.setSelection(null);
        Rendez_Vous r = doubleClic ? controleur.proposer(jour, heure) : controleur.apercu(jour, heure);
        agenda.setPropose(r);
        if (r == null) {
            if (!doubleClic) {
                afficherIndisponible(jour, heure);
            } else {
                afficherAucunDetail();
            }
            return;
        }
        afficherProposition(r);
        if (doubleClic) {
            reserver(r, true);
        }
    }

    /**
     * Un poste sans médecin (secrétaire) dans un cabinet qui en a plusieurs
     * doit choisir le médecin avant de proposer un créneau. Renvoie false si
     * l'utilisateur renonce.
     */
    private boolean medecinChoisi() {
        if (controleur.getIdMedecinReservation() != null || medecins.size() <= 1) {
            return true;
        }
        JComboBox<ChoixMedecin> liste = new JComboBox<>();
        for (int i = 1; i < choixMedecin.getItemCount(); i++) {
            liste.addItem(choixMedecin.getItemAt(i));
        }
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(new JLabel("Avec quel médecin prendre ce rendez-vous ?"), BorderLayout.NORTH);
        p.add(liste, BorderLayout.CENTER);
        int choix = JOptionPane.showConfirmDialog(this, p, "Choix du médecin", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choix != JOptionPane.OK_OPTION || liste.getSelectedItem() == null) {
            return false;
        }
        // L'agenda passe sur ce médecin (déclenche choisirMedecin).
        choixMedecin.setSelectedItem(liste.getSelectedItem());
        return controleur.getIdMedecinReservation() != null;
    }

    private void reserver(Rendez_Vous r, boolean demanderConfirmation) {
        if (demanderConfirmation) {
            JLabel label = new JLabel("<html>Veuillez confirmer le rendez-vous suivant :<br>Patient : <b>"
                    + nomComplet(r.getPatient()) + "</b><br>"
                    + (medecins.size() > 1 && nomMedecin(r.getIdMedecin()) != null
                    ? "Médecin : <b>" + nomMedecin(r.getIdMedecin()) + "</b><br>" : "")
                    + "Date : <b>" + r.getDateString()
                    + "</b><br>Heure : <b>" + r.getHeureString() + "</b><br>" + delai(r) + "</html>");
            label.setFont(Theme.police(Font.PLAIN, Theme.TAILLE_BASE + 1));
            JComboBox<String> motif = champMotif(r.getMotif());
            JPanel p = new JPanel(new BorderLayout(0, 10));
            p.add(label, BorderLayout.NORTH);
            JPanel ligneMotif = new JPanel(new BorderLayout(8, 0));
            ligneMotif.add(new JLabel("Motif :"), BorderLayout.WEST);
            ligneMotif.add(motif, BorderLayout.CENTER);
            p.add(ligneMotif, BorderLayout.SOUTH);
            int result = JOptionPane.showConfirmDialog(this, p, "Confirmation du rendez-vous ?",
                    JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
            r.setMotif(motifDe(motif));
        }
        if (controleur.confirmer(r)) {
            agenda.setPropose(null);
            rafraichir();
            selectionner(r);
        }
    }

    /** Sélectionne et détaille le rendez-vous s'il est affiché. */
    private void selectionner(Rendez_Vous r) {
        Rendez_Vous affiche = agenda.trouver(r);
        if (affiche != null) {
            agenda.setSelection(affiche);
            afficherDetails(affiche);
        } else {
            agenda.setSelection(null);
            afficherAucunDetail();
        }
    }

    private void annuler(Rendez_Vous r) {
        int result = JOptionPane.showConfirmDialog(this,
                "Annuler le rendez-vous de " + nomComplet(r.getPatient()) + "\n"
                + r.getDateString() + " à " + r.getHeureString() + " ?\n"
                + "Le créneau sera libéré ; le rendez-vous reste dans l'historique du patient.",
                "Annulation du rendez-vous", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION && controleur.annuler(r)) {
            agenda.setSelection(null);
            rafraichir();
            afficherAucunDetail();
        }
    }

    private void marquerAbsent(Rendez_Vous r) {
        int result = JOptionPane.showConfirmDialog(this,
                nomComplet(r.getPatient()) + " n'est pas venu au rendez-vous du\n"
                + r.getDateString() + " à " + r.getHeureString() + " ?",
                "Patient absent", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION && controleur.marquerAbsent(r)) {
            rafraichir();
            selectionner(r);
        }
    }

    // ----- déplacement -----

    private void commencerDeplacement(Rendez_Vous r) {
        agenda.setPropose(null);
        agenda.setEnDeplacement(r);
        nouveauxDetails("Déplacer le rendez-vous");
        details.add(badge("Choisissez la nouvelle heure", Theme.PRIMAIRE, Theme.PRIMAIRE_CLAIR));
        details.add(Box.createVerticalStrut(8));
        details.add(ligneDetail("user", "<html><b>" + nomComplet(r.getPatient()) + "</b></html>"));
        ajouterMedecin(r);
        details.add(ligneDetail("calendar", AgendaSemaine.capitaliser(DATE_LONGUE.format(r.getDate()))));
        details.add(ligneDetail("clock", "Actuellement à " + r.getHeureString()));
        JLabel aide = new JLabel("<html>Cliquez sur une heure libre de l'agenda (vous pouvez changer de semaine)."
                + (medecins.size() > 1 ? " Pour changer de médecin, choisissez-le d'abord dans la liste." : "")
                + " Échap pour abandonner.</html>");
        aide.setFont(Theme.police(Font.PLAIN, 12));
        aide.setForeground(Theme.TEXTE_SECONDAIRE);
        aide.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(Box.createVerticalStrut(6));
        details.add(aide);
        details.add(Box.createVerticalStrut(12));
        JButton abandonner = new JButton("Abandonner le déplacement");
        abandonner.setAlignmentX(Component.LEFT_ALIGNMENT);
        abandonner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        abandonner.addActionListener(e -> quitterDeplacement());
        details.add(abandonner);
        finDetails();
    }

    private void quitterDeplacement() {
        Rendez_Vous r = agenda.getEnDeplacement();
        if (r == null) {
            return;
        }
        agenda.setEnDeplacement(null);
        selectionner(r);
    }

    private void deplacerVers(Rendez_Vous r, LocalDate jour, int heure) {
        Rendez_Vous nouveau = controleur.proposerDeplacement(r, jour, heure);
        if (nouveau == null) {
            return; // raison déjà affichée ; on reste en mode déplacement
        }
        String avec = nomMedecin(nouveau.getIdMedecin());
        boolean autreMedecin = !nouveau.getIdMedecin().equals(r.getIdMedecin());
        int result = JOptionPane.showConfirmDialog(this, "<html>Déplacer le rendez-vous de <b>"
                + nomComplet(r.getPatient()) + "</b><br>du " + r.getDateString() + " à " + r.getHeureString()
                + "<br>au <b>" + nouveau.getDateString() + " à " + nouveau.getHeureString() + "</b>"
                + (autreMedecin && avec != null ? "<br>avec <b>" + avec + "</b>" : "") + " ?</html>",
                "Déplacement du rendez-vous", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        if (controleur.deplacer(r, nouveau)) {
            agenda.setEnDeplacement(null);
            if (!dansLaSemaine(nouveau.getDate())) {
                dimanche = RendezVousService.dimancheDeLaSemaine(nouveau.getDate());
            }
            rafraichir();
            selectionner(nouveau);
        }
    }

    // ----- recherche et liste du jour -----

    /** Rendez-vous du patient cherché, dans tout l'agenda (passés, à venir, annulés). */
    private void rechercherPartout() {
        String texte = recherche.getText().trim();
        if (texte.isEmpty()) {
            return;
        }
        List<Rendez_Vous> trouves = controleur.rechercher(texte);
        if (trouves.isEmpty()) {
            afficherInformation("Aucun rendez-vous pour « " + texte + " ».");
            return;
        }
        final JDialog dialogue = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Rendez-vous pour « " + texte + " »", Dialog.ModalityType.APPLICATION_MODAL);
        DefaultListModel<Rendez_Vous> modele = new DefaultListModel<>();
        for (Rendez_Vous r : trouves) {
            modele.addElement(r);
        }
        final JList<Rendez_Vous> liste = new JList<>(modele);
        liste.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        liste.setCellRenderer(new RenduResultat());
        liste.setSelectedIndex(premierAVenir(trouves));
        liste.ensureIndexIsVisible(liste.getSelectedIndex());
        final Runnable afficher = () -> {
            Rendez_Vous r = liste.getSelectedValue();
            if (r != null) {
                dialogue.dispose();
                montrer(r);
            }
        };
        liste.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    afficher.run();
                }
            }
        });
        JButton voir = new JButton("Afficher dans l'agenda", Icones.svg("calendar-event", 16, Theme.PRIMAIRE));
        voir.addActionListener(e -> afficher.run());
        JButton fermer = new JButton("Fermer");
        fermer.addActionListener(e -> dialogue.dispose());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        boutons.setOpaque(false);
        boutons.add(fermer);
        boutons.add(voir);
        JLabel resume = new JLabel(trouves.size() + " rendez-vous, du plus récent au plus ancien");
        resume.setForeground(Theme.TEXTE_SECONDAIRE);
        JPanel contenu = new JPanel(new BorderLayout(0, 10));
        contenu.setBackground(Theme.FOND);
        contenu.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        contenu.add(resume, BorderLayout.NORTH);
        JScrollPane defile = new JScrollPane(liste);
        defile.setPreferredSize(new Dimension(620, 340));
        contenu.add(defile, BorderLayout.CENTER);
        contenu.add(boutons, BorderLayout.SOUTH);
        dialogue.setContentPane(contenu);
        dialogue.getRootPane().setDefaultButton(voir);
        dialogue.pack();
        dialogue.setLocationRelativeTo(this);
        dialogue.setVisible(true);
    }

    /** Indice du premier rendez-vous à venir (liste du plus récent au plus ancien), sinon 0. */
    private int premierAVenir(List<Rendez_Vous> rdvs) {
        LocalDate aujourdhui = controleur.getService().aujourdhui();
        int indice = 0;
        for (int i = 0; i < rdvs.size(); i++) {
            if (!rdvs.get(i).getDate().isBefore(aujourdhui)) {
                indice = i;
            }
        }
        return indice;
    }

    /** Affiche la semaine du rendez-vous et le sélectionne (un annulé n'y figure pas : on le détaille). */
    private void montrer(Rendez_Vous r) {
        quitterDeplacement();
        if (medecinFiltre() != null && !medecinFiltre().equals(r.getIdMedecin())) {
            choixMedecin.setSelectedIndex(0);
        }
        afficherSemaine(r.getDate());
        Rendez_Vous affiche = agenda.trouver(r);
        if (affiche != null) {
            agenda.setSelection(affiche);
            afficherDetails(affiche);
            Rectangle c = agenda.carteDe(affiche);
            if (c != null) {
                agenda.scrollRectToVisible(c);
            }
        } else {
            agenda.setSelection(null);
            afficherDetails(r);
        }
    }

    /** Ligne d'un résultat de recherche : date, heure, patient, médecin, statut. */
    private final class RenduResultat extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;
        private final DateTimeFormatter format = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.FRENCH);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Rendez_Vous r = (Rendez_Vous) value;
            StatutRendezVous s = agenda.statut(r);
            String medecin = nomMedecin(r.getIdMedecin());
            setText("<html><b>" + format.format(r.getDate()) + "  " + r.getHeureString() + "</b>   "
                    + nomComplet(r.getPatient())
                    + (medecin == null ? "" : "   ·   " + medecin)
                    + (r.getMotif() == null ? "" : "   ·   " + r.getMotif())
                    + "   ·   <i>" + s.getLibelle() + "</i></html>");
            setIcon(new Pastille(s.getCouleur(), 10));
            setIconTextGap(10);
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            return this;
        }
    }

    /** PDF des rendez-vous d'un jour de la semaine affichée (médecin choisi, ou tous). */
    public void listeDuJour() {
        JComboBox<LocalDate> jours = new JComboBox<>();
        LocalDate aujourdhui = controleur.getService().aujourdhui();
        List<LocalDate> proposes = new ArrayList<>();
        proposes.add(aujourdhui);
        proposes.add(aujourdhui.plusDays(1));
        for (int j = 0; j < 7; j++) {
            LocalDate d = dimanche.plusDays(j);
            if (!proposes.contains(d)) {
                proposes.add(d);
            }
        }
        proposes.sort(null);
        for (LocalDate d : proposes) {
            jours.addItem(d);
        }
        jours.setSelectedItem(aujourdhui.plusDays(1));
        jours.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                LocalDate d = (LocalDate) value;
                String libelle = d == null ? "" : AgendaSemaine.capitaliser(DATE_LONGUE.format(d));
                if (aujourdhui.equals(d)) {
                    libelle += " (aujourd'hui)";
                } else if (aujourdhui.plusDays(1).equals(d)) {
                    libelle += " (demain)";
                }
                setText(libelle);
                return this;
            }
        });
        String medecin = nomMedecin(medecinFiltre());
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(new JLabel("<html>Rendez-vous " + (medecin == null ? "de tous les médecins" : "de <b>" + medecin + "</b>")
                + ", une page par médecin, avec le téléphone des patients.</html>"), BorderLayout.NORTH);
        p.add(jours, BorderLayout.CENTER);
        Object[] options = {"Imprimer", "Aperçu", "Annuler"};
        int choix = JOptionPane.showOptionDialog(this, p, "Liste du jour", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, Icones.svg("printer", 28, Theme.PRIMAIRE), options, options[1]);
        if ((choix == 0 || choix == 1) && jours.getSelectedItem() != null) {
            controleur.listeDuJour((LocalDate) jours.getSelectedItem(), choix == 0);
        }
    }

    /** « Dans 3 jour(s) », « Aujourd'hui », « Passé ». */
    private String delai(Rendez_Vous r) {
        long jours = ChronoUnit.DAYS.between(controleur.getService().aujourdhui(), r.getDate());
        if (jours < 0 || r.isPassed()) {
            return "Rendez-vous passé";
        }
        if (jours == 0) {
            return "Aujourd'hui";
        }
        return jours == 1 ? "Demain" : "Dans " + jours + " jours";
    }

    private String horaire(Rendez_Vous r) {
        LocalTime debut = r.getHeure();
        return HEURE.format(debut) + " – " + HEURE.format(debut.plusMinutes(dureeCreneau))
                + "  (" + dureeCreneau + " min)";
    }

    private void nouveauxDetails(String titre) {
        details.removeAll();
        details.add(titreSection(titre));
        details.add(Box.createVerticalStrut(10));
    }

    private void finDetails() {
        details.revalidate();
        details.repaint();
    }

    private void afficherAucunDetail() {
        nouveauxDetails("Détails du rendez-vous");
        String avec = nomMedecin(controleur.getIdMedecinReservation());
        JLabel aide = new JLabel("<html>Cliquez sur un rendez-vous pour voir son détail"
                + (malade == null ? ", le déplacer ou l'annuler. Pour prendre un rendez-vous, choisissez "
                + "d'abord le patient (« Prendre un rendez-vous… » ou le dossier)."
                : ", ou sur une heure libre pour proposer un créneau à <b>" + nomComplet(malade) + "</b>"
                + (medecins.size() > 1 && avec != null ? " avec <b>" + avec + "</b>" : "")
                + ".")
                + "</html>");
        aide.setFont(Theme.police(Font.PLAIN, 12));
        aide.setForeground(Theme.TEXTE_SECONDAIRE);
        aide.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(aide);
        finDetails();
    }

    private void afficherDetails(final Rendez_Vous r) {
        StatutRendezVous s = agenda.statut(r);
        nouveauxDetails("Détails du rendez-vous");
        details.add(badge(s.getLibelle(), s.getCouleur(), s.getFond()));
        details.add(Box.createVerticalStrut(8));
        details.add(ligneDetail("user", "<html><b>" + nomComplet(r.getPatient()) + "</b>  ·  "
                + r.getPatient().getStringAge() + "</html>"));
        ajouterMedecin(r);
        details.add(ligneDetail("calendar", AgendaSemaine.capitaliser(DATE_LONGUE.format(r.getDate()))));
        details.add(ligneDetail("clock", horaire(r)));
        details.add(ligneDetail("history", delai(r)));
        if (r.getMotif() != null) {
            details.add(ligneDetail("file-text", "Motif : " + r.getMotif()));
        }
        if (r.getDateFix() != null && r.getHeureFix() != null) {
            details.add(ligneDetail("calendar-event", "Pris le " + r.getDateFix()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " à " + r.getHeureFixString()));
        }
        String tel = r.getPatient().getTelephone();
        if (tel != null && !tel.trim().isEmpty()) {
            details.add(ligneDetail("speakerphone", tel));
        }
        if (controleur.peutDeplacer(r)) {
            details.add(Box.createVerticalStrut(12));
            JButton deplacer = boutonPlein("Déplacer", "clock", Theme.PRIMAIRE, Theme.PRIMAIRE_FONCE);
            deplacer.setToolTipText("Choisir une autre heure (ou un autre jour) dans l'agenda");
            deplacer.addActionListener(e -> commencerDeplacement(r));
            details.add(deplacer);
        }
        if (controleur.peutMarquerAbsent(r)) {
            details.add(Box.createVerticalStrut(12));
            JButton absent = boutonPlein("Marquer absent", "circle-minus", Theme.AVERTISSEMENT,
                    Theme.AVERTISSEMENT.darker());
            absent.addActionListener(e -> marquerAbsent(r));
            details.add(absent);
        }
        if (controleur.peutAnnuler(r)) {
            details.add(Box.createVerticalStrut(6));
            JButton annuler = boutonPlein("Annuler le rendez-vous", "x", Theme.DANGER, Theme.DANGER.darker());
            annuler.addActionListener(e -> annuler(r));
            details.add(annuler);
        }
        finDetails();
    }

    private void afficherProposition(final Rendez_Vous r) {
        nouveauxDetails("Nouveau rendez-vous");
        details.add(badge("Créneau proposé", Theme.PRIMAIRE, Theme.PRIMAIRE_CLAIR));
        details.add(Box.createVerticalStrut(8));
        details.add(ligneDetail("user", "<html><b>" + nomComplet(r.getPatient()) + "</b></html>"));
        ajouterMedecin(r);
        details.add(ligneDetail("calendar", AgendaSemaine.capitaliser(DATE_LONGUE.format(r.getDate()))));
        details.add(ligneDetail("clock", horaire(r)));
        details.add(ligneDetail("history", delai(r)));
        details.add(Box.createVerticalStrut(8));
        JLabel titreMotif = new JLabel("Motif (facultatif)");
        titreMotif.setFont(Theme.police(Font.PLAIN, 12));
        titreMotif.setForeground(Theme.TEXTE_SECONDAIRE);
        titreMotif.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(titreMotif);
        details.add(Box.createVerticalStrut(4));
        final JComboBox<String> motif = champMotif(r.getMotif());
        details.add(motif);
        details.add(Box.createVerticalStrut(12));
        JButton confirmer = boutonPlein("Confirmer le rendez-vous", "check", Theme.PRIMAIRE, Theme.PRIMAIRE_FONCE);
        confirmer.addActionListener(e -> {
            r.setMotif(motifDe(motif));
            reserver(r, false);
        });
        details.add(confirmer);
        details.add(Box.createVerticalStrut(6));
        JButton abandonner = new JButton("Abandonner");
        abandonner.setAlignmentX(Component.LEFT_ALIGNMENT);
        abandonner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        abandonner.addActionListener(e -> {
            agenda.setPropose(null);
            afficherAucunDetail();
        });
        details.add(abandonner);
        finDetails();
    }

    private void ajouterMedecin(Rendez_Vous r) {
        String medecin = nomMedecin(r.getIdMedecin());
        if (medecin != null) {
            details.add(ligneDetail("stethoscope", medecin));
        }
    }

    private void afficherIndisponible(LocalDate jour, int heure) {
        nouveauxDetails("Nouveau rendez-vous");
        details.add(badge("Aucun créneau disponible", Theme.DANGER, Theme.DANGER_CLAIR));
        details.add(Box.createVerticalStrut(8));
        details.add(ligneDetail("calendar", AgendaSemaine.capitaliser(DATE_LONGUE.format(jour))));
        details.add(ligneDetail("clock", String.format("%02d:00 – %02d:00", heure, heure + 1)));
        JLabel raison = new JLabel("<html>L'heure est complète ou passée, ou le patient y a déjà "
                + "un rendez-vous. Double-cliquez pour connaître la raison exacte.</html>");
        raison.setFont(Theme.police(Font.PLAIN, 12));
        raison.setForeground(Theme.TEXTE_SECONDAIRE);
        raison.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(Box.createVerticalStrut(6));
        details.add(raison);
        finDetails();
    }

    @Override
    public void afficherErreur(String message) {
        Dialogues.erreur(this, message);
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }
}
