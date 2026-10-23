package My_application.dossier;

import My_application.entities.DossierPatient;
import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.consultation;
import My_application.entities.medicament;
import My_application.entities.patient;
import My_application.modelTableau.ListeTableModel;
import My_application.rendezvous.StatutRendezVous;
import My_application.service.HistoriquePatient;
import My_application.service.SyntheseDossier;
import My_application.service.SyntheseDossier.Examen;
import My_application.service.SyntheseDossier.Frequence;
import My_application.service.SyntheseDossier.Interpretation;
import My_application.service.SyntheseDossier.Mesure;
import My_application.service.SyntheseDossier.NiveauTension;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * Panneau central du dossier patient, en onglets : vue d'ensemble,
 * consultations (liste des visites et saisie), constantes, examens,
 * ordonnances, antécédents, rendez-vous et règlements.
 * <p>
 * Tout vient du dossier déjà chargé et de l'historique des rendez-vous et
 * passages : les onglets de synthèse sont reconstruits à chaque affichage,
 * ils montrent donc aussi la consultation en cours de saisie.
 */
public class OngletsDossier extends JTabbedPane {

    private static final long serialVersionUID = 1L;

    public static final int VUE_ENSEMBLE = 0;
    public static final int CONSULTATIONS = 1;
    public static final int CONSTANTES = 2;
    public static final int EXAMENS = 3;
    public static final int ORDONNANCES = 4;
    public static final int ANTECEDENTS = 5;
    public static final int RENDEZ_VOUS = 6;

    static final DateTimeFormatter DATE = BandeauIdentite.DATE;
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy 'à' HH:mm",
            Locale.FRENCH);

    /** Ce que les onglets demandent à la fenêtre du dossier. */
    public interface Actions {

        /** Montre la consultation dans l'onglet « Consultations ». */
        void voirConsultation(consultation c);

        /** Aperçu (false) ou impression (true) de l'ordonnance. */
        void ordonnance(consultation c, boolean imprimer);

        /** Aperçu (false) ou impression (true) de la demande d'analyses. */
        void analyses(consultation c, boolean imprimer);

        /** Agenda des rendez-vous du patient. */
        void agenda();

        /** Fiche du patient (état civil, coordonnées). */
        void modifierFiche();
    }

    private final transient DossierPatient dossier;
    private final transient Actions actions;
    private transient HistoriquePatient historique = HistoriquePatient.VIDE;

    private final JPanel ensemble = page();
    private final JPanel constantes = page();
    private final JPanel examens = page();
    private final JPanel ordonnances = page();
    private final JPanel antecedents = page();
    private final JPanel rendezVous = page();
    private final DefaultListModel<consultation> visites = new DefaultListModel<>();
    private final JList<consultation> listeVisites = new JList<>(visites);
    private final JLabel titreVisites = new JLabel();
    /** Évite de rouvrir la consultation quand la liste est remise à jour par programme. */
    private boolean majVisites;

    /** @param saisie panneau de saisie d'une consultation (constantes, symptômes, traitements) */
    public OngletsDossier(DossierPatient dossier, JComponent saisie, Actions actions) {
        this.dossier = dossier;
        this.actions = actions;
        setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE));
        addTab("Vue d'ensemble", Icones.svg("home", 16, Theme.PRIMAIRE), defilant(ensemble));
        addTab("Consultations", Icones.svg("stethoscope", 16, Theme.PRIMAIRE), pageConsultations(saisie));
        addTab("Constantes", Icones.svg("heartbeat", 16, Theme.PRIMAIRE), defilant(constantes));
        addTab("Examens", Icones.svg("flask", 16, Theme.PRIMAIRE), examens);
        addTab("Ordonnances", Icones.svg("prescription", 16, Theme.PRIMAIRE), ordonnances);
        addTab("Antécédents", Icones.svg("clipboard-list", 16, Theme.PRIMAIRE), defilant(antecedents));
        addTab("Rendez-vous et règlements", Icones.svg("calendar-event", 16, Theme.PRIMAIRE), rendezVous);
        addChangeListener(e -> rafraichirOngletCourant());
    }

    /** Rendez-vous et passages du patient (lus à l'ouverture du dossier). */
    public void setHistorique(HistoriquePatient historique) {
        this.historique = historique == null ? HistoriquePatient.VIDE : historique;
        rafraichirOngletCourant();
    }

    public HistoriquePatient getHistorique() {
        return historique;
    }

    /** Reconstruit l'onglet affiché et met à jour les compteurs des titres. */
    public final void rafraichirOngletCourant() {
        List<consultation> cs = dossier.getConsultations();
        setTitleAt(CONSULTATIONS, "Consultations (" + cs.size() + ")");
        setTitleAt(EXAMENS, "Examens (" + SyntheseDossier.examens(cs).size() + ")");
        setTitleAt(ORDONNANCES, "Ordonnances (" + SyntheseDossier.ordonnances(cs).size() + ")");
        switch (getSelectedIndex()) {
            case VUE_ENSEMBLE:
                remplir(ensemble, vueEnsemble());
                break;
            case CONSULTATIONS:
                rafraichirVisites();
                break;
            case CONSTANTES:
                remplir(constantes, vueConstantes());
                break;
            case EXAMENS:
                remplir(examens, vueExamens());
                break;
            case ORDONNANCES:
                remplir(ordonnances, vueOrdonnances());
                break;
            case ANTECEDENTS:
                remplir(antecedents, vueAntecedents());
                break;
            case RENDEZ_VOUS:
                remplir(rendezVous, vueRendezVous());
                break;
            default:
                break;
        }
    }

    // ===================================================== consultations

    private JComponent pageConsultations(JComponent saisie) {
        listeVisites.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listeVisites.setCellRenderer(new RenduVisite());
        listeVisites.setFixedCellHeight(54);
        listeVisites.addListSelectionListener(e -> {
            consultation c = listeVisites.getSelectedValue();
            if (!majVisites && !e.getValueIsAdjusting() && c != null) {
                actions.voirConsultation(c);
            }
        });
        titreVisites.setIcon(Icones.svg("history", 18, Theme.PRIMAIRE));
        titreVisites.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE + 1));
        titreVisites.setForeground(Theme.TEXTE);
        titreVisites.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 0));
        JScrollPane defile = new JScrollPane(listeVisites, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        defile.setBorder(null);
        JPanel gauche = new JPanel(new BorderLayout());
        Theme.carte(gauche, null);
        gauche.add(titreVisites, BorderLayout.NORTH);
        gauche.add(defile, BorderLayout.CENTER);
        gauche.setPreferredSize(new Dimension(260, 100));

        JPanel p = page();
        p.setLayout(new BorderLayout(12, 0));
        p.add(gauche, BorderLayout.WEST);
        p.add(saisie, BorderLayout.CENTER);
        return p;
    }

    /** Liste des visites, de la plus récente à la plus ancienne, sélection sur la consultation affichée. */
    public void rafraichirVisites() {
        majVisites = true;
        try {
            visites.clear();
            for (consultation c : SyntheseDossier.plusRecentesDAbord(dossier.getConsultations())) {
                visites.addElement(c);
            }
            titreVisites.setText("Visites (" + visites.size() + ")");
            consultation courante = dossier.getNombreConsultation() == 0 ? null : dossier.getCurrentConsultation();
            if (courante != null) {
                listeVisites.setSelectedValue(courante, true);
            } else {
                listeVisites.clearSelection();
            }
        } finally {
            majVisites = false;
        }
    }

    /** Ouvre l'onglet « Consultations » sur cette consultation. */
    public void montrerConsultation(consultation c) {
        setSelectedIndex(CONSULTATIONS);
        actions.voirConsultation(c);
        rafraichirVisites();
    }

    /** « 15/10/2026 · 10:12 », puis le médecin et le diagnostic. */
    private static final class RenduVisite extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> liste, Object valeur, int index, boolean choisie,
                boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(liste, "", index, choisie, focus);
            consultation c = (consultation) valeur;
            String quand = c.getDate_cons().format(DATE) + " · " + c.getHeureString();
            String detail = c.estNouvelle() ? "Nouvelle consultation, non enregistrée"
                    : c.getMedecin().getNomComplet().trim() + diagnostic(c);
            l.setText("<html><b>" + echapper(quand) + "</b><br><span style='color:#6B7C8F'>"
                    + echapper(detail) + "</span></html>");
            l.setFont(Theme.police());
            l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            if (!choisie) {
                l.setBackground(Theme.SURFACE);
                l.setForeground(Theme.TEXTE);
            }
            return l;
        }

        private static String diagnostic(consultation c) {
            return c.getPathologies().isEmpty() ? "" : " · " + c.getPathologies().get(0).getDesignation();
        }
    }

    // ===================================================== vue d'ensemble

    private JComponent vueEnsemble() {
        List<consultation> cs = dossier.getConsultations();
        JPanel colonnes = new JPanel(new GridLayout(1, 3, 12, 0));
        colonnes.setOpaque(false);

        colonnes.add(colonne(carteInformations(), carteProchainRendezVous()));

        List<JComponent> milieu = new ArrayList<>();
        milieu.add(carteConstantes(cs));
        milieu.add(cartePathologies(cs));
        milieu.add(carteDernieresConsultations(cs));
        colonnes.add(colonne(milieu.toArray(new JComponent[0])));

        colonnes.add(colonne(carteDerniersExamens(cs), carteDernieresOrdonnances(cs), carteDernierReglement()));
        return colonnes;
    }

    private JComponent carteInformations() {
        patient p = dossier.getMalade();
        Grille g = new Grille();
        g.ligne("Nom", p.getNom());
        g.ligne("Prénom", p.getPrenom());
        g.ligne("Date de naissance", p.getDat_nais() == null ? null
                : p.getDat_nais().format(DATE) + " (" + p.getStringAge() + ")");
        g.ligne("Sexe", p.isMasculin() ? "Masculin" : "Féminin");
        g.ligne("Situation familiale", p.getSituatMarit());
        g.ligne("Téléphone", p.getTelephone());
        g.ligne("E-mail", p.getEmail());
        g.ligne("Localité", p.getLocalite());
        g.ligne("Ville / wilaya", joindre(p.getVille(), p.getWilaya()));
        g.ligne("Pays", p.getPays());
        g.ligne("N° sécurité sociale", p.getNum_soc());
        return carte("user", "Informations personnelles", "Modifier", actions::modifierFiche, g);
    }

    private JComponent carteProchainRendezVous() {
        Rendez_Vous r = historique.prochainRendezVous(LocalDate.now());
        JComponent contenu;
        if (r == null) {
            contenu = discret("Aucun rendez-vous prévu.");
        } else {
            JPanel ligne = new JPanel(new BorderLayout(12, 0));
            ligne.setOpaque(false);
            JLabel jour = new JLabel("<html><div style='text-align:center'><span style='font-size:16pt'><b>"
                    + r.getDate().getDayOfMonth() + "</b></span><br>"
                    + r.getDate().format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH)) + "</div></html>");
            jour.setForeground(Theme.PRIMAIRE_FONCE);
            jour.setOpaque(true);
            jour.setBackground(Theme.PRIMAIRE_CLAIR);
            jour.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            String motif = r.getMotif() == null || r.getMotif().trim().isEmpty() ? "" : " · " + r.getMotif().trim();
            ligne.add(jour, BorderLayout.WEST);
            ligne.add(deuxLignes(premiereMajuscule(r.getDebut().format(JOUR)),
                    historique.nomMedecin(r.getIdMedecin()).trim() + motif), BorderLayout.CENTER);
            contenu = ligne;
        }
        return carte("calendar-event", "Prochain rendez-vous", "Agenda", actions::agenda, contenu);
    }

    private JComponent carteConstantes(List<consultation> cs) {
        JPanel tuiles = new JPanel(new GridLayout(1, 5, 8, 0));
        tuiles.setOpaque(false);
        for (Mesure m : Mesure.values()) {
            tuiles.add(tuile(cs, m));
        }
        String titre = "Dernières constantes";
        if (!cs.isEmpty()) {
            consultation derniere = SyntheseDossier.plusRecentesDAbord(cs).get(0);
            titre += " · " + derniere.getDate_cons().format(DATE);
        }
        return carte("heartbeat", titre, "Évolution", () -> setSelectedIndex(CONSTANTES), tuiles);
    }

    private static JComponent tuile(List<consultation> cs, Mesure m) {
        List<consultation> mesures = SyntheseDossier.deuxDernieresMesures(cs, m);
        JPanel t = new JPanel();
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setBackground(Theme.LIGNE_ALTERNEE);
        t.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDURE),
                BorderFactory.createEmptyBorder(6, 4, 6, 4)));
        JLabel libelle = centre(m.getLibelle(), Theme.police(Font.PLAIN, Theme.TAILLE_BASE - 2),
                Theme.TEXTE_SECONDAIRE);
        String valeur = mesures.isEmpty() ? "—" : m.texte(mesures.get(0));
        JLabel v = centre(valeur, Theme.police(Font.BOLD, Theme.TAILLE_BASE + 4),
                m == Mesure.TENSION && !mesures.isEmpty()
                ? Etiquette.couleur(NiveauTension.de(mesures.get(0))) : Theme.TEXTE);
        String unite = mesures.isEmpty() ? " " : m.getUnite();
        JLabel u = centre(unite, Theme.police(Font.PLAIN, Theme.TAILLE_BASE - 2), Theme.TEXTE_SECONDAIRE);
        String tendance = SyntheseDossier.tendance(cs, m);
        double ecart = SyntheseDossier.ecart(cs, m);
        // Une tension, un pouls ou un poids qui montent méritent l'attention ; une baisse est neutre.
        Color c = ecart > 0 && m != Mesure.RESPIRATION ? Theme.AVERTISSEMENT : Theme.TEXTE_SECONDAIRE;
        JLabel tend = centre(tendance.isEmpty() ? " " : tendance, Theme.police(Font.PLAIN, Theme.TAILLE_BASE - 2), c);
        t.add(libelle);
        t.add(v);
        t.add(u);
        t.add(tend);
        if (!mesures.isEmpty()) {
            t.setToolTipText("Mesure du " + mesures.get(0).getDate_cons().format(DATE));
        }
        return t;
    }

    private JComponent cartePathologies(List<consultation> cs) {
        List<Frequence> l = SyntheseDossier.pathologies(cs);
        JComponent contenu;
        if (l.isEmpty()) {
            contenu = discret("Aucune pathologie diagnostiquée.");
        } else {
            JPanel puces = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            puces.setOpaque(false);
            for (int i = 0; i < l.size() && i < 6; i++) {
                Frequence f = l.get(i);
                puces.add(etiquetteFrequence(f.getDesignation() + " · " + fois(f.getNombre()), f.getNombre()));
            }
            contenu = puces;
        }
        return carte("clipboard-list", "Pathologies diagnostiquées", "Antécédents",
                () -> setSelectedIndex(ANTECEDENTS), contenu);
    }

    private JComponent carteDernieresConsultations(List<consultation> cs) {
        Liste liste = new Liste();
        List<consultation> recentes = SyntheseDossier.plusRecentesDAbord(cs);
        for (int i = 0; i < recentes.size() && i < 3; i++) {
            consultation c = recentes.get(i);
            liste.ligne(c.getDate_cons().format(DATE),
                    c.estNouvelle() ? "Consultation en cours" : c.getMedecin().getNomComplet().trim(),
                    SyntheseDossier.resume(c), chevron(), () -> montrerConsultation(c));
        }
        return carte("stethoscope", "Dernières consultations", "Voir tout", () -> setSelectedIndex(CONSULTATIONS),
                recentes.isEmpty() ? discret("Aucune consultation.") : liste);
    }

    private JComponent carteDerniersExamens(List<consultation> cs) {
        Liste liste = new Liste();
        List<Examen> l = SyntheseDossier.examens(cs);
        for (int i = 0; i < l.size() && i < 5; i++) {
            Examen e = l.get(i);
            String resultat = e.getAnalyse().getResultat();
            liste.ligne(e.getConsultation().getDate_cons().format(DATE), e.getAnalyse().getDesign(),
                    resultat == null || resultat.trim().isEmpty() ? "Résultat non saisi" : resultat.trim(),
                    Etiquette.de(e.getInterpretation()), () -> montrerConsultation(e.getConsultation()));
        }
        return carte("flask", "Derniers examens", "Voir tout", () -> setSelectedIndex(EXAMENS),
                l.isEmpty() ? discret("Aucune analyse demandée.") : liste);
    }

    private JComponent carteDernieresOrdonnances(List<consultation> cs) {
        Liste liste = new Liste();
        List<consultation> l = SyntheseDossier.ordonnances(cs);
        for (int i = 0; i < l.size() && i < 2; i++) {
            consultation c = l.get(i);
            List<medicament> meds = c.getOrdoPat().getMedicaments();
            String titre = meds.get(0).getDesignmed() + (meds.size() > 1 ? "  + " + (meds.size() - 1)
                    + (meds.size() > 2 ? " autres" : " autre") : "");
            liste.ligne(c.getDate_cons().format(DATE), titre, SyntheseDossier.posologie(meds.get(0)), chevron(),
                    () -> montrerConsultation(c));
        }
        return carte("prescription", "Dernières ordonnances", "Voir tout", () -> setSelectedIndex(ORDONNANCES),
                l.isEmpty() ? discret("Aucune ordonnance.") : liste);
    }

    private JComponent carteDernierReglement() {
        Passage p = historique.dernierReglement();
        JComponent contenu;
        if (p == null) {
            contenu = discret("Aucun règlement enregistré.");
        } else {
            JPanel ligne = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            ligne.setOpaque(false);
            JLabel date = new JLabel(p.getJour().format(DATE));
            date.setForeground(Theme.TEXTE_SECONDAIRE);
            ligne.add(date);
            ligne.add(new JLabel(dinars(p.getMontantRegle()) + " réglés"));
            ligne.add(etiquetteReglement(p));
            contenu = ligne;
        }
        return carte("cash", "Dernier règlement", "Détail", () -> setSelectedIndex(RENDEZ_VOUS), contenu);
    }

    // ===================================================== constantes

    private JComponent vueConstantes() {
        List<consultation> cs = SyntheseDossier.plusRecentesDAbord(dossier.getConsultations());
        ListeTableModel<consultation> modele = new ListeTableModel<>("Date", "Médecin", "T° (°C)", "Pouls",
                "Tension", "Resp.", "Poids (kg)");
        modele.setDonnees(cs);
        JTable table = tableau(modele, (l, c, col) -> {
            l.setHorizontalAlignment(col <= 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
            switch (col) {
                case 0:
                    l.setText(c.getDate_cons().format(DATE));
                    break;
                case 1:
                    l.setText(c.getMedecin() == null ? "" : c.getMedecin().getNomComplet().trim());
                    break;
                case 4:
                    l.setText(tiret(Mesure.TENSION.texte(c)));
                    NiveauTension n = NiveauTension.de(c);
                    if (n != null && n != NiveauTension.NORMALE) {
                        l.setForeground(Etiquette.couleur(n));
                        l.setFont(Theme.policeGras());
                    }
                    break;
                default:
                    Mesure[] parColonne = {null, null, Mesure.TEMPERATURE, Mesure.POULS, null, Mesure.RESPIRATION,
                        Mesure.POIDS};
                    l.setText(tiret(parColonne[col].texte(c)));
            }
        });
        largeur(table, 0, 100);
        largeur(table, 1, 160);
        table.addMouseListener(doubleClic(table, modele));
        JPanel gauche = new JPanel(new BorderLayout(0, 6));
        gauche.setOpaque(false);
        gauche.add(new JScrollPane(table), BorderLayout.CENTER);
        gauche.add(discret("« — » : non mesuré. Tension en orange à partir de 130/85, en rouge à partir de 140/90. "
                + "Double-clic : ouvre la consultation."), BorderLayout.SOUTH);
        JComponent carteTableau = carte("heartbeat", "Constantes relevées à chaque consultation", null, null, gauche);
        carteTableau.setPreferredSize(new Dimension(560, 420));

        List<consultation> chrono = new ArrayList<>(cs);
        java.util.Collections.reverse(chrono);
        List<LocalDate> jours = new ArrayList<>();
        double[] sys = new double[chrono.size()];
        double[] dia = new double[chrono.size()];
        double[] poids = new double[chrono.size()];
        for (int i = 0; i < chrono.size(); i++) {
            consultation c = chrono.get(i);
            jours.add(c.getDate_cons());
            boolean ta = Mesure.TENSION.estMesuree(c);
            sys[i] = ta ? c.getPressionSystolique() : Double.NaN;
            dia[i] = ta ? c.getPressionDiastolique() : Double.NaN;
            poids[i] = Mesure.POIDS.valeur(c);
        }
        CourbeConstantes tension = new CourbeConstantes();
        tension.setJours(jours);
        tension.ajouterSerie("Systolique", Theme.DANGER, sys);
        tension.ajouterSerie("Diastolique", Theme.PRIMAIRE, dia);
        tension.setSeuil(130.0);
        CourbeConstantes courbePoids = new CourbeConstantes();
        courbePoids.setJours(jours);
        courbePoids.ajouterSerie("Poids", Theme.ACCENT, poids);

        JPanel droite = new JPanel(new GridLayout(2, 1, 0, 12));
        droite.setOpaque(false);
        droite.add(carte("chart-bar", "Tension artérielle (mmHg)", null, null, avecLegende(tension, "Seuil 130")));
        droite.add(carte("chart-bar", "Poids (kg)", null, null, avecLegende(courbePoids, null)));

        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false);
        p.add(carteTableau);
        p.add(droite);
        return p;
    }

    private static JComponent avecLegende(CourbeConstantes courbe, String seuil) {
        JPanel legende = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        legende.setOpaque(false);
        List<String> noms = courbe.getNomsSeries();
        for (int i = 0; i < noms.size(); i++) {
            legende.add(legende(noms.get(i), courbe.getCouleurSerie(i)));
        }
        if (seuil != null) {
            legende.add(legende(seuil, Theme.AVERTISSEMENT));
        }
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(courbe, BorderLayout.CENTER);
        p.add(legende, BorderLayout.SOUTH);
        return p;
    }

    private static JLabel legende(String texte, Color couleur) {
        JLabel l = new JLabel(texte, new My_application.rendezvous.Pastille(couleur, 10), JLabel.LEFT);
        l.setFont(Theme.police(Font.PLAIN, Theme.TAILLE_BASE - 1));
        l.setForeground(Theme.TEXTE_SECONDAIRE);
        return l;
    }

    // ===================================================== examens

    private JComponent vueExamens() {
        List<Examen> l = SyntheseDossier.examens(dossier.getConsultations());
        ListeTableModel<Examen> modele = new ListeTableModel<>("Date", "Analyse", "Observation", "Résultat",
                "Interprétation");
        modele.setDonnees(l);
        JTable table = tableau(modele, (lab, e, col) -> {
            switch (col) {
                case 0:
                    lab.setText(e.getConsultation().getDate_cons().format(DATE));
                    break;
                case 1:
                    lab.setText(e.getAnalyse().getDesign());
                    lab.setFont(Theme.policeGras());
                    break;
                case 2:
                    lab.setText(nonNul(e.getAnalyse().getObserv()));
                    break;
                case 3:
                    String r = nonNul(e.getAnalyse().getResultat()).trim();
                    lab.setText(r.isEmpty() ? "non saisi" : r);
                    if (r.isEmpty()) {
                        lab.setForeground(Theme.TEXTE_SECONDAIRE);
                    }
                    break;
                default:
                    Interpretation i = e.getInterpretation();
                    lab.setText(i.getLibelle());
                    lab.setForeground(Etiquette.couleur(i));
                    lab.setFont(Theme.policeGras());
            }
        });
        largeur(table, 0, 100);

        TableRowSorter<ListeTableModel<Examen>> tri = new TableRowSorter<>(modele);
        table.setRowSorter(tri);
        JTextField recherche = new JTextField(18);
        recherche.putClientProperty("JTextField.placeholderText", "Rechercher une analyse…");
        recherche.putClientProperty("JTextField.leadingIcon", Icones.svg("search", 16, Theme.TEXTE_SECONDAIRE));
        JComboBox<String> filtre = new JComboBox<>(new String[]{"Tous les résultats", "Normal", "À surveiller",
            "Anormal", "Non interprété", "En attente"});
        Runnable filtrer = () -> {
            String texte = recherche.getText().trim().toLowerCase(Locale.FRENCH);
            String choisi = filtre.getSelectedIndex() <= 0 ? null : (String) filtre.getSelectedItem();
            tri.setRowFilter(new RowFilter<ListeTableModel<Examen>, Integer>() {
                @Override
                public boolean include(Entry<? extends ListeTableModel<Examen>, ? extends Integer> ligne) {
                    Examen e = ligne.getModel().getElement(ligne.getIdentifier());
                    return e.getAnalyse().getDesign().toLowerCase(Locale.FRENCH).contains(texte)
                            && (choisi == null || choisi.equals(e.getInterpretation().getLibelle()));
                }
            });
        };
        recherche.getDocument().addDocumentListener(surChangement(filtrer));
        filtre.addActionListener(e -> filtrer.run());

        JButton apercu = new JButton("Aperçu de la demande", Icones.svg("eye", 16, Theme.PRIMAIRE));
        JButton imprimer = new JButton("Imprimer", Icones.svg("printer", 16, Theme.PRIMAIRE));
        apercu.addActionListener(e -> {
            Examen x = selection(table, modele);
            if (x != null) {
                actions.analyses(x.getConsultation(), false);
            }
        });
        imprimer.addActionListener(e -> {
            Examen x = selection(table, modele);
            if (x != null) {
                actions.analyses(x.getConsultation(), true);
            }
        });
        apercu.setEnabled(false);
        imprimer.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            boolean choisi = table.getSelectedRow() >= 0;
            apercu.setEnabled(choisi);
            imprimer.setEnabled(choisi);
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                Examen x = selection(table, modele);
                if (ev.getClickCount() == 2 && x != null) {
                    montrerConsultation(x.getConsultation());
                }
            }
        });

        JPanel outils = new JPanel(new BorderLayout());
        outils.setOpaque(false);
        JPanel filtres = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtres.setOpaque(false);
        filtres.add(recherche);
        filtres.add(filtre);
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        boutons.setOpaque(false);
        boutons.add(apercu);
        boutons.add(imprimer);
        outils.add(filtres, BorderLayout.WEST);
        outils.add(boutons, BorderLayout.EAST);

        JPanel contenu = new JPanel(new BorderLayout(0, 8));
        contenu.setOpaque(false);
        contenu.add(outils, BorderLayout.NORTH);
        contenu.add(new JScrollPane(table), BorderLayout.CENTER);
        contenu.add(discret("Double-clic : ouvre la consultation de la demande, où l'on saisit les résultats. "
                + "L'interprétation est la note de couleur donnée au résultat."), BorderLayout.SOUTH);
        return carte("flask", "Analyses demandées", null, null, contenu);
    }

    // ===================================================== ordonnances

    private JComponent vueOrdonnances() {
        List<consultation> l = SyntheseDossier.ordonnances(dossier.getConsultations());
        JPanel grille = new JPanel(new GridBagLayout());
        grille.setOpaque(false);
        List<JComponent> cartes = new ArrayList<>();
        List<String> textes = new ArrayList<>();
        for (consultation c : l) {
            cartes.add(carteOrdonnance(c));
            StringBuilder t = new StringBuilder();
            for (medicament m : c.getOrdoPat().getMedicaments()) {
                t.append(m.getDesignmed()).append('\n');
            }
            textes.add(t.toString().toLowerCase(Locale.FRENCH));
        }
        JTextField recherche = new JTextField(18);
        recherche.putClientProperty("JTextField.placeholderText", "Rechercher un médicament…");
        recherche.putClientProperty("JTextField.leadingIcon", Icones.svg("search", 16, Theme.TEXTE_SECONDAIRE));
        Runnable filtrer = () -> {
            grille.removeAll();
            String texte = recherche.getText().trim().toLowerCase(Locale.FRENCH);
            int n = 0;
            for (int i = 0; i < cartes.size(); i++) {
                if (textes.get(i).contains(texte)) {
                    GridBagConstraints g = new GridBagConstraints();
                    g.gridx = n % 2;
                    g.gridy = n / 2;
                    g.weightx = 1;
                    g.fill = GridBagConstraints.HORIZONTAL;
                    g.anchor = GridBagConstraints.NORTH;
                    g.insets = new Insets(0, g.gridx == 0 ? 0 : 6, 12, g.gridx == 0 ? 6 : 0);
                    grille.add(cartes.get(i), g);
                    n++;
                }
            }
            GridBagConstraints reste = new GridBagConstraints();
            reste.gridy = n / 2 + 1;
            reste.weighty = 1;
            grille.add(Box.createGlue(), reste);
            grille.revalidate();
            grille.repaint();
        };
        recherche.getDocument().addDocumentListener(surChangement(filtrer));
        filtrer.run();

        JPanel outils = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        outils.setOpaque(false);
        outils.add(recherche);
        outils.add(discret(l.size() + " ordonnance" + (l.size() > 1 ? "s" : "")
                + " · de la plus récente à la plus ancienne"));

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.add(outils, BorderLayout.NORTH);
        p.add(l.isEmpty() ? discret("Aucune ordonnance dans ce dossier.") : defilant(grille), BorderLayout.CENTER);
        return p;
    }

    private JComponent carteOrdonnance(consultation c) {
        JPanel tete = new JPanel(new BorderLayout(10, 0));
        tete.setOpaque(false);
        JLabel date = new JLabel("<html><b>" + c.getDate_cons().format(DATE) + "</b>&nbsp;&nbsp;<span "
                + "style='color:#6B7C8F'>" + echapper(c.getMedecin().getNomComplet().trim()) + "</span></html>");
        date.setFont(Theme.police());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        boutons.setOpaque(false);
        JButton apercu = new JButton("Aperçu", Icones.svg("eye", 16, Theme.PRIMAIRE));
        apercu.addActionListener(e -> actions.ordonnance(c, false));
        JButton imprimer = new JButton("Imprimer", Icones.svg("printer", 16, Theme.PRIMAIRE));
        imprimer.addActionListener(e -> actions.ordonnance(c, true));
        if (c.estNouvelle()) {
            apercu.setEnabled(false);
            imprimer.setEnabled(false);
            apercu.setToolTipText("Enregistrez d'abord la consultation");
        }
        boutons.add(apercu);
        boutons.add(imprimer);
        tete.add(date, BorderLayout.CENTER);
        tete.add(boutons, BorderLayout.EAST);

        StringBuilder html = new StringBuilder("<html><ol style='margin-left:18px'>");
        for (medicament m : c.getOrdoPat().getMedicaments()) {
            html.append("<li><b>").append(echapper(m.getDesignmed())).append("</b> <span style='color:#6B7C8F'>— ")
                    .append(echapper(SyntheseDossier.posologie(m))).append("</span></li>");
        }
        html.append("</ol></html>");
        JLabel liste = new JLabel(html.toString());
        liste.setFont(Theme.police());

        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(tete, BorderLayout.NORTH);
        p.add(liste, BorderLayout.CENTER);
        JPanel carte = new JPanel(new BorderLayout());
        Theme.carte(carte, null);
        carte.add(p);
        return carte;
    }

    // ===================================================== antécédents

    private JComponent vueAntecedents() {
        List<consultation> cs = dossier.getConsultations();
        List<Frequence> pathologies = SyntheseDossier.pathologies(cs);
        ListeTableModel<Frequence> modele = new ListeTableModel<>("Pathologie", "Première fois", "Dernière fois",
                "Consultations");
        modele.setDonnees(pathologies);
        JTable table = tableau(modele, (l, f, col) -> {
            l.setHorizontalAlignment(col == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            switch (col) {
                case 0:
                    l.setText(f.getDesignation());
                    if (f.getNombre() > 1) {
                        l.setFont(Theme.policeGras());
                    }
                    break;
                case 1:
                    l.setText(f.getPremiere() == null ? "" : f.getPremiere().format(DATE));
                    break;
                case 2:
                    l.setText(f.getDerniere() == null ? "" : f.getDerniere().format(DATE));
                    break;
                default:
                    l.setText(String.valueOf(f.getNombre()));
                    l.setFont(Theme.policeGras());
                    l.setForeground(couleurFrequence(f.getNombre()));
            }
        });
        largeur(table, 1, 110);
        largeur(table, 2, 110);
        largeur(table, 3, 120);
        int hauteur = table.getRowHeight() * Math.max(3, pathologies.size()) + 48;
        JScrollPane defile = new JScrollPane(table);
        defile.setPreferredSize(new Dimension(500, Math.min(hauteur, 380)));

        JPanel puces = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        puces.setOpaque(false);
        List<Frequence> symptomes = SyntheseDossier.symptomes(cs);
        for (int i = 0; i < symptomes.size() && i < 20; i++) {
            Frequence f = symptomes.get(i);
            puces.add(new Etiquette(f.getDesignation() + " · " + f.getNombre(), Theme.TEXTE_SECONDAIRE, Theme.FOND));
        }

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JComponent c1 = carte("clipboard-list", "Pathologies diagnostiquées au cabinet", null, null,
                pathologies.isEmpty() ? discret("Aucune pathologie notée dans les consultations.") : defile);
        JComponent c2 = carte("stethoscope", "Symptômes les plus fréquents", null, null,
                symptomes.isEmpty() ? discret("Aucun symptôme noté.") : puces);
        c1.setAlignmentX(LEFT_ALIGNMENT);
        c2.setAlignmentX(LEFT_ALIGNMENT);
        p.add(c1);
        p.add(Box.createVerticalStrut(12));
        p.add(c2);
        return p;
    }

    // ===================================================== rendez-vous et règlements

    private JComponent vueRendezVous() {
        LocalDateTime maintenant = LocalDateTime.now();
        ListeTableModel<Rendez_Vous> rdvs = new ListeTableModel<>("Date", "Heure", "Médecin", "Motif", "État");
        rdvs.setDonnees(historique.getRendezVous());
        JTable tableRdv = tableau(rdvs, (l, r, col) -> {
            switch (col) {
                case 0:
                    l.setText(r.getDate().format(DATE));
                    break;
                case 1:
                    l.setText(r.getHeure().format(HEURE));
                    break;
                case 2:
                    l.setText(historique.nomMedecin(r.getIdMedecin()).trim());
                    break;
                case 3:
                    l.setText(nonNul(r.getMotif()));
                    break;
                default:
                    StatutRendezVous s = StatutRendezVous.de(r, null, maintenant);
                    l.setText(s.getLibelle());
                    l.setIcon(new My_application.rendezvous.Pastille(s.getCouleur(), 10));
                    l.setForeground(s.getCouleur());
                    l.setFont(Theme.policeGras());
            }
        });
        largeur(tableRdv, 0, 100);
        largeur(tableRdv, 1, 70);
        JButton agenda = new JButton("Ouvrir l'agenda", Icones.svg("calendar-event", 16, Theme.PRIMAIRE));
        agenda.addActionListener(e -> actions.agenda());
        JPanel bas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bas.setOpaque(false);
        bas.add(agenda);
        JPanel gauche = new JPanel(new BorderLayout(0, 8));
        gauche.setOpaque(false);
        gauche.add(historique.getRendezVous().isEmpty() ? discret("Aucun rendez-vous.") : new JScrollPane(tableRdv),
                BorderLayout.CENTER);
        gauche.add(bas, BorderLayout.SOUTH);

        ListeTableModel<Passage> passages = new ListeTableModel<>("Date", "Arrivée", "Frais", "Réglé", "État");
        passages.setDonnees(historique.getPassages());
        JTable tablePassages = tableau(passages, (l, p, col) -> {
            switch (col) {
                case 0:
                    l.setText(p.getJour().format(DATE));
                    break;
                case 1:
                    l.setText(p.getArriveeLe() == null ? "" : p.getArriveeLe().format(HEURE));
                    break;
                case 2:
                    l.setText(p.getFrais() == null ? "—" : dinars(p.getFrais()));
                    break;
                case 3:
                    l.setText(p.getMontantRegle() == null ? "—" : dinars(p.getMontantRegle()));
                    break;
                default:
                    l.setText(etatPassage(p));
                    l.setForeground(couleurPassage(p));
                    l.setFont(Theme.policeGras());
            }
        });
        largeur(tablePassages, 0, 100);
        largeur(tablePassages, 1, 70);
        JPanel droite = new JPanel(new BorderLayout(0, 8));
        droite.setOpaque(false);
        droite.add(historique.getPassages().isEmpty() ? discret("Aucun passage en salle d'attente.")
                : new JScrollPane(tablePassages), BorderLayout.CENTER);
        droite.add(discret("Un passage par venue au cabinet : frais fixés par le médecin, montant encaissé "
                + "par la secrétaire."), BorderLayout.SOUTH);

        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false);
        p.add(carte("calendar-event", "Rendez-vous du patient", null, null, gauche));
        p.add(carte("cash", "Passages et règlements", null, null, droite));
        return p;
    }

    static String etatPassage(Passage p) {
        switch (p.getEtat()) {
            case Passage.EN_ATTENTE:
                return "En attente";
            case Passage.EN_CONSULTATION:
                return "En consultation";
            case Passage.A_REGLER:
                return "À régler";
            case Passage.PARTI:
                return "Parti sans consultation";
            default:
                int reste = HistoriquePatient.resteDu(p);
                if (reste > 0) {
                    return "Reste " + dinars(reste);
                }
                return p.getMontantRegle() != null ? "Réglé" : "Terminé";
        }
    }

    private static Color couleurPassage(Passage p) {
        switch (p.getEtat()) {
            case Passage.EN_ATTENTE:
            case Passage.EN_CONSULTATION:
            case Passage.A_REGLER:
                return Theme.AVERTISSEMENT;
            case Passage.PARTI:
                return Theme.TEXTE_SECONDAIRE;
            default:
                if (HistoriquePatient.resteDu(p) > 0) {
                    return Theme.DANGER;
                }
                return p.getMontantRegle() != null ? Theme.SUCCES : Theme.TEXTE_SECONDAIRE;
        }
    }

    private static Etiquette etiquetteReglement(Passage p) {
        Color c = couleurPassage(p);
        Color fond = c.equals(Theme.SUCCES) ? Theme.SUCCES_CLAIR : c.equals(Theme.DANGER) ? Theme.DANGER_CLAIR
                : Theme.FOND;
        return new Etiquette(etatPassage(p), c, fond);
    }

    /** « 1 500 DA ». */
    static String dinars(int montant) {
        return String.format(Locale.FRENCH, "%,d DA", montant).replace(' ', ' ').replace(' ', ' ');
    }

    // ===================================================== briques communes

    private static JPanel page() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.FOND);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        return p;
    }

    private static JScrollPane defilant(JComponent contenu) {
        JPanel haut = new Defilable();
        haut.add(contenu, BorderLayout.NORTH);
        JScrollPane s = new JScrollPane(haut, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.setBorder(null);
        s.getViewport().setBackground(Theme.FOND);
        s.getVerticalScrollBar().setUnitIncrement(16);
        return s;
    }

    /** Contenu qui prend la largeur de la zone de défilement et ne défile que verticalement. */
    private static final class Defilable extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        Defilable() {
            super(new BorderLayout());
            setOpaque(false);
        }

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

    private static void remplir(JPanel page, JComponent contenu) {
        page.removeAll();
        page.add(contenu, BorderLayout.CENTER);
        page.revalidate();
        page.repaint();
    }

    private static JPanel colonne(JComponent... cartes) {
        JPanel c = new JPanel();
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        for (JComponent carte : cartes) {
            carte.setAlignmentX(LEFT_ALIGNMENT);
            c.add(carte);
            c.add(Box.createVerticalStrut(12));
        }
        JPanel haut = new JPanel(new BorderLayout());
        haut.setOpaque(false);
        haut.add(c, BorderLayout.NORTH);
        return haut;
    }

    /** Carte blanche avec un en-tête : pictogramme, titre et, à droite, un lien. */
    static JComponent carte(String icone, String titre, String lien, Runnable action, JComponent contenu) {
        JLabel t = new JLabel(titre, Icones.svg(icone, 18, Theme.PRIMAIRE), JLabel.LEFT);
        t.setIconTextGap(8);
        t.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE + 1));
        t.setForeground(Theme.TEXTE);
        JPanel tete = new JPanel(new BorderLayout());
        tete.setOpaque(false);
        tete.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        tete.add(t, BorderLayout.CENTER);
        if (lien != null && action != null) {
            JLabel l = new JLabel(lien);
            l.setFont(Theme.police(Font.BOLD, Theme.TAILLE_BASE - 1));
            l.setForeground(Theme.PRIMAIRE);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
            tete.add(l, BorderLayout.EAST);
        }
        JPanel carte = new JPanel(new BorderLayout());
        Theme.carte(carte, null);
        carte.setBorder(BorderFactory.createCompoundBorder(carte.getBorder(),
                BorderFactory.createEmptyBorder(2, 4, 4, 4)));
        carte.add(tete, BorderLayout.NORTH);
        carte.add(contenu, BorderLayout.CENTER);
        return carte;
    }

    /** Grille « libellé : valeur » des informations personnelles. */
    private static final class Grille extends JPanel {

        private static final long serialVersionUID = 1L;
        private int ligne;

        Grille() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        void ligne(String libelle, String valeur) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridy = ligne++;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(3, 0, 3, 16);
            JLabel l = new JLabel(libelle);
            l.setFont(Theme.police());
            l.setForeground(Theme.TEXTE_SECONDAIRE);
            add(l, g);
            g.gridx = 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.insets = new Insets(3, 0, 3, 0);
            JLabel v = new JLabel(tiret(valeur));
            v.setFont(Theme.police());
            v.setForeground(Theme.TEXTE);
            add(v, g);
        }
    }

    /** Liste de lignes « date · titre / détail · élément à droite », cliquables. */
    private static final class Liste extends JPanel {

        private static final long serialVersionUID = 1L;

        Liste() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        void ligne(String date, String titre, String detail, JComponent droite, Runnable clic) {
            JPanel l = new JPanel(new BorderLayout(12, 0));
            l.setOpaque(false);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(getComponentCount() == 0 ? 0 : 1, 0, 0, 0, Theme.BORDURE),
                    BorderFactory.createEmptyBorder(7, 0, 7, 0)));
            JLabel d = new JLabel(date);
            d.setFont(Theme.police(Font.PLAIN, Theme.TAILLE_BASE - 1));
            d.setForeground(Theme.TEXTE_SECONDAIRE);
            d.setPreferredSize(new Dimension(78, d.getPreferredSize().height));
            l.add(d, BorderLayout.WEST);
            l.add(deuxLignes(titre, detail), BorderLayout.CENTER);
            JPanel droiteCentree = new JPanel(new GridBagLayout());
            droiteCentree.setOpaque(false);
            droiteCentree.add(droite);
            l.add(droiteCentree, BorderLayout.EAST);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    clic.run();
                }
            });
            l.setAlignmentX(LEFT_ALIGNMENT);
            add(l);
        }
    }

    private static JComponent deuxLignes(String titre, String detail) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(titre);
        t.setFont(Theme.policeGras());
        t.setForeground(Theme.TEXTE);
        JLabel d = new JLabel(detail == null || detail.isEmpty() ? " " : detail);
        d.setFont(Theme.police(Font.PLAIN, Theme.TAILLE_BASE - 1));
        d.setForeground(Theme.TEXTE_SECONDAIRE);
        d.setToolTipText(detail);
        // Une ligne trop longue est coupée au lieu d'élargir la colonne.
        d.setMinimumSize(new Dimension(10, d.getPreferredSize().height));
        d.setPreferredSize(new Dimension(10, d.getPreferredSize().height));
        p.add(t);
        p.add(d);
        return p;
    }

    private static JLabel chevron() {
        return new JLabel(Icones.svg("chevron-right", 16, Theme.TEXTE_SECONDAIRE));
    }

    private static JLabel discret(String texte) {
        JLabel l = new JLabel(texte);
        l.setFont(Theme.police(Font.PLAIN, Theme.TAILLE_BASE - 1));
        l.setForeground(Theme.TEXTE_SECONDAIRE);
        l.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        return l;
    }

    private static JLabel centre(String texte, Font police, Color couleur) {
        JLabel l = new JLabel(texte, SwingConstants.CENTER);
        l.setFont(police);
        l.setForeground(couleur);
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private static Etiquette etiquetteFrequence(String texte, int nombre) {
        Color c = couleurFrequence(nombre);
        Color fond = c.equals(Theme.DANGER) ? Theme.DANGER_CLAIR
                : c.equals(Theme.AVERTISSEMENT) ? Theme.AVERTISSEMENT_CLAIR : Theme.PRIMAIRE_CLAIR;
        return new Etiquette(texte, c, fond);
    }

    /** Une pathologie qui revient souvent ressort : rouge à partir de 4 fois, orange à partir de 2. */
    private static Color couleurFrequence(int nombre) {
        if (nombre >= 4) {
            return Theme.DANGER;
        }
        return nombre >= 2 ? Theme.AVERTISSEMENT : Theme.PRIMAIRE_FONCE;
    }

    private static String fois(int n) {
        return n + " fois";
    }

    /** Ce que chaque cellule d'un tableau montre de sa ligne. */
    private interface Cellule<T> {
        void remplir(JLabel cellule, T element, int colonne);
    }

    private static <T> JTable tableau(ListeTableModel<T> modele, Cellule<T> cellule) {
        JTable table = new JTable(modele);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object valeur, boolean choisie, boolean focus,
                    int ligne, int colonne) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, "", choisie, false, ligne, colonne);
                Theme.cellule(l, t, choisie, ligne);
                l.setIcon(null);
                l.setHorizontalAlignment(LEFT);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                cellule.remplir(l, modele.getElement(t.convertRowIndexToModel(ligne)), colonne);
                if (choisie) {
                    l.setForeground(t.getSelectionForeground());
                }
                return l;
            }
        });
        return table;
    }

    /** Largeur fixe d'une colonne (dates, noms) : les autres se partagent le reste. */
    private static void largeur(JTable table, int colonne, int pixels) {
        table.getColumnModel().getColumn(colonne).setMinWidth(pixels);
        table.getColumnModel().getColumn(colonne).setMaxWidth(pixels);
    }

    private static <T> T selection(JTable table, ListeTableModel<T> modele) {
        int ligne = table.getSelectedRow();
        return ligne < 0 ? null : modele.getElement(table.convertRowIndexToModel(ligne));
    }

    private MouseAdapter doubleClic(JTable table, ListeTableModel<consultation> modele) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                consultation c = selection(table, modele);
                if (e.getClickCount() == 2 && c != null) {
                    montrerConsultation(c);
                }
            }
        };
    }

    private static DocumentListener surChangement(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        };
    }

    static String tiret(String s) {
        return s == null || s.trim().isEmpty() ? "—" : s.trim();
    }

    private static String nonNul(String s) {
        return s == null ? "" : s;
    }

    static String joindre(String... parties) {
        List<String> l = new ArrayList<>();
        for (String s : parties) {
            if (s != null && !s.trim().isEmpty() && !l.contains(s.trim())) {
                l.add(s.trim());
            }
        }
        return String.join(", ", l);
    }

    private static String premiereMajuscule(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.FRENCH) + s.substring(1);
    }

    static String echapper(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
