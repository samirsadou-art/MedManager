package My_application.salle;

import My_application.controller.AccueilController;
import My_application.entities.Passage;
import My_application.service.SalleAttenteService;
import My_application.vue.Icones;
import My_application.vue.RetourALaLigne;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

/**
 * Salle d'attente de l'accueil : une carte par patient, dans l'ordre
 * d'appel (en consultation, urgences, rendez-vous par heure, puis arrivées).
 * À l'accueil, les patients dont la consultation est terminée viennent en
 * tête avec les frais à régler, jusqu'à ce que la secrétaire les libère.
 * <p>
 * Chaque carte donne le rang, le patient, « RDV 09:00 » ou « Sans RDV »,
 * le médecin, l'heure d'arrivée et les minutes d'attente (en orange au-delà
 * de {@value SalleAttenteService#ATTENTE_LONGUE} minutes). Les boutons sont
 * ajoutés par la fenêtre selon le rôle ; ils agissent sur le patient
 * sélectionné.
 */
public class PanneauSalleAttente extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    private final DefaultListModel<Passage> modele = new DefaultListModel<>();
    private final JList<Passage> liste = new JList<>(modele);
    private final JLabel annonce = new JLabel();
    private final JPanel boutons = new JPanel(new RetourALaLigne(FlowLayout.LEFT, 6, 4));
    private final List<Runnable> miseAJourBoutons = new ArrayList<>();
    private final Rendu rendu = new Rendu();
    private Map<Integer, String> medecins = Collections.emptyMap();
    private LocalDateTime maintenant = LocalDateTime.now();
    /** Une seule file affichée (médecin) : le nom du médecin est inutile sur chaque carte. */
    private final boolean afficherMedecin;

    public PanneauSalleAttente(boolean afficherMedecin) {
        super(new BorderLayout());
        this.afficherMedecin = afficherMedecin;
        setOpaque(false);

        annonce.setVisible(false);
        annonce.setOpaque(true);
        annonce.setBackground(Theme.ACCENT_CLAIR);
        annonce.setForeground(Theme.ACCENT);
        annonce.setFont(Theme.policeGras());
        annonce.setIcon(Icones.svg("speakerphone", 18, Theme.ACCENT));
        annonce.setIconTextGap(8);
        annonce.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        annonce.setToolTipText("Cliquez pour masquer l'annonce");
        annonce.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                annonce.setVisible(false);
            }
        });
        add(annonce, BorderLayout.NORTH);

        liste.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        liste.setCellRenderer(rendu);
        liste.setBackground(Theme.SURFACE);
        liste.addListSelectionListener(e -> majBoutons());
        JScrollPane defilement = new JScrollPane(liste);
        defilement.setBorder(BorderFactory.createEmptyBorder());
        add(defilement, BorderLayout.CENTER);

        boutons.setOpaque(false);
        add(boutons, BorderLayout.SOUTH);
    }

    /** Remplace la file affichée, en gardant le patient sélectionné s'il y est encore. */
    public void setFile(List<Passage> file, Map<Integer, String> medecins, LocalDateTime maintenant) {
        Passage avant = liste.getSelectedValue();
        this.medecins = medecins;
        this.maintenant = maintenant;
        modele.clear();
        for (Passage p : file) {
            modele.addElement(p);
        }
        if (avant != null) {
            for (int i = 0; i < modele.size(); i++) {
                if (modele.get(i).getIdPassage().equals(avant.getIdPassage())) {
                    liste.setSelectedIndex(i);
                }
            }
        }
        liste.setToolTipText(file.isEmpty() ? "Personne en salle d'attente" : null);
        majBoutons();
    }

    /** Met à jour les minutes d'attente sans relire la base. */
    public void setMaintenant(LocalDateTime maintenant) {
        this.maintenant = maintenant;
        liste.repaint();
    }

    public void annoncer(String texte) {
        annonce.setText(texte);
        annonce.setVisible(true);
        revalidate();
    }

    public Passage getSelection() {
        return liste.getSelectedValue();
    }

    /** Patient de la file (pour les tests et les captures). */
    public int getNombre() {
        return modele.size();
    }

    /** Ouvre l'action principale par double-clic sur une carte. */
    public void surDoubleClic(Consumer<Passage> action) {
        liste.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && liste.getSelectedValue() != null) {
                    action.accept(liste.getSelectedValue());
                }
            }
        });
    }

    /** Bouton qui agit sur le patient sélectionné, actif seulement si {@code actif} l'accepte. */
    public JButton ajouterBouton(String texte, String icone, Predicate<Passage> actif, Consumer<Passage> action) {
        JButton b = new JButton(texte, Icones.svg(icone, 16, Theme.PRIMAIRE));
        b.addActionListener(e -> {
            Passage p = liste.getSelectedValue();
            if (p != null && actif.test(p)) {
                action.accept(p);
            }
        });
        miseAJourBoutons.add(() -> {
            Passage p = liste.getSelectedValue();
            b.setEnabled(p != null && actif.test(p));
        });
        boutons.add(b);
        majBoutons();
        return b;
    }

    /** Bouton toujours actif (patient suivant, patient sans rendez-vous). */
    public JButton ajouterAction(String texte, String icone, Runnable action) {
        JButton b = new JButton(texte, Icones.svg(icone, 16, Theme.PRIMAIRE));
        b.addActionListener(e -> action.run());
        boutons.add(b);
        return b;
    }

    private void majBoutons() {
        for (Runnable r : miseAJourBoutons) {
            r.run();
        }
    }

    /** Carte d'un patient dans la file. */
    private final class Rendu extends JPanel implements ListCellRenderer<Passage> {

        private static final long serialVersionUID = 1L;
        private final JLabel rang = new JLabel("", SwingConstants.CENTER);
        private final JLabel nom = new JLabel();
        private final JLabel detail = new JLabel();
        private final JLabel attente = new JLabel("", SwingConstants.RIGHT);
        private final JLabel badge = new JLabel();

        Rendu() {
            super(new GridBagLayout());
            rang.setFont(Theme.police(Font.BOLD, 18f));
            rang.setPreferredSize(new java.awt.Dimension(34, 34));
            nom.setFont(Theme.policeGras());
            detail.setFont(Theme.police(Font.PLAIN, 12f));
            attente.setFont(Theme.policeGras());
            badge.setFont(Theme.police(Font.BOLD, 11f));
            badge.setOpaque(true);
            badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            c.insets = new Insets(0, 0, 0, 10);
            add(rang, c);
            c.gridx = 1;
            c.gridheight = 1;
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 2, 0);
            add(nom, c);
            c.gridy = 1;
            add(detail, c);
            c.gridx = 2;
            c.gridy = 0;
            c.weightx = 0;
            c.anchor = GridBagConstraints.EAST;
            add(badge, c);
            c.gridy = 1;
            add(attente, c);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Passage> l, Passage p, int index,
                boolean selectionne, boolean focus) {
            boolean enConsultation = p.getEtat() == Passage.EN_CONSULTATION;
            boolean aRegler = p.getEtat() == Passage.A_REGLER;
            Color accent = aRegler ? Theme.SUCCES : enConsultation ? Theme.ACCENT
                    : p.isUrgence() ? Theme.DANGER : Theme.INFO;
            setBackground(selectionne ? Theme.PRIMAIRE_CLAIR : Theme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 1, 0, accent),
                    BorderFactory.createEmptyBorder(8, 8, 8, 10)));

            rang.setText(enConsultation || aRegler ? "" : String.valueOf(index + 1 - horsFileAvant(index)));
            rang.setIcon(aRegler ? Icones.svg("check", 22, Theme.SUCCES)
                    : enConsultation ? Icones.svg("stethoscope", 22, Theme.ACCENT) : null);
            rang.setForeground(accent);

            nom.setText(p.getPatient().getNom().toUpperCase(Locale.FRENCH) + " " + p.getPatient().getPrenom());
            nom.setForeground(Theme.TEXTE);
            StringBuilder d = new StringBuilder();
            d.append(p.isSansRendezVous() ? "Sans RDV" : "RDV " + HEURE.format(p.getHeurePrevue()));
            d.append(" · arrivé à ").append(HEURE.format(p.getArriveeLe()));
            String medecin = medecins.get(p.getIdMedecin());
            if (afficherMedecin && medecin != null) {
                d.append(" · ").append(medecin);
            }
            detail.setText(d.toString());
            detail.setForeground(Theme.TEXTE_SECONDAIRE);

            if (aRegler) {
                badge.setText(p.getFrais() == null ? "Consultation terminée"
                        : p.getFrais() == 0 ? "Gratuit" : "À régler : " + AccueilController.montant(p.getFrais()));
                badge.setForeground(Theme.SUCCES);
                badge.setBackground(Theme.SUCCES_CLAIR);
                badge.setVisible(true);
                attente.setText("attend sa sortie");
                attente.setForeground(Theme.TEXTE_SECONDAIRE);
            } else if (enConsultation) {
                badge.setText("En consultation");
                badge.setForeground(Theme.ACCENT);
                badge.setBackground(Theme.ACCENT_CLAIR);
                badge.setVisible(true);
                attente.setText(p.getAppeleLe() == null ? "" : "depuis " + HEURE.format(p.getAppeleLe()));
                attente.setForeground(Theme.TEXTE_SECONDAIRE);
            } else {
                badge.setText("Urgence");
                badge.setForeground(Theme.DANGER);
                badge.setBackground(Theme.DANGER_CLAIR);
                badge.setVisible(p.isUrgence());
                long minutes = p.minutesDAttente(maintenant);
                attente.setText(minutes + " min");
                attente.setForeground(minutes >= SalleAttenteService.ATTENTE_LONGUE
                        ? Theme.AVERTISSEMENT : Theme.TEXTE);
            }
            return this;
        }

        /** Les patients à régler ou en consultation sont en tête et ne comptent pas dans le rang d'attente. */
        private int horsFileAvant(int index) {
            int n = 0;
            for (int i = 0; i < index; i++) {
                if (modele.get(i).getEtat() != Passage.EN_ATTENTE) {
                    n++;
                }
            }
            return n;
        }
    }
}
