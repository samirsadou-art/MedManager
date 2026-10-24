package My_application.messagerie;

import My_application.Patient.FichePatient;
import My_application.controller.MessagerieController;
import My_application.controller.MessagerieVue;
import My_application.entities.MessageInterne;
import My_application.entities.patient;
import My_application.service.MessagerieService;
import My_application.service.MessagerieService.Conversation;
import My_application.service.PatientService;
import My_application.vue.Dialogues;
import My_application.vue.EnTetePage;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * Messagerie interne (Vue) : à gauche « Tout le cabinet » et les membres
 * (point vert : en ligne, pastille : non-lus), à droite le fil choisi et la
 * zone de saisie. Un message peut citer un patient ; un double-clic sur le
 * message ouvre sa fiche administrative.
 * <p>
 * La fenêtre est créée au démarrage et seulement masquée à la fermeture :
 * la fenêtre principale appelle {@link #verifier()} toutes les 3 secondes
 * pour le badge du menu et le son.
 */
public class FenetreMessagerie extends JFrame implements MessagerieVue {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.FRENCH);

    private final transient MessagerieController controleur;
    private final transient PatientService patientService;
    private final transient IntConsumer surNonLus;
    private final Integer moi;

    private final DefaultListModel<Conversation> conversations = new DefaultListModel<>();
    private final JList<Conversation> listeConversations = new JList<>(conversations);
    private final DefaultListModel<MessageInterne> messages = new DefaultListModel<>();
    private final JList<MessageInterne> listeMessages = new JList<>(messages);
    private final JLabel titreFil = new JLabel("Choisissez une conversation");
    private final JTextArea saisie = new JTextArea(3, 40);
    private final JLabel patientCite = new JLabel();
    private final JButton retirerPatient = new JButton(Icones.svg("x", 14, Theme.TEXTE_SECONDAIRE));
    private final JButton envoyer = new JButton("Envoyer", Icones.svg("send", 16, Color.WHITE));
    private transient patient cite;
    /** Évite de rouvrir le fil quand la liste est rechargée par programme. */
    private boolean miseAJour;

    /**
     * @param moi utilisateur connecté (ses messages sont à droite)
     * @param surNonLus reçoit le nombre total de non-lus (badge du menu)
     */
    public FenetreMessagerie(MessagerieService messagerie, PatientService patientService, Integer moi,
            IntConsumer surNonLus) {
        this.patientService = patientService;
        this.moi = moi;
        this.surNonLus = surNonLus;
        setTitle("Messagerie du cabinet");
        setIconImages(Theme.iconesFenetre());
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        construire();
        Theme.harmoniser(this);
        EnTetePage.installer(this, "message-circle", "Messagerie",
                "Messages entre les membres du cabinet, enregistrés dans la base du cabinet");
        setSize(1000, 680);
        setLocationRelativeTo(null);
        controleur = new MessagerieController(this, messagerie, patientService);
    }

    private void construire() {
        listeConversations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listeConversations.setCellRenderer(new RenduConversation());
        listeConversations.addListSelectionListener(e -> {
            Conversation c = listeConversations.getSelectedValue();
            if (!e.getValueIsAdjusting() && !miseAJour && c != null) {
                controleur.ouvrir(c.getIdAutre());
            }
        });
        JScrollPane gauche = new JScrollPane(listeConversations);
        gauche.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDURE));
        gauche.setMinimumSize(new Dimension(240, 100));

        titreFil.setFont(Theme.police(Font.BOLD, 15f));
        titreFil.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDURE),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        listeMessages.setCellRenderer(new RenduMessage());
        listeMessages.setBackground(Theme.FOND);
        listeMessages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listeMessages.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MessageInterne m = listeMessages.getSelectedValue();
                if (e.getClickCount() == 2 && m != null && m.getPatient() != null) {
                    ouvrirFiche(m.getPatient());
                }
            }
        });
        JScrollPane fil = new JScrollPane(listeMessages);
        fil.setBorder(BorderFactory.createEmptyBorder());

        saisie.setLineWrap(true);
        saisie.setWrapStyleWord(true);
        saisie.setToolTipText("Entrée : envoyer · Maj+Entrée : aller à la ligne");
        saisie.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        saisie.insert("\n", saisie.getCaretPosition());
                    } else {
                        envoyer();
                    }
                    e.consume();
                }
            }
        });
        JButton citer = new JButton("Citer un patient…", Icones.svg("user", 16, Theme.PRIMAIRE));
        citer.addActionListener(e -> citerPatient());
        patientCite.setForeground(Theme.INFO);
        patientCite.setFont(Theme.policeGras());
        retirerPatient.setToolTipText("Ne plus citer ce patient");
        retirerPatient.setVisible(false);
        retirerPatient.addActionListener(e -> citer(null));
        envoyer.setBackground(Theme.PRIMAIRE);
        envoyer.setForeground(Color.WHITE);
        envoyer.addActionListener(e -> envoyer());

        JPanel outils = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        outils.setOpaque(false);
        outils.add(citer);
        outils.add(patientCite);
        outils.add(retirerPatient);
        JPanel droiteSaisie = new JPanel(new BorderLayout());
        droiteSaisie.setOpaque(false);
        droiteSaisie.add(envoyer, BorderLayout.SOUTH);
        JPanel composer = new JPanel(new BorderLayout(8, 6));
        composer.setBackground(Theme.SURFACE);
        composer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDURE),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        composer.add(outils, BorderLayout.NORTH);
        composer.add(new JScrollPane(saisie), BorderLayout.CENTER);
        composer.add(droiteSaisie, BorderLayout.EAST);

        JPanel droite = new JPanel(new BorderLayout());
        droite.setBackground(Theme.SURFACE);
        droite.add(titreFil, BorderLayout.NORTH);
        droite.add(fil, BorderLayout.CENTER);
        droite.add(composer, BorderLayout.SOUTH);

        JSplitPane centre = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gauche, droite);
        centre.setDividerLocation(270);
        centre.setBorder(BorderFactory.createEmptyBorder());
        getContentPane().add(centre, BorderLayout.CENTER);
        activerSaisie(false);
    }

    private void activerSaisie(boolean actif) {
        saisie.setEnabled(actif);
        envoyer.setEnabled(actif);
    }

    // ----- appelé par la fenêtre principale -----

    /** Montre la fenêtre, sur « Tout le cabinet » si aucun fil n'est ouvert. */
    public void montrer() {
        setVisible(true);
        toFront();
        if (controleur.isFilChoisi()) {
            controleur.rafraichir();
        } else {
            controleur.ouvrir(null);
        }
    }

    public void verifier() {
        controleur.verifier();
    }

    public void signalerPresence(String poste) {
        controleur.signalerPresence(poste);
    }

    public void demarrer() {
        controleur.purger();
        controleur.rafraichir();
    }

    public void quitter() {
        controleur.quitter();
    }

    // ----- actions -----

    private void envoyer() {
        if (controleur.envoyer(saisie.getText(), cite)) {
            saisie.setText("");
            citer(null);
        }
    }

    private void citerPatient() {
        String texte = JOptionPane.showInputDialog(this, "Nom ou téléphone du patient à citer :",
                "Citer un patient", JOptionPane.QUESTION_MESSAGE);
        if (texte == null || texte.trim().isEmpty()) {
            return;
        }
        List<patient> trouves = controleur.rechercherPatients(texte.trim());
        if (trouves.isEmpty()) {
            Dialogues.information(this, "Aucun patient ne correspond à « " + texte.trim() + " ».");
            return;
        }
        if (trouves.size() == 1) {
            citer(trouves.get(0));
            return;
        }
        Object choix = JOptionPane.showInputDialog(this, "Patient à citer :", "Citer un patient",
                JOptionPane.QUESTION_MESSAGE, null, trouves.toArray(), trouves.get(0));
        if (choix != null) {
            citer((patient) choix);
        }
    }

    private void citer(patient p) {
        cite = p;
        patientCite.setText(p == null ? "" : "Patient : " + nomPatient(p));
        retirerPatient.setVisible(p != null);
    }

    private void ouvrirFiche(patient p) {
        patient fiche = controleur.fichePatient(p);
        if (fiche == null) {
            Dialogues.information(this, "La fiche de ce patient n'existe plus.");
            return;
        }
        new FichePatient(this, patientService, fiche).setVisible(true);
    }

    private static String nomPatient(patient p) {
        return p.getNom().toUpperCase(Locale.FRENCH) + " " + p.getPrenom();
    }

    // ----- MessagerieVue -----

    @Override
    public void afficherConversations(List<Conversation> liste) {
        Integer choisi = controleur.isFilChoisi() ? controleur.getFilOuvert() : null;
        miseAJour = true;
        try {
            conversations.clear();
            for (Conversation c : liste) {
                conversations.addElement(c);
                boolean memeFil = choisi == null ? c.isTousLeCabinet() : choisi.equals(c.getIdAutre());
                if (controleur.isFilChoisi() && memeFil) {
                    listeConversations.setSelectedIndex(conversations.size() - 1);
                    titreFil.setText(c.getLibelle());
                    titreFil.setIcon(Icones.svg(c.isTousLeCabinet() ? "users" : "user", 20, Theme.PRIMAIRE));
                }
            }
        } finally {
            miseAJour = false;
        }
    }

    @Override
    public void afficherMessages(Integer autre, List<MessageInterne> liste) {
        boolean enBas = messages.isEmpty() || estEnBas();
        messages.clear();
        for (MessageInterne m : liste) {
            messages.addElement(m);
        }
        activerSaisie(true);
        if (enBas && !messages.isEmpty()) {
            listeMessages.ensureIndexIsVisible(messages.size() - 1);
        }
        saisie.requestFocusInWindow();
    }

    private boolean estEnBas() {
        return listeMessages.getLastVisibleIndex() >= messages.size() - 2;
    }

    @Override
    public void afficherNonLus(int total) {
        setTitle(total == 0 ? "Messagerie du cabinet" : "Messagerie du cabinet (" + total + ")");
        surNonLus.accept(total);
    }

    @Override
    public void signalerNouveauMessage() {
        Sons.jouer("call.wav");
    }

    @Override
    public boolean estAffichee() {
        return isShowing();
    }

    @Override
    public void afficherErreur(String message) {
        if (isShowing()) {
            Dialogues.erreur(this, message);
        }
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }

    // ----- rendus -----

    /** Membre : nom, rôle, point vert s'il est en ligne, pastille des non-lus. */
    private static final class RenduConversation extends JPanel implements ListCellRenderer<Conversation> {

        private static final long serialVersionUID = 1L;
        private final JLabel nom = new JLabel();
        private final JLabel detail = new JLabel();
        private final JLabel nonLus = new JLabel("", SwingConstants.CENTER);

        RenduConversation() {
            super(new BorderLayout(8, 0));
            JPanel textes = new JPanel(new BorderLayout());
            textes.setOpaque(false);
            nom.setFont(Theme.policeGras());
            nom.setIconTextGap(8);
            detail.setFont(Theme.police(Font.PLAIN, 11.5f));
            detail.setForeground(Theme.TEXTE_SECONDAIRE);
            detail.setBorder(BorderFactory.createEmptyBorder(2, 30, 0, 0));
            textes.add(nom, BorderLayout.NORTH);
            textes.add(detail, BorderLayout.SOUTH);
            nonLus.setOpaque(true);
            nonLus.setBackground(Theme.DANGER);
            nonLus.setForeground(Color.WHITE);
            nonLus.setFont(Theme.police(Font.BOLD, 11f));
            nonLus.setBorder(BorderFactory.createEmptyBorder(1, 7, 1, 7));
            JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
            droite.setOpaque(false);
            droite.add(nonLus);
            add(textes, BorderLayout.CENTER);
            add(droite, BorderLayout.EAST);
            setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Conversation> l, Conversation c, int index,
                boolean selectionne, boolean focus) {
            setBackground(selectionne ? Theme.PRIMAIRE_CLAIR : Theme.SURFACE);
            nom.setText(c.getLibelle());
            nom.setForeground(Theme.TEXTE);
            nom.setIcon(Icones.svg(c.isTousLeCabinet() ? "users" : "user", 22,
                    c.isEnLigne() ? Theme.SUCCES : Theme.TEXTE_SECONDAIRE));
            detail.setText(c.isTousLeCabinet() ? c.getDetail()
                    : c.getDetail() + (c.isEnLigne() ? " · en ligne" : ""));
            detail.setForeground(c.isEnLigne() ? Theme.SUCCES : Theme.TEXTE_SECONDAIRE);
            nonLus.setText(String.valueOf(c.getNonLus()));
            nonLus.setVisible(c.getNonLus() > 0);
            return this;
        }
    }

    /** Bulle : mes messages à droite, ceux des autres à gauche, ceux du système au centre. */
    private final class RenduMessage extends JPanel implements ListCellRenderer<MessageInterne> {

        private static final long serialVersionUID = 1L;
        private final JLabel bulle = new JLabel();

        RenduMessage() {
            super(new FlowLayout(FlowLayout.LEFT, 12, 4));
            setBackground(Theme.FOND);
            bulle.setOpaque(true);
            bulle.setFont(Theme.police());
            add(bulle);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends MessageInterne> l, MessageInterne m,
                int index, boolean selectionne, boolean focus) {
            boolean mien = moi != null && moi.equals(m.getIdExpediteur());
            ((FlowLayout) getLayout()).setAlignment(m.isSysteme() ? FlowLayout.CENTER
                    : mien ? FlowLayout.RIGHT : FlowLayout.LEFT);
            Color bord = selectionne ? Theme.PRIMAIRE : mien ? Theme.PRIMAIRE_CLAIR : Theme.BORDURE;
            bulle.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bord),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            bulle.setBackground(m.isSysteme() ? Theme.FOND : mien ? Theme.PRIMAIRE_CLAIR : Theme.SURFACE);
            bulle.setForeground(Theme.TEXTE);
            bulle.setText(html(m, mien));
            return this;
        }

        private String html(MessageInterne m, boolean mien) {
            StringBuilder h = new StringBuilder("<html><div style='width:360px'>");
            String auteur = m.isSysteme() ? Theme.NOM_LOGICIEL : mien ? "Moi" : echapper(m.getNomExpediteur());
            String quand = m.getEnvoyeLe() == null ? ""
                    : (m.getEnvoyeLe().toLocalDate().equals(LocalDate.now()) ? HEURE : JOUR).format(m.getEnvoyeLe());
            h.append("<b>").append(auteur).append("</b> <font color='#6B7C8F'>").append(quand).append("</font><br>");
            h.append(echapper(m.getTexte()).replace("\n", "<br>"));
            if (m.getPatient() != null) {
                h.append("<br><font color='#6A3FB5'><b>Patient : ").append(echapper(nomPatient(m.getPatient())))
                        .append("</b> (double-clic : fiche)</font>");
            }
            if (mien && !m.isPourTous() && m.isLuParDestinataire()) {
                h.append("<br><font color='#2E7D32'>✓ Lu</font>");
            }
            return h.append("</div></html>").toString();
        }
    }

    /** Le texte d'un message est affiché tel quel, jamais interprété comme du HTML. */
    static String echapper(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
