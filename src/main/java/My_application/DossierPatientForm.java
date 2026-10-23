
package My_application;

import My_application.vue.EnTetePage;
import My_application.Analyse.AnalyseForm;
import My_application.Analyse.PaneauAnalyse;
import My_application.Ordonnance.OrdonnanceForm;
import My_application.Ordonnance.PaneauOrdonnance;
import My_application.Ordonnance.SaisieOrdonnance;
import My_application.controller.DossierPatientController.Constante;
import My_application.controller.DossierPatientController;
import My_application.controller.Vue;
import My_application.dossier.BandeauIdentite;
import My_application.dossier.Etiquette;
import My_application.dossier.OngletsDossier;
import My_application.entities.Antecedent;
import My_application.entities.DossierPatient;
import My_application.entities.NoteDossier;
import My_application.entities.consultation;
import My_application.entities.patient;
import My_application.entities.pathologie;
import My_application.entities.symptome;
import My_application.Patient.FichePatient;
import My_application.service.Posologie;
import My_application.vue.AutoCompletion;
import My_application.vue.Dialogues;
import My_application.vue.FiltreSaisie;
import My_application.vue.Formulaire;
import My_application.vue.Icones;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * Dossier médical d'un patient (Vue).
 * <p>
 * La fenêtre ne contient plus de SQL : les recherches de symptômes et
 * pathologies, l'enregistrement (transactionnel) et l'impression passent par
 * {@link DossierPatientController}. Elle reçoit un dossier déjà chargé.
 * <p>
 * Sous les boutons du haut : un bandeau d'identité, puis les onglets du
 * dossier ({@link OngletsDossier}). Les champs de saisie d'une consultation,
 * dessinés avec l'éditeur de NetBeans, sont replacés dans des cartes
 * ({@link #presenterSaisie()}) dans l'onglet « Consultations », à côté de la
 * liste des visites qui remplace l'arbre de l'historique et les flèches de
 * navigation.
 */
public class DossierPatientForm extends javax.swing.JFrame implements Vue {

    private static final String ONGLET_ORDONNANCE = "Ordonnance";
    private static final String ONGLET_ANALYSES = "Analyses";
    private static final String AVEC_TRAITEMENTS = "traitements";
    private static final String TRAITEMENTS = "Traitements";
    /** Sous-onglet « Diagnostics » de l'onglet « Consultations ». */
    private static final int DIAGNOSTICS = 0;
    private static final String SANS_TRAITEMENT = "aucun";
    private static final DateTimeFormatter JOUR_CONSULTATION = DateTimeFormatter.ofPattern(
            "EEEE dd/MM/yyyy 'à' ", Locale.FRENCH);

    private final DossierPatientController controleur;
    private final DossierPatient dossier;
    private final EnTetePage entete;
    private final BandeauIdentite bandeau;
    private final OngletsDossier onglets;
    /** Onglet « Consultations » : la saisie de la consultation, en cartes. */
    private final JPanel saisie;
    private final JLabel titreConsultation = new JLabel();
    private final JLabel detailConsultation = new JLabel();
    private final JPanel etatConsultation = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private final JPanel traitements = new JPanel(new CardLayout());
    /** Sous-onglets de la consultation : « Diagnostics » et « Traitements ». */
    private final javax.swing.JTabbedPane sousOnglets = new javax.swing.JTabbedPane();
    /** Enregistre la consultation en cours sans fermer le dossier. */
    private final JButton enregistrerBtn = new JButton("Enregistrer");

    /** Consultation en cours de saisie (null si aucune). */
    consultation consult = null;
    PaneauAnalyse Panalyse = null;
    PaneauOrdonnance Pordonnance = null;
    /** Saisie de l'ordonnance de la consultation en cours (null pour une consultation enregistrée). */
    private SaisieOrdonnance saisieOrdo = null;
    /** Allergies notées dans le dossier, rappelées pendant la saisie de l'ordonnance. */
    private List<String> allergiesDossier = new ArrayList<>();
    /** Vrai si l'utilisateur a choisi l'onglet Analyses (conservé d'une consultation à l'autre). */
    private boolean analysesChoisies = false;

    DefaultComboBoxModel BoxSympmodel = new DefaultComboBoxModel();
    DefaultComboBoxModel BoxPathomodel = new DefaultComboBoxModel();

    private final AppContext ctx;
    /** Montre la page « Rendez-vous » de la fenêtre principale pour ce patient. */
    private final Consumer<patient> rendezVous;

    public DossierPatientForm(DossierPatient doss, AppContext ctx, Consumer<patient> rendezVous) {
        this.ctx = ctx;
        this.rendezVous = rendezVous;
        initComponents();
        Theme.harmoniser(this);
        entete = EnTetePage.installer(this, "folder-open", "Dossier patient", sousTitre(doss.getMalade()));
        presenterActionsDossier();
        installerFiltres();
        masquerFonctionsAbsentes();
        saisie = presenterSaisie();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.photo.setIcon(Icones.patient(doss.getMalade(), 128));
        dossier = doss;
        controleur = new DossierPatientController(this, doss, ctx.getDossierService(),
                ctx.getCatalogueService(), ctx.getRapportService());

        int nombreConsult = dossier.getNombreConsultation();
        if (nombreConsult > 0) {
            nextBtn.setEnabled(false);
            VoirConsultation(dossier.LastConsultation());
        } else {
            desactiverVueConsultation();
            this.MedcinLabel.setText(dossier.getMedTraitant().getNomComplet());
            enTeteConsultation(null);
        }
        this.NbreVisiteLabel.setText(String.valueOf(nombreConsult));
        remplirInfo();
        configCombo();

        bandeau = new BandeauIdentite(this::modifierFiche);
        onglets = new OngletsDossier(dossier, saisie, new ActionsOnglets());
        presenterOnglets();
        onglets.setHistorique(controleur.historique());
        rechargerDossierClinique();
        onglets.rafraichirVisites();
        majBoutonsConsultation();
    }

    private static String sousTitre(patient p) {
        return p.getNom().toUpperCase() + " " + p.getPrenom() + "  ·  " + p.getStringAge();
    }

    /**
     * Remplace les deux colonnes dessinées dans NetBeans (informations et
     * historique à gauche, consultation à droite) par le bandeau d'identité
     * et les onglets. Le panneau de la consultation est repris tel quel dans
     * l'onglet « Consultations », sans ses flèches de navigation.
     */
    private void presenterOnglets() {
        Container centre = jPanel5.getParent();
        centre.removeAll();
        centre.setLayout(new BorderLayout(0, 10));
        centre.setBackground(Theme.FOND);
        if (centre instanceof JComponent) {
            ((JComponent) centre).setBorder(BorderFactory.createEmptyBorder(0, 14, 12, 14));
        }
        centre.add(bandeau, BorderLayout.NORTH);
        centre.add(onglets, BorderLayout.CENTER);
        jPanel7.setVisible(false);
    }

    /**
     * Onglet « Consultations » présenté comme le reste du dossier : une carte
     * d'en-tête (date, médecin, état), puis deux sous-onglets : « Diagnostics »
     * (paramètres vitaux, symptômes, pathologies côte à côte) et
     * « Traitements » (ordonnance et analyses avec leurs boutons). Les champs, listes et boutons sont ceux dessinés dans NetBeans
     * (le {@code .form} ne change pas), seulement replacés et restylés.
     */
    private JPanel presenterSaisie() {
        JPanel haut = new JPanel(new GridBagLayout());
        haut.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.weightx = 0.8;
        g.insets = new java.awt.Insets(0, 0, 0, 12);
        haut.add(carteParametresVitaux(), g);
        g.weightx = 1;
        haut.add(carteListe("report-medical", "Symptômes", symptomeCombo, "Choisir ou saisir un symptôme…",
                addSympBtn, suppSympBtn, jScrollPane2, SymptomeList), g);
        g.insets = new java.awt.Insets(0, 0, 0, 0);
        haut.add(carteListe("clipboard-list", "Pathologies", pathologieCombo, "Choisir ou saisir une pathologie…",
                addPathoBtn, suppPathoBtn, jScrollPane6, PathologieList), g);

        sousOnglets.setFont(Theme.police(java.awt.Font.BOLD, Theme.TAILLE_BASE));
        sousOnglets.addTab("Diagnostics", Icones.svg("heartbeat", 16, Theme.PRIMAIRE), sousOnglet(haut));
        sousOnglets.addTab(TRAITEMENTS, Icones.svg("pill", 16, Theme.PRIMAIRE), sousOnglet(carteTraitements()));
        sousOnglets.setToolTipTextAt(DIAGNOSTICS, "Paramètres vitaux, symptômes et pathologies");
        sousOnglets.setToolTipTextAt(1, "Ordonnance et analyses de la consultation");

        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);
        p.add(carteEnTete(), BorderLayout.NORTH);
        p.add(sousOnglets, BorderLayout.CENTER);
        return p;
    }

    private static JPanel sousOnglet(JComponent contenu) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.FOND);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        p.add(contenu, BorderLayout.CENTER);
        return p;
    }

    /** Date et heure de la consultation, médecin, et si elle est en cours de saisie ou enregistrée. */
    private JComponent carteEnTete() {
        titreConsultation.setIcon(Icones.svg("stethoscope", 22, Theme.PRIMAIRE));
        titreConsultation.setIconTextGap(10);
        titreConsultation.setFont(Theme.police(java.awt.Font.BOLD, Theme.TAILLE_BASE + 3));
        titreConsultation.setForeground(Theme.TEXTE);
        detailConsultation.setFont(Theme.police());
        detailConsultation.setForeground(Theme.TEXTE_SECONDAIRE);
        detailConsultation.setBorder(BorderFactory.createEmptyBorder(2, 32, 0, 0));
        JPanel textes = new JPanel(new BorderLayout());
        textes.setOpaque(false);
        textes.add(titreConsultation, BorderLayout.NORTH);
        textes.add(detailConsultation, BorderLayout.CENTER);
        etatConsultation.setOpaque(false);
        JPanel etat = new JPanel(new GridBagLayout());
        etat.setOpaque(false);
        etat.add(etatConsultation);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new java.awt.Insets(0, 12, 0, 0);
        etat.add(NewConsultBtn, g);
        etat.add(enregistrerBtn, g);
        JPanel carte = new JPanel(new BorderLayout(12, 0));
        Theme.carte(carte, null);
        carte.setBorder(BorderFactory.createCompoundBorder(carte.getBorder(),
                BorderFactory.createEmptyBorder(0, 4, 0, 4)));
        carte.add(textes, BorderLayout.CENTER);
        carte.add(etat, BorderLayout.EAST);
        return carte;
    }

    private JComponent carteParametresVitaux() {
        JPanel grille = new JPanel(new GridBagLayout());
        grille.setOpaque(false);
        constante(grille, 0, jLabel1, "Température (°C)", TempTxt);
        constante(grille, 1, jLabel6, "Pouls (bpm)", pulsationTxt);
        constante(grille, 2, jLabel4, "Tension (mmHg)", pressionTxt);
        constante(grille, 3, jLabel7, "Respiration (/min)", respirationTxt);
        constante(grille, 4, jLabel3, "Poids (kg)", poidsTxt);
        JPanel haut = new JPanel(new BorderLayout());
        haut.setOpaque(false);
        haut.add(grille, BorderLayout.NORTH);
        return OngletsDossier.carte("heartbeat", "Paramètres vitaux", (JComponent) null, haut);
    }

    private static void constante(JPanel grille, int ligne, JLabel libelle, String texte, JTextField champ) {
        libelle.setText(texte);
        libelle.setFont(Theme.police());
        libelle.setForeground(Theme.TEXTE_SECONDAIRE);
        champ.setFont(Theme.police(java.awt.Font.BOLD, Theme.TAILLE_BASE));
        champ.setColumns(7);
        champ.setHorizontalAlignment(JTextField.CENTER);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = ligne;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        g.insets = new java.awt.Insets(3, 0, 3, 12);
        grille.add(libelle, g);
        g.gridx = 1;
        g.weightx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new java.awt.Insets(3, 0, 3, 0);
        grille.add(champ, g);
    }

    /** Carte « Symptômes » ou « Pathologies » : liste déroulante à saisie, boutons ajouter / retirer, liste. */
    private static JComponent carteListe(String icone, String titre, JComboBox<String> combo, String invite,
            JButton ajouter, JButton retirer, JScrollPane defile, javax.swing.JList<String> liste) {
        combo.putClientProperty("JTextField.placeholderText", invite);
        combo.setPreferredSize(null);
        combo.setMinimumSize(new java.awt.Dimension(80, 30));
        boutonListe(ajouter, "circle-plus", "Ajouter à la consultation");
        boutonListe(retirer, "circle-minus", "Retirer l'élément choisi dans la liste");
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        boutons.setOpaque(false);
        boutons.add(ajouter);
        boutons.add(retirer);
        JPanel saisie = new JPanel(new BorderLayout(4, 0));
        saisie.setOpaque(false);
        saisie.add(combo, BorderLayout.CENTER);
        saisie.add(boutons, BorderLayout.EAST);

        liste.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        liste.setFont(Theme.police());
        liste.setPreferredSize(null);
        defile.setBorder(BorderFactory.createLineBorder(Theme.BORDURE));
        defile.setPreferredSize(new java.awt.Dimension(120, 60));
        JPanel contenu = new JPanel(new BorderLayout(0, 8));
        contenu.setOpaque(false);
        contenu.add(saisie, BorderLayout.NORTH);
        contenu.add(defile, BorderLayout.CENTER);
        return OngletsDossier.carte(icone, titre, (JComponent) null, contenu);
    }

    private static void boutonListe(JButton b, String icone, String bulle) {
        b.setIcon(Icones.svg(icone, 20, Theme.PRIMAIRE));
        b.setDisabledIcon(null);
        b.setToolTipText(bulle);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setPreferredSize(new java.awt.Dimension(30, 30));
    }

    /**
     * Ordonnance et analyses de la consultation dans des onglets, avec en
     * haut les boutons pour les établir et en bas aperçu, impression et
     * suppression du document affiché.
     */
    private JComponent carteTraitements() {
        boutonTraitement(addOrdo, "prescription", "Ordonnance", "Établir l'ordonnance de la consultation");
        boutonTraitement(addAnal, "flask", "Analyses", "Demander des analyses biologiques");
        JPanel etablir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        etablir.setOpaque(false);
        etablir.add(addOrdo);
        etablir.add(addAnal);

        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.TOP);
        jTabbedPane1.setBorder(null);
        jTabbedPane1.setFont(Theme.police(java.awt.Font.BOLD, Theme.TAILLE_BASE));
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(400, 180));
        jTabbedPane1.setBackground(Theme.SURFACE);
        JLabel aucun = new JLabel("<html><div style='text-align:center'>Pas d'ordonnance ni d'analyses pour "
                + "cette consultation.<br>Boutons « Ordonnance » et « Analyses » ci-dessus pour les établir."
                + "</div></html>", JLabel.CENTER);
        aucun.setFont(Theme.police());
        aucun.setForeground(Theme.TEXTE_SECONDAIRE);
        traitements.setOpaque(false);
        traitements.add(jTabbedPane1, AVEC_TRAITEMENTS);
        traitements.add(aucun, SANS_TRAITEMENT);
        jTabbedPane1.addContainerListener(new java.awt.event.ContainerAdapter() {
            @Override
            public void componentAdded(java.awt.event.ContainerEvent e) {
                majTraitements();
            }

            @Override
            public void componentRemoved(java.awt.event.ContainerEvent e) {
                majTraitements();
            }
        });
        majTraitements();

        boutonTraitement(visualiserBtn, "eye", "Aperçu", "Aperçu du document affiché (ordonnance ou analyses)");
        boutonTraitement(imprimerBtn, "printer", "Imprimer", "Imprimer le document affiché");
        boutonTraitement(supprimerBtn, "trash", "Supprimer", "Retirer de la consultation le document affiché");
        supprimerBtn.setIcon(Icones.svg("trash", 16, Theme.DANGER));
        supprimerBtn.setForeground(Theme.DANGER);
        JPanel documents = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        documents.setOpaque(false);
        documents.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        documents.add(visualiserBtn);
        documents.add(imprimerBtn);
        documents.add(supprimerBtn);

        JPanel contenu = new JPanel(new BorderLayout());
        contenu.setOpaque(false);
        contenu.add(traitements, BorderLayout.CENTER);
        contenu.add(documents, BorderLayout.SOUTH);
        return OngletsDossier.carte("prescription", "Ordonnance et analyses", etablir, contenu);
    }

    private static void boutonTraitement(JButton b, String icone, String texte, String bulle) {
        b.setText(texte);
        b.setIcon(Icones.svg(icone, 16, Theme.PRIMAIRE));
        b.setDisabledIcon(null);
        b.setToolTipText(bulle);
        b.setFont(Theme.police(java.awt.Font.BOLD, Theme.TAILLE_BASE));
        b.setPreferredSize(null);
        b.setMaximumSize(null);
        b.setIconTextGap(8);
        // Bouton à bord, comme « Aperçu » et « Imprimer » de l'onglet Ordonnances
        // (le thème avait fait des boutons à icône seule des boutons plats).
        b.putClientProperty("JButton.buttonType", null);
        b.setFocusable(true);
        b.setBorder(UIManager.getBorder("Button.border"));
        b.setBackground(UIManager.getColor("Button.background"));
    }

    /** Les onglets ordonnance / analyses, ou une phrase quand la consultation n'en a pas. */
    private void majTraitements() {
        int onglets = jTabbedPane1.getTabCount();
        ((CardLayout) traitements.getLayout()).show(traitements, onglets > 0 ? AVEC_TRAITEMENTS : SANS_TRAITEMENT);
        // L'onglet de saisie est toujours là pendant la consultation : il ne compte que s'il a des médicaments.
        int n = onglets - (saisieOrdo != null && jTabbedPane1.indexOfComponent(saisieOrdo) >= 0
                && consult != null && !consult.hasOrdonnance() ? 1 : 0);
        if (sousOnglets.getTabCount() > 1) {
            sousOnglets.setTitleAt(1, n > 0 ? TRAITEMENTS + " (" + n + ")" : TRAITEMENTS);
        }
    }

    /** En-tête de l'onglet « Consultations » ; {@code c} null : le dossier n'a encore aucune consultation. */
    private void enTeteConsultation(consultation c) {
        etatConsultation.removeAll();
        if (c == null) {
            titreConsultation.setText("Aucune consultation");
            detailConsultation.setText("« Nouvelle consultation », à droite, pour commencer la première visite.");
        } else {
            String jour = c.getDate_cons().format(JOUR_CONSULTATION);
            titreConsultation.setText("Consultation du " + jour + c.getHeureString());
            detailConsultation.setText(c.getMedecin().getNomComplet().trim());
            Etiquette etat = c == consult
                    ? new Etiquette("En cours de saisie", Theme.AVERTISSEMENT, Theme.AVERTISSEMENT_CLAIR)
                    : new Etiquette("Enregistrée · lecture seule", Theme.TEXTE_SECONDAIRE, Theme.LIGNE_ALTERNEE);
            if (c != consult) {
                etat.setIcon(Icones.svg("lock", 14, Theme.TEXTE_SECONDAIRE));
            }
            etatConsultation.add(etat);
        }
        etatConsultation.revalidate();
        etatConsultation.repaint();
    }

    /** Ce que les onglets du dossier demandent à la fenêtre. */
    private final class ActionsOnglets implements OngletsDossier.Actions {

        @Override
        public void voirConsultation(consultation c) {
            int index = dossier.getConsultations().indexOf(c);
            if (index < 0 || !lireConstantes()) {
                return;
            }
            dossier.SetPosition(index);
            VoirConsultation(dossier.getCurrentConsultation());
            majNavigation();
        }

        @Override
        public void ordonnance(consultation c, boolean imprimer) {
            controleur.ordonnance(c, imprimer);
        }

        @Override
        public void analyses(consultation c, boolean imprimer) {
            controleur.analyses(c, imprimer);
        }

        @Override
        public void agenda() {
            rendezVous.accept(dossier.getMalade());
        }

        @Override
        public void modifierFiche() {
            DossierPatientForm.this.modifierFiche();
        }

        @Override
        public void ajouterAntecedent() {
            JComboBox<Antecedent.Categorie> rubrique = new JComboBox<>(Antecedent.Categorie.values());
            JTextField libelle = new JTextField(28);
            libelle.putClientProperty("JTextField.placeholderText", "Pénicilline, appendicectomie, diabète du père…");
            JTextField complement = new JTextField(28);
            complement.putClientProperty("JTextField.placeholderText", "Facultatif : date, réaction, dosage…");
            Formulaire f = new Formulaire(DossierPatientForm.this, "Nouvel antécédent", "clipboard-list",
                    "Noté tout de suite dans le dossier, sans attendre sa fermeture.", "Ajouter")
                    .ligne("Rubrique", rubrique)
                    .ligne("Antécédent", libelle)
                    .ligne("Précision", complement);
            if (f.afficher(() -> controleur.ajouterAntecedent((Antecedent.Categorie) rubrique.getSelectedItem(),
                    libelle.getText(), complement.getText()))) {
                rechargerDossierClinique();
            }
        }

        @Override
        public void supprimerAntecedent(Antecedent a) {
            if (Dialogues.confirmer(DossierPatientForm.this, "Retirer « " + a + " » des antécédents ?")
                    && controleur.supprimerAntecedent(a)) {
                rechargerDossierClinique();
            }
        }

        @Override
        public void ajouterNote() {
            JTextArea texte = new JTextArea(8, 40);
            texte.setLineWrap(true);
            texte.setWrapStyleWord(true);
            Formulaire f = new Formulaire(DossierPatientForm.this, "Nouvelle note", "pencil",
                    "Note datée, à votre nom, enregistrée tout de suite dans le dossier.", "Enregistrer")
                    .ligne(null, new JScrollPane(texte));
            if (f.afficher(() -> controleur.ajouterNote(texte.getText()))) {
                rechargerDossierClinique();
            }
        }

        @Override
        public void supprimerNote(NoteDossier n) {
            if (Dialogues.confirmer(DossierPatientForm.this, "Supprimer cette note ?") && controleur.supprimerNote(n)) {
                rechargerDossierClinique();
            }
        }
    }

    /** Relit antécédents et notes, et les allergies du bandeau. */
    private void rechargerDossierClinique() {
        List<Antecedent> antecedents = controleur.antecedents();
        onglets.setDossierClinique(antecedents, controleur.notes());
        afficherBandeau(antecedents);
    }

    private void afficherBandeau(List<Antecedent> antecedents) {
        List<String> allergies = new ArrayList<>();
        for (Antecedent a : antecedents) {
            if (a.getCategorie() == Antecedent.Categorie.ALLERGIE) {
                allergies.add(a.getLibelle());
            }
        }
        allergiesDossier = allergies;
        bandeau.afficher(dossier.getMalade(), onglets.getHistorique().passageEnCours(LocalDate.now()), allergies);
    }

    /** Fiche du patient (état civil, coordonnées) : le bandeau et la vue d'ensemble suivent les changements. */
    private void modifierFiche() {
        FichePatient fiche = new FichePatient(this, ctx.getPatientService(), dossier.getMalade());
        fiche.setVisible(true);
        patient modifie = fiche.getPatient();
        if (modifie != null) {
            dossier.setMalade(modifie);
            entete.setSousTitre(sousTitre(modifie));
            rechargerDossierClinique();
            remplirInfo();
            onglets.rafraichirOngletCourant();
        }
    }

    /**
     * Caractères permis dans les constantes vitales. Le filtre s'applique aussi
     * au collage, que les anciens contrôles {@code keyTyped} laissaient passer ;
     * les bornes sont vérifiées par le service ({@code ConstantesVitales}).
     */
    private void installerFiltres() {
        FiltreSaisie.installer(TempTxt, FiltreSaisie.decimal(6));
        FiltreSaisie.installer(pulsationTxt, FiltreSaisie.entier(3));
        FiltreSaisie.installer(pressionTxt, new FiltreSaisie("0-9/.,", 9)); // 12/8, 13,5/8, 120/80...
        pressionTxt.setToolTipText("12/8 (cmHg) ou 120/80 (mmHg) ; enregistrée et affichée en mmHg");
        FiltreSaisie.installer(respirationTxt, FiltreSaisie.entier(3));
        FiltreSaisie.installer(poidsTxt, FiltreSaisie.decimal(6));
    }

    /**
     * Imagerie et actes médicaux ne sont pas encore développés, et la barre de
     * menus ne contenait que des menus vides : on ne propose pas à l'utilisateur
     * des commandes qui ne font rien.
     */
    private void masquerFonctionsAbsentes() {
        addRadio.setVisible(false);
        addAct.setVisible(false);
        setJMenuBar(null);
    }

    /**
     * La rangée de boutons du haut (nouvelle consultation, rendez-vous, lettre
     * d'orientation, statistiques, fermeture) est retirée : « Nouvelle
     * consultation » passe dans l'en-tête de l'onglet « Consultations », le
     * rendez-vous est dans l'onglet « Rendez-vous et règlements », les
     * constantes ont leur onglet et la fenêtre se ferme par sa croix.
     */
    private void presenterActionsDossier() {
        jPanel9.removeAll();
        jPanel9.setLayout(new BorderLayout());
        jPanel9.add(jScrollPane1, BorderLayout.CENTER);
        actionDossier(NewConsultBtn, "stethoscope", true);
        actionDossier(enregistrerBtn, "device-floppy", true);
        enregistrerBtn.setToolTipText("Enregistrer la consultation en cours dans le dossier");
        enregistrerBtn.addActionListener(e -> enregistrerConsultation());
    }

    /**
     * « Nouvelle Consultation » et « Enregistrer » s'activent à tour de rôle :
     * le second pendant la saisie d'une consultation, le premier sinon. Le
     * bouton actif est plein, l'autre grisé.
     */
    private void majBoutonsConsultation() {
        boolean enSaisie = consult != null;
        boutonAlterne(NewConsultBtn, "stethoscope", !enSaisie);
        boutonAlterne(enregistrerBtn, "device-floppy", enSaisie);
    }

    private static void boutonAlterne(JButton b, String icone, boolean actif) {
        b.setEnabled(actif);
        b.setBackground(actif ? Theme.PRIMAIRE : UIManager.getColor("Button.background"));
        b.setForeground(actif ? java.awt.Color.WHITE : Theme.TEXTE);
        b.setIcon(Icones.svg(icone, 18, actif ? java.awt.Color.WHITE : Theme.PRIMAIRE));
        b.setDisabledIcon(null);
    }

    private static void actionDossier(JButton b, String icone, boolean principal) {
        b.setBorder(UIManager.getBorder("Button.border"));
        b.setFont(Theme.police(java.awt.Font.BOLD, Theme.TAILLE_BASE));
        b.setIconTextGap(10);
        if (principal) {
            b.setBackground(Theme.PRIMAIRE);
            b.setForeground(java.awt.Color.WHITE);
            b.setIcon(Icones.svg(icone, 18, java.awt.Color.WHITE));
        } else {
            b.setBackground(UIManager.getColor("Button.background"));
            b.setForeground(Theme.TEXTE);
            b.setIcon(Icones.svg(icone, 18, Theme.PRIMAIRE));
        }
        b.setPreferredSize(new java.awt.Dimension(b.getPreferredSize().width + 24, 38));
    }

    void remplirInfo() {
        numLabel.setText(dossier.getMalade().getNumeroDossier());
        nomLabel.setText(dossier.getMalade().getNom());
        prenomLabel.setText(dossier.getMalade().getPrenom());
        ageLabel.setText(dossier.getMalade().getStringAge());
    }

    @Override
    public void afficherErreur(String message) {
        Dialogues.erreur(this, message);
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel5 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        NbreVisiteLabel = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        historique = new javax.swing.JTree();
        jPanel3 = new javax.swing.JPanel();
        NewConsultBtn = new javax.swing.JButton();
        NewRendezVousBtn = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        FermerDossierBtn = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        jPanel20 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        numLabel = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        nomLabel = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        prenomLabel = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        ageLabel = new javax.swing.JLabel();
        jPanel21 = new javax.swing.JPanel();
        jPanel22 = new javax.swing.JPanel();
        photo = new javax.swing.JLabel();
        jPanel23 = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        jPanel24 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel28 = new javax.swing.JPanel();
        FirstBtn = new javax.swing.JButton();
        previousBtn = new javax.swing.JButton();
        dateText = new javax.swing.JTextField();
        TimeTxt = new javax.swing.JTextField();
        nextBtn = new javax.swing.JButton();
        LastBtn = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        consultPanel = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        TempTxt = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        pulsationTxt = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        pressionTxt = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        respirationTxt = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        poidsTxt = new javax.swing.JTextField();
        jPanel12 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        SymptomeList = new javax.swing.JList<>();
        jPanel14 = new javax.swing.JPanel();
        symptomeCombo = new javax.swing.JComboBox<>();
        jPanel25 = new javax.swing.JPanel();
        addSympBtn = new javax.swing.JButton();
        suppSympBtn = new javax.swing.JButton();
        jPanel15 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        PathologieList = new javax.swing.JList<>();
        jPanel16 = new javax.swing.JPanel();
        pathologieCombo = new javax.swing.JComboBox<>();
        jPanel26 = new javax.swing.JPanel();
        addPathoBtn = new javax.swing.JButton();
        suppPathoBtn = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        jPanel18 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel19 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        addOrdo = new javax.swing.JButton();
        addAnal = new javax.swing.JButton();
        addRadio = new javax.swing.JButton();
        addAct = new javax.swing.JButton();
        jPanel27 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        imprimerBtn = new javax.swing.JButton();
        visualiserBtn = new javax.swing.JButton();
        supprimerBtn = new javax.swing.JButton();
        jPanel17 = new javax.swing.JPanel();
        MedcinLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        FichierMn = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel5.setText("Nom:");
        jLabel5.setPreferredSize(new java.awt.Dimension(20, 20));

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.X_AXIS));

        jPanel5.setPreferredSize(new java.awt.Dimension(300, 530));
        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel11.setBackground(new java.awt.Color(204, 204, 255));
        jPanel11.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel11.setBackground(new java.awt.Color(153, 153, 255));
        jLabel11.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        jLabel11.setText("Nombre de visites :");
        jPanel11.add(jLabel11);

        NbreVisiteLabel.setBackground(new java.awt.Color(153, 153, 255));
        NbreVisiteLabel.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        NbreVisiteLabel.setForeground(new java.awt.Color(0, 102, 51));
        NbreVisiteLabel.setText("nombre");
        jPanel11.add(NbreVisiteLabel);

        jPanel5.add(jPanel11, java.awt.BorderLayout.PAGE_END);

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder("Historique des visites"));
        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.LINE_AXIS));

        jSplitPane2.setDividerLocation(350);

        jScrollPane1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        historique.setBackground(new java.awt.Color(204, 255, 255));
        historique.setFont(new java.awt.Font("Goudy Old Style", 1, 18)); // NOI18N
        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Visites");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Consultations");
        javax.swing.tree.DefaultMutableTreeNode treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Années");
        javax.swing.tree.DefaultMutableTreeNode treeNode4 = new javax.swing.tree.DefaultMutableTreeNode("Mois");
        javax.swing.tree.DefaultMutableTreeNode treeNode5 = new javax.swing.tree.DefaultMutableTreeNode("Jours");
        treeNode4.add(treeNode5);
        treeNode3.add(treeNode4);
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Rendez-vous");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Années");
        treeNode4 = new javax.swing.tree.DefaultMutableTreeNode("Mois");
        treeNode5 = new javax.swing.tree.DefaultMutableTreeNode("Jours");
        treeNode4.add(treeNode5);
        treeNode3.add(treeNode4);
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        historique.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        historique.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                historiqueMousePressed(evt);
            }
        });
        historique.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                historiqueKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(historique);

        jSplitPane2.setLeftComponent(jScrollPane1);

        jPanel3.setBackground(new java.awt.Color(0, 51, 51));
        jPanel3.setLayout(new java.awt.GridLayout(5, 1));

        NewConsultBtn.setBackground(new java.awt.Color(0, 38, 38));
        NewConsultBtn.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        NewConsultBtn.setForeground(new java.awt.Color(255, 255, 255));
        NewConsultBtn.setText("Nouvelle Consultation");
        NewConsultBtn.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        NewConsultBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewConsultBtnActionPerformed(evt);
            }
        });
        jPanel3.add(NewConsultBtn);

        NewRendezVousBtn.setBackground(new java.awt.Color(0, 38, 38));
        NewRendezVousBtn.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        NewRendezVousBtn.setForeground(new java.awt.Color(255, 255, 255));
        NewRendezVousBtn.setText("Nouveau Rendez-vous");
        NewRendezVousBtn.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        NewRendezVousBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewRendezVousBtnActionPerformed(evt);
            }
        });
        jPanel3.add(NewRendezVousBtn);

        jButton10.setBackground(new java.awt.Color(0, 38, 38));
        jButton10.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jButton10.setForeground(new java.awt.Color(255, 255, 255));
        jButton10.setText("Lettre d'Orientation");
        jButton10.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel3.add(jButton10);

        jButton2.setBackground(new java.awt.Color(0, 38, 38));
        jButton2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setText("Statistiques");
        jButton2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel3.add(jButton2);

        FermerDossierBtn.setBackground(new java.awt.Color(0, 38, 38));
        FermerDossierBtn.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        FermerDossierBtn.setForeground(new java.awt.Color(204, 51, 0));
        FermerDossierBtn.setText("Fermer Dossier");
        FermerDossierBtn.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        FermerDossierBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FermerDossierBtnActionPerformed(evt);
            }
        });
        jPanel3.add(FermerDossierBtn);

        jSplitPane2.setRightComponent(jPanel3);

        jPanel9.add(jSplitPane2);

        jPanel5.add(jPanel9, java.awt.BorderLayout.CENTER);

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("Informations du patient"));
        jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.LINE_AXIS));

        jPanel20.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
        jPanel20.setPreferredSize(new java.awt.Dimension(400, 130));
        jPanel20.setLayout(new java.awt.GridLayout(4, 2));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel2.setText("Num:");
        jLabel2.setAlignmentX(0.5F);
        jLabel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel2.setPreferredSize(new java.awt.Dimension(20, 20));
        jPanel20.add(jLabel2);

        numLabel.setFont(new java.awt.Font("Arial", 1, 15)); // NOI18N
        numLabel.setForeground(new java.awt.Color(102, 0, 0));
        numLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        numLabel.setText("01578");
        numLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        numLabel.setPreferredSize(new java.awt.Dimension(20, 20));
        jPanel20.add(numLabel);

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel15.setText("Nom:");
        jLabel15.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel15.setPreferredSize(new java.awt.Dimension(20, 20));
        jPanel20.add(jLabel15);

        nomLabel.setFont(new java.awt.Font("Arial", 1, 15)); // NOI18N
        nomLabel.setForeground(new java.awt.Color(102, 0, 0));
        nomLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        nomLabel.setText("SADOU");
        nomLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        nomLabel.setPreferredSize(new java.awt.Dimension(20, 20));
        jPanel20.add(nomLabel);

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText("Prénom:");
        jLabel8.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel8.setPreferredSize(new java.awt.Dimension(20, 20));
        jPanel20.add(jLabel8);

        prenomLabel.setFont(new java.awt.Font("Arial", 1, 15)); // NOI18N
        prenomLabel.setForeground(new java.awt.Color(102, 0, 0));
        prenomLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        prenomLabel.setText("SAMIR");
        prenomLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        prenomLabel.setPreferredSize(new java.awt.Dimension(20, 20));
        jPanel20.add(prenomLabel);

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel16.setText("Age:");
        jLabel16.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel16.setPreferredSize(new java.awt.Dimension(20, 20));
        jPanel20.add(jLabel16);

        ageLabel.setFont(new java.awt.Font("Arial", 1, 15)); // NOI18N
        ageLabel.setForeground(new java.awt.Color(102, 0, 0));
        ageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ageLabel.setText("35 ans");
        ageLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        ageLabel.setPreferredSize(new java.awt.Dimension(20, 20));
        jPanel20.add(ageLabel);

        jPanel10.add(jPanel20);

        jPanel21.setLayout(new java.awt.BorderLayout());

        jPanel22.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        photo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/h128.png"))); // NOI18N
        jPanel22.add(photo);

        jPanel21.add(jPanel22, java.awt.BorderLayout.CENTER);

        jPanel23.setPreferredSize(new java.awt.Dimension(150, 35));
        jPanel23.setLayout(new java.awt.BorderLayout());

        jButton5.setText("Détails");
        jPanel23.add(jButton5, java.awt.BorderLayout.CENTER);

        jPanel21.add(jPanel23, java.awt.BorderLayout.SOUTH);

        jPanel10.add(jPanel21);

        jPanel5.add(jPanel10, java.awt.BorderLayout.PAGE_START);

        getContentPane().add(jPanel5);

        jPanel24.setLayout(new java.awt.BorderLayout());

        jPanel7.setPreferredSize(new java.awt.Dimension(805, 60));

        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jPanel2.setPreferredSize(new java.awt.Dimension(430, 60));

        FirstBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/first.png"))); // NOI18N
        FirstBtn.setPreferredSize(new java.awt.Dimension(50, 50));
        FirstBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FirstBtnActionPerformed(evt);
            }
        });
        jPanel28.add(FirstBtn);

        previousBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/previous.png"))); // NOI18N
        previousBtn.setFocusable(false);
        previousBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        previousBtn.setPreferredSize(new java.awt.Dimension(48, 48));
        previousBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        previousBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousBtnActionPerformed(evt);
            }
        });
        jPanel28.add(previousBtn);

        dateText.setEditable(false);
        dateText.setBackground(new java.awt.Color(0, 0, 0));
        dateText.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        dateText.setForeground(new java.awt.Color(255, 0, 0));
        dateText.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        dateText.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        dateText.setPreferredSize(new java.awt.Dimension(220, 35));
        dateText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dateTextActionPerformed(evt);
            }
        });
        jPanel28.add(dateText);

        TimeTxt.setEditable(false);
        TimeTxt.setBackground(new java.awt.Color(0, 0, 0));
        TimeTxt.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        TimeTxt.setForeground(new java.awt.Color(255, 0, 0));
        TimeTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        TimeTxt.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        TimeTxt.setPreferredSize(new java.awt.Dimension(120, 35));
        jPanel28.add(TimeTxt);

        nextBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/next.png"))); // NOI18N
        nextBtn.setFocusable(false);
        nextBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nextBtn.setPreferredSize(new java.awt.Dimension(48, 48));
        nextBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        nextBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextBtnActionPerformed(evt);
            }
        });
        jPanel28.add(nextBtn);

        LastBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/last.png"))); // NOI18N
        LastBtn.setPreferredSize(new java.awt.Dimension(50, 50));
        LastBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LastBtnActionPerformed(evt);
            }
        });
        jPanel28.add(LastBtn);

        jPanel2.add(jPanel28);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 806, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jPanel24.add(jPanel7, java.awt.BorderLayout.SOUTH);

        jPanel6.setPreferredSize(new java.awt.Dimension(805, 709));
        jPanel6.setLayout(new java.awt.BorderLayout());

        jSplitPane1.setDividerLocation(250);
        jSplitPane1.setDividerSize(10);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jScrollPane4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jScrollPane4.setPreferredSize(new java.awt.Dimension(807, 190));

        consultPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Consultation"));
        consultPanel.setPreferredSize(new java.awt.Dimension(805, 120));
        consultPanel.setLayout(new javax.swing.BoxLayout(consultPanel, javax.swing.BoxLayout.X_AXIS));

        jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Paramètres Vitaux", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.ABOVE_TOP));
        jPanel13.setPreferredSize(new java.awt.Dimension(300, 120));
        jPanel13.setLayout(new java.awt.GridLayout(5, 2, 10, 5));

        jLabel1.setText("Température ");
        jPanel13.add(jLabel1);

        TempTxt.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        TempTxt.setToolTipText("# # . # # ");
        TempTxt.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                TempTxtFocusLost(evt);
            }
        });
        TempTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                TempTxtKeyTyped(evt);
            }
        });
        jPanel13.add(TempTxt);

        jLabel6.setText("Pulsations Cardiaques");
        jPanel13.add(jLabel6);

        pulsationTxt.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        pulsationTxt.setToolTipText("# # #");
        pulsationTxt.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                pulsationTxtFocusLost(evt);
            }
        });
        pulsationTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                pulsationTxtKeyTyped(evt);
            }
        });
        jPanel13.add(pulsationTxt);

        jLabel4.setText("Pression Artérielle");
        jPanel13.add(jLabel4);

        pressionTxt.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        pressionTxt.setToolTipText("# # / # #");
        pressionTxt.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                pressionTxtFocusLost(evt);
            }
        });
        pressionTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                pressionTxtKeyTyped(evt);
            }
        });
        jPanel13.add(pressionTxt);

        jLabel7.setText("Taux de respiration");
        jPanel13.add(jLabel7);

        respirationTxt.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        respirationTxt.setToolTipText("# # #");
        respirationTxt.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                respirationTxtFocusLost(evt);
            }
        });
        respirationTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                respirationTxtKeyTyped(evt);
            }
        });
        jPanel13.add(respirationTxt);

        jLabel3.setText("Poids");
        jPanel13.add(jLabel3);

        poidsTxt.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        poidsTxt.setToolTipText("# # # . # #");
        poidsTxt.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                poidsTxtFocusLost(evt);
            }
        });
        poidsTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                poidsTxtKeyTyped(evt);
            }
        });
        jPanel13.add(poidsTxt);

        consultPanel.add(jPanel13);

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder("Symptômes"));
        jPanel12.setPreferredSize(new java.awt.Dimension(250, 207));
        jPanel12.setLayout(new java.awt.BorderLayout());

        SymptomeList.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        SymptomeList.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        SymptomeList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(SymptomeList);

        jPanel12.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jPanel14.setPreferredSize(new java.awt.Dimension(245, 30));
        jPanel14.setLayout(new java.awt.BorderLayout());

        symptomeCombo.setEditable(true);
        symptomeCombo.setMinimumSize(new java.awt.Dimension(200, 30));
        symptomeCombo.setPreferredSize(new java.awt.Dimension(200, 30));
        jPanel14.add(symptomeCombo, java.awt.BorderLayout.CENTER);

        jPanel25.setMinimumSize(new java.awt.Dimension(35, 43));
        jPanel25.setPreferredSize(new java.awt.Dimension(60, 30));
        jPanel25.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));

        addSympBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/My_application/icons/addCombo.png"))); // NOI18N
        addSympBtn.setPreferredSize(new java.awt.Dimension(30, 30));
        addSympBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSympBtnActionPerformed(evt);
            }
        });
        jPanel25.add(addSympBtn);

        suppSympBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/supp24.png"))); // NOI18N
        suppSympBtn.setPreferredSize(new java.awt.Dimension(30, 30));
        suppSympBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                suppSympBtnActionPerformed(evt);
            }
        });
        jPanel25.add(suppSympBtn);

        jPanel14.add(jPanel25, java.awt.BorderLayout.EAST);

        jPanel12.add(jPanel14, java.awt.BorderLayout.NORTH);

        consultPanel.add(jPanel12);

        jPanel15.setBorder(javax.swing.BorderFactory.createTitledBorder("Pathologies"));
        jPanel15.setPreferredSize(new java.awt.Dimension(250, 70));
        jPanel15.setLayout(new java.awt.BorderLayout());

        PathologieList.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        PathologieList.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        PathologieList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        PathologieList.setPreferredSize(new java.awt.Dimension(235, 0));
        jScrollPane6.setViewportView(PathologieList);

        jPanel15.add(jScrollPane6, java.awt.BorderLayout.CENTER);

        jPanel16.setPreferredSize(new java.awt.Dimension(235, 30));
        jPanel16.setLayout(new java.awt.BorderLayout());

        pathologieCombo.setEditable(true);
        pathologieCombo.setMinimumSize(new java.awt.Dimension(123, 30));
        pathologieCombo.setPreferredSize(new java.awt.Dimension(200, 20));
        jPanel16.add(pathologieCombo, java.awt.BorderLayout.CENTER);

        jPanel26.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));

        addPathoBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/addCombo.png"))); // NOI18N
        addPathoBtn.setPreferredSize(new java.awt.Dimension(30, 30));
        addPathoBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPathoBtnActionPerformed(evt);
            }
        });
        jPanel26.add(addPathoBtn);

        suppPathoBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/supp24.png"))); // NOI18N
        suppPathoBtn.setPreferredSize(new java.awt.Dimension(30, 30));
        suppPathoBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                suppPathoBtnActionPerformed(evt);
            }
        });
        jPanel26.add(suppPathoBtn);

        jPanel16.add(jPanel26, java.awt.BorderLayout.EAST);

        jPanel15.add(jPanel16, java.awt.BorderLayout.NORTH);

        consultPanel.add(jPanel15);

        jScrollPane4.setViewportView(consultPanel);

        jSplitPane1.setTopComponent(jScrollPane4);

        jPanel18.setMinimumSize(new java.awt.Dimension(800, 500));
        jPanel18.setPreferredSize(new java.awt.Dimension(850, 350));
        jPanel18.setLayout(new java.awt.BorderLayout());

        jTabbedPane1.setBackground(new java.awt.Color(255, 255, 255));
        jTabbedPane1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.RIGHT);
        jTabbedPane1.setAlignmentX(2.0F);
        jTabbedPane1.setAlignmentY(2.0F);
        jTabbedPane1.setAutoscrolls(true);
        jTabbedPane1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTabbedPane1.setFont(new java.awt.Font("Arial Black", 1, 15)); // NOI18N
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(850, 300));
        jTabbedPane1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTabbedPane1MouseClicked(evt);
            }
        });
        jPanel18.add(jTabbedPane1, java.awt.BorderLayout.CENTER);
        jTabbedPane1.getAccessibleContext().setAccessibleName("                Analyses  ");

        jPanel19.setPreferredSize(new java.awt.Dimension(805, 5));

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 850, Short.MAX_VALUE)
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        jPanel18.add(jPanel19, java.awt.BorderLayout.SOUTH);

        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel4.setPreferredSize(new java.awt.Dimension(848, 110));
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("Traitements"));

        addOrdo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/ordon.png"))); // NOI18N
        addOrdo.setToolTipText("Etablir Ordonnance");
        addOrdo.setPreferredSize(new java.awt.Dimension(50, 50));
        addOrdo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addOrdoActionPerformed(evt);
            }
        });
        jPanel8.add(addOrdo);

        addAnal.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/analyse1.png"))); // NOI18N
        addAnal.setToolTipText("Demmander analyses biologiques");
        addAnal.setMaximumSize(new java.awt.Dimension(48, 48));
        addAnal.setPreferredSize(new java.awt.Dimension(50, 50));
        addAnal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAnalActionPerformed(evt);
            }
        });
        jPanel8.add(addAnal);

        addRadio.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/radio.png"))); // NOI18N
        addRadio.setToolTipText("Demander imagerie ");
        addRadio.setPreferredSize(new java.awt.Dimension(50, 50));
        addRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRadioActionPerformed(evt);
            }
        });
        jPanel8.add(addRadio);

        addAct.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/bistouri1.png"))); // NOI18N
        addAct.setToolTipText("Faire un act médical");
        addAct.setPreferredSize(new java.awt.Dimension(50, 50));
        addAct.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addActActionPerformed(evt);
            }
        });
        jPanel8.add(addAct);

        jPanel4.add(jPanel8);

        jPanel27.setPreferredSize(new java.awt.Dimension(400, 48));

        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jPanel27Layout.setVerticalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 48, Short.MAX_VALUE)
        );

        jPanel4.add(jPanel27);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Actions"));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        imprimerBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/My_application/icons/Print-icon.png"))); // NOI18N
        imprimerBtn.setToolTipText("Imprimer tout");
        imprimerBtn.setPreferredSize(new java.awt.Dimension(50, 50));
        imprimerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imprimerBtnActionPerformed(evt);
            }
        });
        jPanel1.add(imprimerBtn);

        visualiserBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/visualiser.png"))); // NOI18N
        visualiserBtn.setToolTipText("Visualiser");
        visualiserBtn.setPreferredSize(new java.awt.Dimension(50, 50));
        visualiserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                visualiserBtnActionPerformed(evt);
            }
        });
        jPanel1.add(visualiserBtn);

        supprimerBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/trash2.png"))); // NOI18N
        supprimerBtn.setToolTipText("Supprimer Traitement");
        supprimerBtn.setPreferredSize(new java.awt.Dimension(50, 50));
        supprimerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                supprimerBtnActionPerformed(evt);
            }
        });
        jPanel1.add(supprimerBtn);

        jPanel4.add(jPanel1);

        jPanel18.add(jPanel4, java.awt.BorderLayout.NORTH);

        jScrollPane5.setViewportView(jPanel18);

        jSplitPane1.setRightComponent(jScrollPane5);

        jPanel6.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jPanel24.add(jPanel6, java.awt.BorderLayout.CENTER);

        jPanel17.setBackground(new java.awt.Color(0, 0, 153));
        jPanel17.setForeground(new java.awt.Color(0, 0, 102));
        jPanel17.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 0));

        MedcinLabel.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        MedcinLabel.setForeground(new java.awt.Color(255, 255, 255));
        MedcinLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel17.add(MedcinLabel);

        jPanel24.add(jPanel17, java.awt.BorderLayout.PAGE_START);

        getContentPane().add(jPanel24);

        FichierMn.setText("Fichier");

        jMenuItem1.setText("jMenuItem1");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        FichierMn.add(jMenuItem1);

        jMenuBar1.add(FichierMn);

        jMenu2.setText("Patient");
        jMenuBar1.add(jMenu2);

        jMenu3.setText("Visite");
        jMenuBar1.add(jMenu3);

        jMenu4.setText("Traitement");
        jMenuBar1.add(jMenu4);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addOrdoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addOrdoActionPerformed
        if (consult == null) {
            return;
        }
        if (saisieOrdo != null && jTabbedPane1.indexOfComponent(saisieOrdo) >= 0) {
            jTabbedPane1.setSelectedComponent(saisieOrdo);
            saisieOrdo.commencer();
        } else if (jTabbedPane1.indexOfComponent(Pordonnance) < 0) {
            new OrdonnanceForm(consult, ctx.getCatalogueService()).setVisible(true);
            addOrdonnance(consult);
        } else {
            afficherInformation("Ordonnance déjà établie !");
        }
    }//GEN-LAST:event_addOrdoActionPerformed

    private void supprimerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_supprimerBtnActionPerformed
        int onglet = jTabbedPane1.getSelectedIndex();
        if (onglet >= 0 && consult != null) {
            if (saisieOrdo != null && onglet == jTabbedPane1.indexOfComponent(saisieOrdo)) {
                // L'onglet de saisie reste : on vide seulement l'ordonnance.
                if (consult.hasOrdonnance() && Dialogues.confirmer(this, "Retirer tous les médicaments de l'ordonnance ?")) {
                    saisieOrdo.toutRetirer();
                }
                return;
            }
            if (onglet == jTabbedPane1.indexOfComponent(Pordonnance)) {
                consult.addOrdonnance(); // ordonnance vide (et non null : évite une NullPointerException plus tard)
            } else if (onglet == jTabbedPane1.indexOfComponent(Panalyse)) {
                consult.addAnalyse();
            }
            jTabbedPane1.remove(onglet);
        }
    }//GEN-LAST:event_supprimerBtnActionPerformed

    private void addAnalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAnalActionPerformed
        if (consult == null) {
            return;
        }
        if (jTabbedPane1.indexOfComponent(Panalyse) < 0) {
            new AnalyseForm(consult, ctx.getCatalogueService()).setVisible(true);
            addAnalyse(consult);
        } else {
            afficherInformation("Analyses déjà établies !");
        }
    }//GEN-LAST:event_addAnalActionPerformed

    private void addActActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addActActionPerformed

    }//GEN-LAST:event_addActActionPerformed

    private void jTabbedPane1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTabbedPane1MouseClicked
        if (jTabbedPane1.getSelectedIndex() >= 0) {
            analysesChoisies = Panalyse != null && jTabbedPane1.getSelectedComponent() == Panalyse;
        }
    }//GEN-LAST:event_jTabbedPane1MouseClicked

    private void dateTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dateTextActionPerformed

    }//GEN-LAST:event_dateTextActionPerformed

    private void addRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRadioActionPerformed

    }//GEN-LAST:event_addRadioActionPerformed

    private void NewConsultBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewConsultBtnActionPerformed
        NouvelleConsultation();
    }//GEN-LAST:event_NewConsultBtnActionPerformed

    private void visualiserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_visualiserBtnActionPerformed
        documentCourant(false);
    }//GEN-LAST:event_visualiserBtnActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void TempTxtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TempTxtKeyTyped
        // remplacé par FiltreSaisie (voir installerFiltres)
    }//GEN-LAST:event_TempTxtKeyTyped

    private void pulsationTxtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_pulsationTxtKeyTyped
        // remplacé par FiltreSaisie (voir installerFiltres)
    }//GEN-LAST:event_pulsationTxtKeyTyped

    private void pressionTxtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_pressionTxtKeyTyped
        // remplacé par FiltreSaisie (voir installerFiltres)
    }//GEN-LAST:event_pressionTxtKeyTyped

    private void respirationTxtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_respirationTxtKeyTyped
        // remplacé par FiltreSaisie (voir installerFiltres)
    }//GEN-LAST:event_respirationTxtKeyTyped

    private void poidsTxtKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_poidsTxtKeyTyped
        // remplacé par FiltreSaisie (voir installerFiltres)
    }//GEN-LAST:event_poidsTxtKeyTyped

    private void addSympBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSympBtnActionPerformed
        String saisie = AutoCompletion.saisie(symptomeCombo);
        if (consult != null && !saisie.isEmpty()) {
            symptome choisi = AutoCompletion.selection(symptomeCombo, symptome.class);
            if (controleur.ajouterSymptome(consult, choisi, saisie)) {
                afficherSymptomes(consult);
            }
            symptomeCombo.getEditor().setItem("");
            BoxSympmodel.removeAllElements();
        }
    }//GEN-LAST:event_addSympBtnActionPerformed
    
    
   
    private void addPathoBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPathoBtnActionPerformed
        String saisie = AutoCompletion.saisie(pathologieCombo);
        if (consult != null && !saisie.isEmpty()) {
            pathologie choisie = AutoCompletion.selection(pathologieCombo, pathologie.class);
            if (controleur.ajouterPathologie(consult, choisie, saisie)) {
                afficherPathologies(consult);
            }
            pathologieCombo.getEditor().setItem("");
            BoxPathomodel.removeAllElements();
        }
    }//GEN-LAST:event_addPathoBtnActionPerformed

    private void suppSympBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_suppSympBtnActionPerformed
        int index = SymptomeList.getSelectedIndex();
        if (index >= 0 && consult != null) {
            consult.retirerSymptome(index);
            afficherSymptomes(consult);
        }
    }//GEN-LAST:event_suppSympBtnActionPerformed

    private void suppPathoBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_suppPathoBtnActionPerformed
        int index = PathologieList.getSelectedIndex();
        if (index >= 0 && consult != null) {
            consult.retirerPathologie(index);
            afficherPathologies(consult);
        }
    }//GEN-LAST:event_suppPathoBtnActionPerformed

    private void previousBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousBtnActionPerformed
        VoirConsultation(dossier.PreviousConsultation());
        majNavigation();
        SelectHistoriqueNoeud();
    }//GEN-LAST:event_previousBtnActionPerformed

    private void nextBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextBtnActionPerformed
        VoirConsultation(dossier.NextConsultation());
        majNavigation();
        SelectHistoriqueNoeud();
    }//GEN-LAST:event_nextBtnActionPerformed

    private void LastBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LastBtnActionPerformed
        VoirConsultation(dossier.LastConsultation());
        majNavigation();
        SelectHistoriqueNoeud();
    }//GEN-LAST:event_LastBtnActionPerformed

    private void FirstBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FirstBtnActionPerformed
        VoirConsultation(dossier.FirstConsultation());
        majNavigation();
        SelectHistoriqueNoeud();
    }//GEN-LAST:event_FirstBtnActionPerformed

    private void TempTxtFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_TempTxtFocusLost
        saisirConstante(TempTxt, Constante.TEMPERATURE);
    }//GEN-LAST:event_TempTxtFocusLost

    private void pressionTxtFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_pressionTxtFocusLost
        saisirConstante(pressionTxt, Constante.PRESSION);
    }//GEN-LAST:event_pressionTxtFocusLost

    private void poidsTxtFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_poidsTxtFocusLost
        saisirConstante(poidsTxt, Constante.POIDS);
    }//GEN-LAST:event_poidsTxtFocusLost

    private void historiqueKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_historiqueKeyPressed

    }//GEN-LAST:event_historiqueKeyPressed

    private void historiqueMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_historiqueMousePressed
        // L'arbre de l'historique n'est plus affiché : la liste des visites de l'onglet
        // « Consultations » le remplace (OngletsDossier).
    }//GEN-LAST:event_historiqueMousePressed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        FermerDossier();
    }//GEN-LAST:event_formWindowClosing

    private void FermerDossierBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FermerDossierBtnActionPerformed
        FermerDossier();
    }//GEN-LAST:event_FermerDossierBtnActionPerformed

    private void imprimerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imprimerBtnActionPerformed
        documentCourant(true);
    }//GEN-LAST:event_imprimerBtnActionPerformed

    private void pulsationTxtFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_pulsationTxtFocusLost
        saisirConstante(pulsationTxt, Constante.PULSATION);
    }//GEN-LAST:event_pulsationTxtFocusLost

    private void respirationTxtFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_respirationTxtFocusLost
        saisirConstante(respirationTxt, Constante.RESPIRATION);
    }//GEN-LAST:event_respirationTxtFocusLost

    private void NewRendezVousBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewRendezVousBtnActionPerformed
        // L'agenda est une page de la fenêtre principale, qui passe devant le dossier.
        rendezVous.accept(this.dossier.getMalade());
    }//GEN-LAST:event_NewRendezVousBtnActionPerformed
    // ----- onglets ordonnance / analyses -----

    public void addOrdonnance(consultation c) {
        if (c.hasOrdonnance()) {
            Pordonnance = new PaneauOrdonnance(c.getOrdoPat());
            jTabbedPane1.addTab(ONGLET_ORDONNANCE, Icones.svg("prescription", 16, Theme.PRIMAIRE), Pordonnance);
            jTabbedPane1.setSelectedComponent(Pordonnance);
        }
    }

    public void addAnalyse(consultation c) {
        if (c.hasAnalyse()) {
            Panalyse = new PaneauAnalyse(c.getAnaPat());
            jTabbedPane1.addTab(ONGLET_ANALYSES, Icones.svg("flask", 16, Theme.PRIMAIRE), Panalyse);
            jTabbedPane1.setSelectedComponent(Panalyse);
        }
    }

    /** Aperçu (false) ou impression (true) du document de l'onglet sélectionné. */
    private void documentCourant(boolean imprimer) {
        if (dossier.getNombreConsultation() == 0) {
            return;
        }
        consultation c = dossier.getCurrentConsultation();
        int onglet = jTabbedPane1.getSelectedIndex();
        if (onglet >= 0 && (onglet == jTabbedPane1.indexOfComponent(Pordonnance)
                || onglet == jTabbedPane1.indexOfComponent(saisieOrdo))) {
            controleur.ordonnance(c, imprimer);
        } else if (onglet >= 0 && onglet == jTabbedPane1.indexOfComponent(Panalyse)) {
            controleur.analyses(c, imprimer);
        }
    }

    // ----- symptômes / pathologies -----

    void configCombo() {
        AutoCompletion.installer(symptomeCombo, BoxSympmodel, false, new AutoCompletion.Source<symptome>() {
            @Override
            public List<symptome> suggestions(String prefixe) {
                return controleur.suggestionsSymptomes(prefixe);
            }
        });
        AutoCompletion.installer(pathologieCombo, BoxPathomodel, true, new AutoCompletion.Source<pathologie>() {
            @Override
            public List<pathologie> suggestions(String prefixe) {
                return controleur.suggestionsPathologies(prefixe);
            }
        });
    }

    private void afficherSymptomes(consultation c) {
        DefaultListModel<String> m = new DefaultListModel<>();
        for (symptome s : c.getSymptomes()) {
            m.addElement(s.getDesignation());
        }
        SymptomeList.setModel(m);
    }

    private void afficherPathologies(consultation c) {
        DefaultListModel<String> m = new DefaultListModel<>();
        for (pathologie p : c.getPathologies()) {
            m.addElement(p.getDesignation());
        }
        PathologieList.setModel(m);
    }

    // ----- constantes vitales -----

    /** Valide et recopie un champ de constante dans la consultation en cours. */
    private boolean saisirConstante(JTextField champ, Constante constante) {
        if (consult == null || !champ.isEditable()) {
            return true;
        }
        if (!controleur.saisirConstante(consult, constante, champ.getText())) {
            champ.requestFocus();
            return false;
        }
        return true;
    }

    /** Relit tous les champs (au cas où le dernier n'a pas perdu le focus avant la fermeture). */
    private boolean lireConstantes() {
        return saisirConstante(TempTxt, Constante.TEMPERATURE)
                && saisirConstante(pulsationTxt, Constante.PULSATION)
                && saisirConstante(pressionTxt, Constante.PRESSION)
                && saisirConstante(respirationTxt, Constante.RESPIRATION)
                && saisirConstante(poidsTxt, Constante.POIDS);
    }

    // ----- consultations -----

    void NouvelleConsultation() {
        consultation nouvelle = controleur.nouvelleConsultation();
        if (nouvelle == null) {
            return; // message déjà affiché (une consultation est déjà en cours)
        }
        consult = nouvelle;
        activerVueConsultation();
        ActiverTraitement();
        initialiser();
        int index = dossier.getNombreConsultation() - 1;
        dossier.SetPosition(index);
        VoirConsultation(consult);
        majNavigation();
        NbreVisiteLabel.setText(String.valueOf(dossier.getNombreConsultation()));
        onglets.setSelectedIndex(OngletsDossier.CONSULTATIONS);
        sousOnglets.setSelectedIndex(DIAGNOSTICS);
        SelectHistoriqueNoeud();
        majBoutonsConsultation();
    }

    /**
     * « Enregistrer » : écrit la consultation en cours (frais demandés si le
     * patient est dans la salle d'attente) ; le dossier reste ouvert et la
     * consultation passe en lecture seule.
     */
    void enregistrerConsultation() {
        if (consult == null || !lireConstantes()) {
            return;
        }
        boolean fait = controleur.fraisADemander() ? enregistrerAvecFrais() : controleur.enregistrer();
        if (!fait) {
            return;
        }
        consultation enregistree = consult;
        consult = null;
        dossier.SetPosition(dossier.getConsultations().indexOf(enregistree));
        VoirConsultation(enregistree);
        onglets.setHistorique(controleur.historique());
        rechargerDossierClinique();
        SelectHistoriqueNoeud();
        majBoutonsConsultation();
    }

    void initialiser() {
        this.TempTxt.setText("");
        this.pulsationTxt.setText("");
        this.pressionTxt.setText("");
        this.respirationTxt.setText("");
        this.poidsTxt.setText("");
        for (int i = jTabbedPane1.getTabCount() - 1; i >= 0; i--) {
            jTabbedPane1.remove(i);
        }
        this.TempTxt.requestFocus();
    }

    private static String valeur(double v) {
        return v == consultation.NON_RENSEIGNE ? "" : String.valueOf(v);
    }

    private static String valeur(int v) {
        return v == consultation.NON_RENSEIGNE ? "" : String.valueOf(v);
    }

    void VoirConsultation(consultation myconsult) {
        if (myconsult != consult) {
            DesactiverTraitement();
        } else {
            ActiverTraitement();
        }
        initialiser();
        FiltreSaisie.afficher(TempTxt, valeur(myconsult.getTemperature()));
        FiltreSaisie.afficher(pulsationTxt, valeur(myconsult.getPul_Cardiaque()));
        FiltreSaisie.afficher(pressionTxt, myconsult.getPressionA());
        FiltreSaisie.afficher(respirationTxt, valeur(myconsult.getTauxRespir()));
        FiltreSaisie.afficher(poidsTxt, valeur(myconsult.getPoids()));
        afficherSymptomes(myconsult);
        afficherPathologies(myconsult);
        TimeTxt.setText(myconsult.getHeureString());
        dateText.setText(" " + myconsult.getDate_cons().format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.FRENCH)));
        MedcinLabel.setText(myconsult.getMedecin().getNomComplet().trim() + "  ·  consultation du "
                + myconsult.getDate_cons().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH))
                + " à " + myconsult.getHeureString());
        enTeteConsultation(myconsult);
        saisieOrdo = null;
        if (myconsult == consult) {
            // Consultation en cours : l'ordonnance se saisit directement dans l'onglet.
            if (consult.getOrdoPat() == null) {
                consult.addOrdonnance();
            }
            saisieOrdo = new SaisieOrdonnance(consult.getOrdoPat(), ctx.getCatalogueService(),
                    Posologie.ordonnancePrecedente(dossier.getConsultations(), consult),
                    () -> allergiesDossier, this::majTraitements);
            jTabbedPane1.addTab(ONGLET_ORDONNANCE, Icones.svg("prescription", 16, Theme.PRIMAIRE), saisieOrdo);
        } else if (myconsult.hasOrdonnance()) {
            Pordonnance = new PaneauOrdonnance(myconsult.getOrdoPat());
            jTabbedPane1.addTab(ONGLET_ORDONNANCE, Icones.svg("prescription", 16, Theme.PRIMAIRE), Pordonnance);
        }
        if (myconsult.hasAnalyse()) {
            Panalyse = new PaneauAnalyse(myconsult.getAnaPat());
            jTabbedPane1.addTab(ONGLET_ANALYSES, Icones.svg("flask", 16, Theme.PRIMAIRE), Panalyse);
        }
        int onglet = jTabbedPane1.indexOfComponent(analysesChoisies ? Panalyse
                : saisieOrdo != null ? saisieOrdo : Pordonnance);
        if (onglet >= 0) {
            jTabbedPane1.setSelectedIndex(onglet);
        }
    }

    /** Active / désactive les boutons de navigation selon la position courante. */
    private void majNavigation() {
        previousBtn.setEnabled(dossier.HasPrevious());
        nextBtn.setEnabled(dossier.HasNext());
    }

    void DesactiverTraitement() {
        activerSaisie(false);
    }

    void ActiverTraitement() {
        activerSaisie(true);
        this.imprimerBtn.setEnabled(true);
    }

    private void activerSaisie(boolean actif) {
        this.TempTxt.setEditable(actif);
        this.pulsationTxt.setEditable(actif);
        this.pressionTxt.setEditable(actif);
        this.respirationTxt.setEditable(actif);
        this.poidsTxt.setEditable(actif);
        this.symptomeCombo.setEditable(actif);
        this.pathologieCombo.setEditable(actif);
        this.addSympBtn.setEnabled(actif);
        this.suppSympBtn.setEnabled(actif);
        this.addPathoBtn.setEnabled(actif);
        this.suppPathoBtn.setEnabled(actif);
        this.addOrdo.setEnabled(actif);
        this.addAnal.setEnabled(actif);
        this.addRadio.setEnabled(actif);
        this.addAct.setEnabled(actif);
        this.supprimerBtn.setEnabled(actif);
    }

    void desactiverVueConsultation() {
        activerVue(false);
        this.SymptomeList.setModel(new DefaultListModel<String>());
        this.PathologieList.setModel(new DefaultListModel<String>());
    }

    void activerVueConsultation() {
        activerVue(true);
    }

    private void activerVue(boolean actif) {
        this.TempTxt.setEnabled(actif);
        this.pulsationTxt.setEnabled(actif);
        this.pressionTxt.setEnabled(actif);
        this.respirationTxt.setEnabled(actif);
        this.poidsTxt.setEnabled(actif);
        this.symptomeCombo.setEnabled(actif);
        this.pathologieCombo.setEnabled(actif);
        this.SymptomeList.setEnabled(actif);
        this.PathologieList.setEnabled(actif);
        this.addSympBtn.setEnabled(actif);
        this.suppSympBtn.setEnabled(actif);
        this.addPathoBtn.setEnabled(actif);
        this.suppPathoBtn.setEnabled(actif);
        this.addOrdo.setEnabled(actif);
        this.addAnal.setEnabled(actif);
        this.addRadio.setEnabled(actif);
        this.addAct.setEnabled(actif);
        this.visualiserBtn.setEnabled(actif);
        this.imprimerBtn.setEnabled(actif);
        this.supprimerBtn.setEnabled(actif);
    }

    /**
     * Fermeture : propose d'enregistrer s'il y a des modifications. En cas
     * d'échec, la fenêtre reste ouverte (avant, « Modifications effectuées avec
     * succès » s'affichait même si l'enregistrement avait échoué).
     */
    void FermerDossier() {
        if (!lireConstantes()) {
            return;
        }
        if (!controleur.aDesModifications()) {
            dispose();
            return;
        }
        int result = JOptionPane.showConfirmDialog(this, "Voulez-vous enregistrer les modifications ?",
                "Fermer et enregistrer ...", JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            if (controleur.fraisADemander()) {
                if (enregistrerAvecFrais()) {
                    dispose();
                }
            } else if (controleur.enregistrer()) {
                afficherInformation("Modifications effectuées avec succès !");
                dispose();
            }
        } else if (result == JOptionPane.NO_OPTION) {
            dispose();
        }
    }

    /**
     * Le patient est dans la salle d'attente : le médecin indique les frais
     * de la consultation, que la secrétaire encaissera avant de le libérer.
     *
     * @return true si la consultation a été enregistrée
     */
    private boolean enregistrerAvecFrais() {
        JTextField frais = new JTextField(controleur.fraisProposes(), 10);
        FiltreSaisie.installer(frais, FiltreSaisie.entier(7));
        Formulaire f = new Formulaire(this, "Fin de consultation", "check",
                "Frais à régler à l'accueil, en dinars. La secrétaire les "
                + "encaissera avant de libérer le patient de la salle d'attente. Laissez vide pour qu'elle les "
                + "demande, 0 pour une consultation gratuite.", "Enregistrer")
                .ligne("Frais (DA)", frais);
        return f.afficher(() -> controleur.enregistrer(frais.getText()));
    }

    /** Sélectionne dans la liste des visites la consultation affichée. */
    void SelectHistoriqueNoeud() {
        onglets.rafraichirOngletCourant();
        onglets.rafraichirVisites();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton FermerDossierBtn;
    private javax.swing.JMenu FichierMn;
    private javax.swing.JButton FirstBtn;
    private javax.swing.JButton LastBtn;
    private javax.swing.JLabel MedcinLabel;
    private javax.swing.JLabel NbreVisiteLabel;
    private javax.swing.JButton NewConsultBtn;
    private javax.swing.JButton NewRendezVousBtn;
    private javax.swing.JList<String> PathologieList;
    private javax.swing.JList<String> SymptomeList;
    private javax.swing.JTextField TempTxt;
    private javax.swing.JTextField TimeTxt;
    private javax.swing.JButton addAct;
    private javax.swing.JButton addAnal;
    private javax.swing.JButton addOrdo;
    private javax.swing.JButton addPathoBtn;
    private javax.swing.JButton addRadio;
    private javax.swing.JButton addSympBtn;
    private javax.swing.JLabel ageLabel;
    private javax.swing.JPanel consultPanel;
    private javax.swing.JTextField dateText;
    private javax.swing.JTree historique;
    private javax.swing.JButton imprimerBtn;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton nextBtn;
    private javax.swing.JLabel nomLabel;
    private javax.swing.JLabel numLabel;
    private javax.swing.JComboBox<String> pathologieCombo;
    private javax.swing.JLabel photo;
    private javax.swing.JTextField poidsTxt;
    private javax.swing.JLabel prenomLabel;
    private javax.swing.JTextField pressionTxt;
    private javax.swing.JButton previousBtn;
    private javax.swing.JTextField pulsationTxt;
    private javax.swing.JTextField respirationTxt;
    private javax.swing.JButton suppPathoBtn;
    private javax.swing.JButton suppSympBtn;
    private javax.swing.JButton supprimerBtn;
    private javax.swing.JComboBox<String> symptomeCombo;
    private javax.swing.JButton visualiserBtn;
    // End of variables declaration//GEN-END:variables
}
