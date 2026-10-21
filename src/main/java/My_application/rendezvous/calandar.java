package My_application.rendezvous;

import My_application.AppContext;
import My_application.controller.RendezVousController;
import My_application.controller.Vue;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.patient;
import My_application.service.RendezVousService;
import My_application.vue.Dialogues;
import My_application.vue.Icones;
import My_application.vue.Theme;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Agenda des rendez-vous (Vue), ouvert depuis le dossier d'un patient.
 * <p>
 * Présentation en planning : la semaine sous forme de cartes colorées par
 * statut ({@link AgendaSemaine}), et à droite un mini-calendrier, le détail du
 * rendez-vous choisi et la légende des couleurs. Un clic sur une heure libre
 * propose le premier créneau disponible pour le patient du dossier ; on le
 * réserve avec « Confirmer » ou d'un double-clic. Les règles (créneau
 * complet, un rendez-vous par heure et par patient, pas de rendez-vous dans le
 * passé) restent dans {@link RendezVousService}, via
 * {@link RendezVousController}.
 * <p>
 * Fenêtre écrite à la main (plus de {@code .form}). Elle est harmonisée
 * ({@link Theme#harmoniser}) avant d'être construite : ses composants
 * utilisent déjà la charte et ne doivent pas être retouchés.
 */
public class calandar extends javax.swing.JFrame implements Vue {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_LONGUE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final RendezVousController controleur;
    private final patient malade;
    private final int dureeCreneau;
    private final AgendaSemaine agenda;
    private final MiniCalendrier miniCalendrier;
    private final JScrollPane defilement;
    private final JLabel libelleSemaine = new JLabel();
    private final JLabel compteur = new JLabel();
    private final JTextField recherche = new JTextField();
    private final JPanel details = new JPanel();
    private final Timer minute;
    private LocalDate dimanche;
    private boolean premierAffichage = true;

    public calandar(patient Patient, User utilisateur) {
        super("Rendez-vous");
        Theme.harmoniser(this);
        this.malade = Patient;
        RendezVousService service = AppContext.get().getRendezVousService();
        controleur = new RendezVousController(this, service, Patient, utilisateur);
        dureeCreneau = 60 / service.getMaxParHeure();
        agenda = new AgendaSemaine(service.getHeureOuverture(), service.getNombreHeures(),
                service.getMaxParHeure(), Patient);
        miniCalendrier = new MiniCalendrier(service.aujourdhui());
        defilement = new JScrollPane(agenda);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        JPanel racine = new JPanel(new BorderLayout(16, 16));
        racine.setBackground(Theme.FOND);
        racine.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        racine.add(enTete(), BorderLayout.NORTH);
        racine.add(planning(), BorderLayout.CENTER);
        racine.add(colonneDroite(), BorderLayout.EAST);
        setContentPane(racine);

        agenda.setEcouteur(new AgendaSemaine.Ecouteur() {
            @Override
            public void rendezVousChoisi(Rendez_Vous r) {
                agenda.setPropose(null);
                afficherDetails(r);
            }

            @Override
            public void creneauChoisi(LocalDate jour, int heure, boolean doubleClic) {
                choisirCreneau(jour, heure, doubleClic);
            }
        });
        miniCalendrier.setEcouteur(jour -> afficherSemaine(jour));

        // L'heure actuelle et les créneaux passés avancent avec le temps.
        minute = new Timer(60_000, e -> {
            miniCalendrier.setAujourdhui(controleur.getService().aujourdhui());
            agenda.repaint();
            agenda.getEnTete().repaint();
        });
        minute.start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                minute.stop();
            }
        });

        afficherAucunDetail();
        afficherSemaine(service.aujourdhui());
        pack();
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);
    }

    // ----- construction -----

    private JComponent enTete() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);

        JLabel icone = new JLabel(Icones.svg("calendar-event", 40, Theme.PRIMAIRE));
        JLabel titre = new JLabel("Rendez-vous");
        titre.setFont(Theme.police(Font.BOLD, 24));
        titre.setForeground(Theme.TEXTE);
        JLabel sousTitre = new JLabel("Planifiez et gérez les consultations");
        sousTitre.setFont(Theme.police(Font.PLAIN, 14));
        sousTitre.setForeground(Theme.TEXTE_SECONDAIRE);
        JPanel textes = new JPanel();
        textes.setOpaque(false);
        textes.setLayout(new BoxLayout(textes, BoxLayout.Y_AXIS));
        textes.add(titre);
        textes.add(sousTitre);
        JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        gauche.setOpaque(false);
        gauche.add(icone);
        gauche.add(textes);
        p.add(gauche, BorderLayout.WEST);

        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        droite.setOpaque(false);
        if (malade != null) {
            JLabel pourPatient = new JLabel("Patient : " + nomComplet(malade),
                    Icones.svg("user", 18, Theme.SUCCES), JLabel.LEFT);
            pourPatient.setFont(Theme.policeGras());
            pourPatient.setForeground(Theme.SUCCES);
            pourPatient.setOpaque(true);
            pourPatient.setBackground(Theme.SUCCES_CLAIR);
            pourPatient.setBorder(BorderFactory.createCompoundBorder(
                    new FlatLineBorder(new Insets(0, 0, 0, 0), Theme.SUCCES_CLAIR.darker(), 1f, 18),
                    BorderFactory.createEmptyBorder(6, 12, 6, 14)));
            pourPatient.setIconTextGap(8);
            droite.add(pourPatient);
            droite.add(Box.createHorizontalStrut(8));
        }
        JButton aujourdhui = new JButton("Aujourd'hui", Icones.svg("calendar", 18, Theme.PRIMAIRE));
        aujourdhui.addActionListener(e -> afficherSemaine(controleur.getService().aujourdhui()));
        JButton actualiser = new JButton("Actualiser", Icones.svg("refresh", 18, Theme.PRIMAIRE));
        actualiser.addActionListener(e -> rafraichir());
        JButton fermer = new JButton("Fermer", Icones.svg("x", 18, Theme.PRIMAIRE));
        fermer.addActionListener(e -> dispose());
        for (JButton b : new JButton[]{aujourdhui, actualiser, fermer}) {
            b.setPreferredSize(new Dimension(b.getPreferredSize().width + 8, 38));
            droite.add(b);
        }
        p.add(droite, BorderLayout.EAST);
        return p;
    }

    private JComponent planning() {
        JPanel carte = carte(new BorderLayout());

        JPanel barre = new JPanel(new BorderLayout(12, 0));
        barre.setOpaque(false);
        barre.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        navigation.setOpaque(false);
        JButton precedente = new JButton(Icones.svg("chevron-left", 18, Theme.TEXTE));
        precedente.setToolTipText("Semaine précédente");
        precedente.addActionListener(e -> afficherSemaine(dimanche.minusWeeks(1)));
        JButton suivante = new JButton(Icones.svg("chevron-right", 18, Theme.TEXTE));
        suivante.setToolTipText("Semaine suivante");
        suivante.addActionListener(e -> afficherSemaine(dimanche.plusWeeks(1)));
        libelleSemaine.setFont(Theme.police(Font.BOLD, 16));
        libelleSemaine.setForeground(Theme.TEXTE);
        libelleSemaine.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        navigation.add(precedente);
        navigation.add(suivante);
        navigation.add(libelleSemaine);
        barre.add(navigation, BorderLayout.WEST);

        recherche.putClientProperty("JTextField.placeholderText", "Rechercher un patient…");
        recherche.putClientProperty("JTextField.leadingIcon", Icones.svg("search", 16, Theme.TEXTE_SECONDAIRE));
        recherche.putClientProperty("JTextField.showClearButton", Boolean.TRUE);
        recherche.setPreferredSize(new Dimension(260, 34));
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
        barre.add(recherche, BorderLayout.EAST);
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

        compteur.setIcon(Icones.svg("calendar", 16, Theme.TEXTE_SECONDAIRE));
        compteur.setFont(Theme.police(Font.PLAIN, 12));
        compteur.setForeground(Theme.TEXTE_SECONDAIRE);
        compteur.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        compteur.setIconTextGap(8);
        carte.add(compteur, BorderLayout.SOUTH);
        return carte;
    }

    private JComponent colonneDroite() {
        JPanel colonne = new JPanel();
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

    /** Affiche la semaine contenant ce jour. */
    private void afficherSemaine(LocalDate jour) {
        dimanche = RendezVousService.dimancheDeLaSemaine(jour);
        rafraichir();
    }

    /** Recharge la semaine affichée depuis la base. */
    void rafraichir() {
        agenda.afficherSemaine(dimanche, controleur.semaine(dimanche));
        libelleSemaine.setText(AgendaSemaine.libelleSemaine(dimanche));
        miniCalendrier.setSemaine(dimanche);
        Rendez_Vous propose = agenda.getPropose();
        if (propose != null && !dansLaSemaine(propose.getDate().toLocalDate())) {
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
        int total = agenda.getRendezVous().size();
        String texte = total + " rendez-vous sur cette semaine";
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

    private void choisirCreneau(LocalDate jour, int heure, boolean doubleClic) {
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

    private void reserver(Rendez_Vous r, boolean demanderConfirmation) {
        if (demanderConfirmation) {
            JLabel label = new JLabel("<html>Veuillez confirmer le rendez-vous suivant :<br>Patient : <b>"
                    + nomComplet(r.getPatient()) + "</b><br>Date : <b>" + r.getDateString()
                    + "</b><br>Heure : <b>" + r.getHeureString() + "</b><br>" + delai(r) + "</html>");
            label.setFont(Theme.police(Font.PLAIN, Theme.TAILLE_BASE + 1));
            int result = JOptionPane.showConfirmDialog(this, label, "Confirmation du rendez-vous ?",
                    JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }
        if (controleur.confirmer(r)) {
            agenda.setPropose(null);
            rafraichir();
            Rendez_Vous enregistre = agenda.trouver(r);
            if (enregistre != null) {
                agenda.setSelection(enregistre);
                afficherDetails(enregistre);
            } else {
                afficherAucunDetail();
            }
        }
    }

    private void annuler(Rendez_Vous r) {
        int result = JOptionPane.showConfirmDialog(this,
                "Annuler le rendez-vous de " + nomComplet(r.getPatient()) + "\n"
                + r.getDateString() + " à " + r.getHeureString() + " ?",
                "Annulation du rendez-vous", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION && controleur.annuler(r)) {
            agenda.setSelection(null);
            rafraichir();
            afficherAucunDetail();
        }
    }

    /** « Dans 3 jour(s) », « Aujourd'hui », « Passé ». */
    private String delai(Rendez_Vous r) {
        long jours = ChronoUnit.DAYS.between(controleur.getService().aujourdhui(), r.getDate().toLocalDate());
        if (jours < 0 || r.isPassed()) {
            return "Rendez-vous passé";
        }
        if (jours == 0) {
            return "Aujourd'hui";
        }
        return jours == 1 ? "Demain" : "Dans " + jours + " jours";
    }

    private String horaire(Rendez_Vous r) {
        LocalTime debut = r.getHeure().toLocalTime();
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
        JLabel aide = new JLabel("<html>Cliquez sur un rendez-vous pour voir son détail, "
                + "ou sur une heure libre pour proposer un créneau"
                + (malade == null ? "." : " à <b>" + nomComplet(malade) + "</b>.") + "</html>");
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
        details.add(ligneDetail("calendar", AgendaSemaine.capitaliser(DATE_LONGUE.format(r.getDate().toLocalDate()))));
        details.add(ligneDetail("clock", horaire(r)));
        details.add(ligneDetail("history", delai(r)));
        if (r.getDateFix() != null && r.getHeureFix() != null) {
            details.add(ligneDetail("calendar-event", "Pris le " + r.getDateFix().toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " à " + r.getHeureFixString()));
        }
        String tel = r.getPatient().getTelephone();
        if (tel != null && !tel.trim().isEmpty()) {
            details.add(ligneDetail("speakerphone", tel));
        }
        details.add(Box.createVerticalStrut(12));
        JButton annuler = boutonPlein("Annuler le rendez-vous", "trash", Theme.DANGER, Theme.DANGER.darker());
        // L'icône « trash » est rouge par défaut : sur fond rouge, elle doit être blanche.
        annuler.setIcon(Icones.svg("x", 16, Color.WHITE));
        boolean annulable = controleur.peutAnnuler(r);
        annuler.setEnabled(annulable);
        if (!annulable) {
            annuler.setToolTipText("Seul un rendez-vous à venir du patient du dossier peut être annulé.");
        }
        annuler.addActionListener(e -> annuler(r));
        details.add(annuler);
        finDetails();
    }

    private void afficherProposition(final Rendez_Vous r) {
        nouveauxDetails("Nouveau rendez-vous");
        details.add(badge("Créneau proposé", Theme.PRIMAIRE, Theme.PRIMAIRE_CLAIR));
        details.add(Box.createVerticalStrut(8));
        details.add(ligneDetail("user", "<html><b>" + nomComplet(r.getPatient()) + "</b></html>"));
        details.add(ligneDetail("calendar", AgendaSemaine.capitaliser(DATE_LONGUE.format(r.getDate().toLocalDate()))));
        details.add(ligneDetail("clock", horaire(r)));
        details.add(ligneDetail("history", delai(r)));
        details.add(Box.createVerticalStrut(12));
        JButton confirmer = boutonPlein("Confirmer le rendez-vous", "check", Theme.PRIMAIRE, Theme.PRIMAIRE_FONCE);
        confirmer.addActionListener(e -> reserver(r, false));
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
