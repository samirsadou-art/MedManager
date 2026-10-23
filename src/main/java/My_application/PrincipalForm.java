

package My_application;

import My_application.Patient.PagePatients;
import My_application.controller.ListePatientController;
import My_application.TableModelPrincipale.ConsultJourTableModel;
import My_application.TableModelPrincipale.ConsultJourTableRondu;
import My_application.TableModelPrincipale.RendezJourTableModel;
import My_application.TableModelPrincipale.RendezJourTableRondu;
import My_application.admin.PageMembres;
import My_application.admin.PageStatistiques;
import My_application.consultations.PageConsultations;
import My_application.controller.AccueilController;
import My_application.controller.AccueilVue;
import My_application.controller.ConnexionController;
import My_application.controller.Executeur;
import My_application.dao.jdbc.DatabaseConfig;
import My_application.dao.jdbc.Migrations;
import My_application.entities.DossierPatient;
import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.entities.consultation;
import My_application.entities.patient;
import My_application.messagerie.FenetreMessagerie;
import My_application.rendezvous.PageRendezVous;
import My_application.salle.PanneauSalleAttente;
import My_application.service.MotDePasse;
import My_application.vue.Bandeau;
import My_application.vue.Dialogues;
import My_application.vue.EnTetePage;
import My_application.vue.FiltreSaisie;
import My_application.vue.Formulaire;
import My_application.vue.Icones;
import My_application.vue.MenuLateral;
import My_application.vue.Theme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Timer;

/**
 * Fenêtre principale (Vue) : rendez-vous et salle d'attente du jour.
 * Les données sont fournies par {@link AccueilController} ; la fenêtre ne
 * fait que les afficher.
 */
public class PrincipalForm extends javax.swing.JFrame implements AccueilVue {

    private final RendezJourTableModel RJModel = new RendezJourTableModel();
    private final ConsultJourTableModel CJModel = new ConsultJourTableModel();
    private final AppContext ctx;
    private final AccueilController controleur;
    /** Rôle du compte connecté : il choisit la page d'accueil et le menu. */
    private final Role role;
    private static final String TITRE_MEMBRES = "Membres du cabinet";
    private static final String SOUS_TITRE_ACCUEIL = "Rendez-vous et salle d'attente du jour";
    private static final String SOUS_TITRE_MEMBRES =
            "Comptes de connexion des médecins, de la secrétaire et des administrateurs";
    /** Pages du compte administrateur (null pour les autres rôles). */
    private PageMembres pageMembres;
    private PageStatistiques pageStatistiques;
    private JPanel pagesAdmin;
    /** Médecin : page « Consultations » (date choisie) à côté de l'accueil, dans le panneau central. */
    private PageConsultations pageConsultations;
    /** Secrétaire et médecin : page « Patients » (liste et recherche), dans le panneau central. */
    private PagePatients pagePatients;
    /** Secrétaire et médecin : page « Rendez-vous » (agenda de la semaine), dans le panneau central. */
    private PageRendezVous pageRendezVous;
    /** Pages du panneau central de la secrétaire et du médecin (accueil, patients, rendez-vous, consultations). */
    private JPanel pagesCabinet;
    private EnTetePage entete;
    /** Boutons de l'en-tête propres à la page « Rendez-vous » (cachés ailleurs). */
    private final List<JButton> boutonsAgenda = new ArrayList<>();
    /** Messagerie interne (null pour l'administrateur) : créée au démarrage, montrée par le menu. */
    private FenetreMessagerie messagerie;
    private MenuLateral menu;
    /** Tours du minuteur : la présence n'est signalée qu'un tour sur dix (30 s). */
    private int tours;
    /** Évite d'afficher la même erreur en boucle (le dialogue réactive la fenêtre). */
    private boolean erreurDejaSignalee;
    private PanneauSalleAttente salle;
    /** Toutes les 3 secondes : un autre poste a-t-il changé la salle d'attente ou l'agenda ? */
    private Timer surveillance;
    private final JButton arriveBtn = new JButton("Patient arrivé");
    private final JButton absentBtn = new JButton("Absent");

    public PrincipalForm(AppContext ctx) {
        this.ctx = ctx;
        this.role = ctx.getSession().getRole() == null ? Role.MEDECIN : ctx.getSession().getRole();
        initComponents();
        setTitle(Theme.NOM_LOGICIEL);
        Theme.harmoniser(this);
        presenterNavigation();
        this.setLocationRelativeTo(null);
        this.setExtendedState(Frame.MAXIMIZED_BOTH);
        TableRendezvous.setModel(RJModel);
        TableRendezvous.setDefaultRenderer(Object.class, new RendezJourTableRondu());
        TableConsult.setModel(CJModel);
        TableConsult.setDefaultRenderer(Object.class, new ConsultJourTableRondu());
        controleur = new AccueilController(this, ctx.getDossierService(), ctx.getRendezVousService(),
                Executeur.SWING, false);
        if (role != Role.ADMIN) {
            messagerie = new FenetreMessagerie(ctx.getMessagerieService(), ctx.getPatientService(),
                    ctx.getSession().getIdUtilisateurConnecte(), n -> menu.setBadge("Messagerie", n));
            messagerie.demarrer();
            messagerie.signalerPresence(nomDuPoste());
            installerSalleAttente();
        }
        refresh();
    }

    /** Compte administrateur : montre la page « Membres » ou « Statistiques » et met l'en-tête à jour. */
    private void pageAdmin(EnTetePage entete, String page) {
        ((CardLayout) pagesAdmin.getLayout()).show(pagesAdmin, page);
        if ("Statistiques".equals(page)) {
            entete.setTitre("Statistiques");
            entete.setSousTitre("Activité du cabinet par période et par médecin, sans aucun nom de patient");
            pageStatistiques.rafraichir();
        } else {
            entete.setTitre(TITRE_MEMBRES);
            entete.setSousTitre(SOUS_TITRE_MEMBRES);
            pageMembres.rafraichir();
        }
    }

    /**
     * Menu « Accueil », « Patients », « Rendez-vous » ou « Consultations » :
     * montre la page dans le panneau central et met l'en-tête à jour
     * (« Consultations » est réservé au médecin). Par le menu, l'agenda
     * s'ouvre sans patient.
     */
    private void pageCabinet(String page) {
        pageCabinet(page, null);
    }

    /** Idem ; {@code pourPatient} : patient de l'agenda (page « Rendez-vous » seulement). */
    private void pageCabinet(String page, patient pourPatient) {
        ((CardLayout) pagesCabinet.getLayout()).show(pagesCabinet, page);
        for (JButton b : boutonsAgenda) {
            b.setVisible("Rendez-vous".equals(page));
        }
        if ("Rendez-vous".equals(page)) {
            entete.setIcone("calendar-event");
            entete.setTitre("Rendez-vous");
            entete.setSousTitre("Agenda de la semaine : prenez, déplacez, pointez ou annulez les rendez-vous");
            pageRendezVous.afficher(pourPatient);
        } else if ("Patients".equals(page)) {
            entete.setIcone("users");
            entete.setTitre("Patients");
            entete.setSousTitre(role == Role.MEDECIN
                    ? "Recherchez un patient (nom, téléphone ou n° de dossier) pour ouvrir son dossier, ou inscrivez-le"
                    : "Recherchez un patient (nom, téléphone ou n° de dossier), inscrivez-le ou prenez-lui un rendez-vous");
            pagePatients.rafraichir();
            pagePatients.saisirRecherche();
        } else if ("Consultations".equals(page)) {
            entete.setIcone("stethoscope");
            entete.setTitre("Consultations");
            entete.setSousTitre("Consultations de la date choisie, tous médecins confondus");
            pageConsultations.rafraichir();
        } else {
            entete.setIcone("home");
            entete.setTitre("Accueil");
            entete.setSousTitre(SOUS_TITRE_ACCUEIL);
            controleur.rafraichir();
        }
    }

    /** Menu latéral « Mot de passe » : chaque membre connecté change le sien. */
    private void changerMotDePasse() {
        JPasswordField actuel = new JPasswordField(20);
        JPasswordField nouveau = new JPasswordField(20);
        JPasswordField repete = new JPasswordField(20);
        ConnexionController c = new ConnexionController(this, ctx.getAuthentificationService());
        new Formulaire(this, "Changer mon mot de passe", "key", "Au moins " + MotDePasse.LONGUEUR_MIN
                + " caractères, dont une lettre et un chiffre.", "Changer")
                .ligne("Mot de passe actuel", actuel)
                .ligne("Nouveau mot de passe", nouveau)
                .ligne("Confirmation", repete)
                .afficher(() -> c.changerMonMotDePasse(actuel.getPassword(), nouveau.getPassword(),
                        repete.getPassword()));
    }

    /**
     * Menu « Se déconnecter » : ferme toutes les fenêtres de ce membre
     * (dossiers, agenda, messagerie…) et sa session, puis rouvre l'écran de
     * connexion pour le membre suivant. Quitter cet écran ferme l'application.
     */
    private void seDeconnecter() {
        if (!Dialogues.confirmer(this, "Fermer la session de " + ctx.getSession().getUtilisateur() + " ?")) {
            return;
        }
        if (surveillance != null) {
            surveillance.stop();
        }
        if (messagerie != null) {
            messagerie.quitter();
            messagerie.dispose();
        }
        for (Window w : Window.getWindows()) {
            if (w.isShowing()) {
                w.dispose();
            }
        }
        dispose();
        ctx.getAuthentificationService().deconnecter();
        if (ConnexionForm.demander(ctx.getAuthentificationService()) == null) {
            ctx.close();
            System.exit(0);
        }
        new PrincipalForm(ctx).setVisible(true);
    }

    /** Nom de l'ordinateur, affiché avec la présence des membres. */
    private static String nomDuPoste() {
        String nom = System.getenv("COMPUTERNAME");
        if (nom == null) {
            nom = System.getenv("HOSTNAME");
        }
        return nom == null ? null : nom.length() > 60 ? nom.substring(0, 60) : nom;
    }

    /**
     * Salle d'attente : le médecin voit sa file et appelle ; la secrétaire voit
     * toutes les files, pointe les arrivées et est prévenue de chaque appel.
     * Sous les rendez-vous du jour : pointer l'arrivée, l'absence, la fin de journée.
     */
    private void installerSalleAttente() {
        boolean secretaire = role == Role.SECRETAIRE;
        Integer medecinDuPoste = ctx.getSession().getIdMedecinDuPoste();
        controleur.brancherSalleAttente(ctx.getSalleAttenteService(), secretaire ? null : medecinDuPoste,
                ctx.getSession()::getMedecinTraitant, secretaire, secretaire);

        salle = new PanneauSalleAttente(secretaire || medecinDuPoste == null);
        if (secretaire) {
            salle.ajouterAction("Patient sans rendez-vous…", "user-plus", () -> userMenuBtn.doClick());
            salle.ajouterBouton("Encaisser et libérer…", "check", p -> p.getEtat() == Passage.A_REGLER,
                    this::encaisserEtLiberer);
            salle.surDoubleClic(p -> {
                if (p.getEtat() == Passage.A_REGLER) {
                    encaisserEtLiberer(p);
                }
            });
        } else {
            salle.ajouterAction("Patient suivant", "speakerphone", controleur::appelerSuivant);
            salle.ajouterBouton("Appeler", "stethoscope",
                    p -> p.getEtat() == Passage.EN_ATTENTE || p.getEtat() == Passage.EN_CONSULTATION,
                    controleur::appeler);
            salle.surDoubleClic(controleur::appeler);
        }
        salle.ajouterBouton("Urgence", "first-aid-kit", p -> p.getEtat() == Passage.EN_ATTENTE,
                controleur::urgence);
        salle.ajouterBouton("Parti sans consultation", "x", p -> p.getEtat() == Passage.EN_ATTENTE, p -> {
            if (Dialogues.confirmer(this, p.getPatient() + " est reparti sans consultation ?")) {
                controleur.parti(p);
            }
        });
        jPanel11.removeAll();
        jPanel11.add(salle, BorderLayout.CENTER);
        jButton2.setVisible(false);

        arriveBtn.setIcon(Icones.svg("check", 16, Theme.PRIMAIRE));
        absentBtn.setIcon(Icones.svg("circle-minus", 16, Theme.PRIMAIRE));
        JButton finBtn = new JButton("Fin de journée…", Icones.svg("player-stop", 16, Theme.PRIMAIRE));
        arriveBtn.addActionListener(e -> {
            Rendez_Vous r = rendezVousSelectionne();
            if (r != null) {
                controleur.arrivee(r);
            }
        });
        absentBtn.addActionListener(e -> {
            Rendez_Vous r = rendezVousSelectionne();
            if (r != null && Dialogues.confirmer(this, r.getPatient() + " n'est pas venu ?")) {
                controleur.absent(r);
            }
        });
        finBtn.addActionListener(e -> {
            if (Dialogues.confirmer(this, "Fin de journée : les rendez-vous d'aujourd'hui encore prévus et "
                    + "dont l'heure est passée seront marqués absents.\nContinuer ?")) {
                controleur.finDeJournee();
            }
        });
        TableRendezvous.getSelectionModel().addListSelectionListener(e -> majBoutonsRendezVous());
        majBoutonsRendezVous();
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(arriveBtn);
        actions.add(absentBtn);
        actions.add(finBtn);
        jPanel9.add(actions);

        surveillance = new Timer(3000, e -> {
            controleur.verifierEvenements();
            salle.setMaintenant(LocalDateTime.now());
            messagerie.verifier();
            if (++tours % 10 == 0) {
                messagerie.signalerPresence(nomDuPoste());
            }
        });
        surveillance.start();
        controleur.purgerEvenements();
    }

    /**
     * Le patient a terminé sa consultation : la secrétaire encaisse les frais
     * fixés par le médecin (modifiables) et le libère de la salle d'attente.
     */
    private void encaisserEtLiberer(Passage p) {
        Integer frais = p.getFrais();
        JTextField montant = new JTextField(frais == null ? "" : String.valueOf(frais), 10);
        FiltreSaisie.installer(montant, FiltreSaisie.entier(7));
        JLabel patient = new JLabel(p.getPatient().getNom().toUpperCase(Locale.FRENCH) + " "
                + p.getPatient().getPrenom());
        patient.setFont(Theme.policeGras());
        JLabel demandes = new JLabel(frais == null ? "non précisés par le médecin"
                : frais == 0 ? "consultation gratuite" : AccueilController.montant(frais));
        new Formulaire(this, "Encaisser et libérer", "check",
                "Saisissez le montant reçu, puis le patient quitte la salle d'attente. "
                + "Laissez vide s'il n'a rien payé.", "Libérer le patient")
                .ligne("Patient", patient).ligne("Frais demandés", demandes).ligne("Montant reçu (DA)", montant)
                .afficher(() -> controleur.liberer(p, montant.getText()));
    }

    private Rendez_Vous rendezVousSelectionne() {
        int ligne = TableRendezvous.getSelectedRow();
        return ligne < 0 ? null : RJModel.getElement(TableRendezvous.convertRowIndexToModel(ligne));
    }

    private void majBoutonsRendezVous() {
        Rendez_Vous r = rendezVousSelectionne();
        arriveBtn.setEnabled(controleur.peutPointer(r));
        absentBtn.setEnabled(controleur.peutMarquerAbsent(r));
    }

    // ----- AccueilVue -----

    @Override
    public void afficherRendezVousDuJour(List<Rendez_Vous> rendezVous) {
        Rendez_Vous avant = rendezVousSelectionne();
        RJModel.setDonnees(rendezVous);
        nbRendez.setText(String.valueOf(RJModel.getSize()));
        erreurDejaSignalee = false;
        if (avant != null && avant.getIdRendezVous() != null) {
            for (int i = 0; i < RJModel.getSize(); i++) {
                if (avant.getIdRendezVous().equals(RJModel.getElement(i).getIdRendezVous())) {
                    int ligne = TableRendezvous.convertRowIndexToView(i);
                    TableRendezvous.setRowSelectionInterval(ligne, ligne);
                }
            }
        }
        if (salle != null) {
            majBoutonsRendezVous();
        }
    }

    @Override
    public void afficherSalleAttente(List<Passage> file, Map<Integer, String> medecins, LocalDateTime maintenant) {
        salle.setFile(file, medecins, maintenant);
        int enAttente = 0;
        for (Passage p : file) {
            if (p.getEtat() == Passage.EN_ATTENTE) {
                enAttente++;
            }
        }
        nbAttente.setText(String.valueOf(enAttente));
    }

    @Override
    public void annoncerAppel(String annonce) {
        salle.annoncer(annonce);
        Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public void ouvrirDossier(DossierPatient dossier) {
        new DossierPatientForm(dossier, ctx, this::rendezVousPour).setVisible(true);
    }

    /**
     * « Rendez-vous » d'un patient (liste des patients ou dossier) : montre
     * l'agenda du panneau central pour ce patient, fenêtre principale devant.
     */
    private void rendezVousPour(patient p) {
        menu.selectionner("Rendez-vous");
        pageCabinet("Rendez-vous", p);
        if ((getExtendedState() & Frame.ICONIFIED) != 0) {
            setExtendedState(getExtendedState() & ~Frame.ICONIFIED);
        }
        toFront();
        requestFocus();
    }

    @Override
    public void afficherConsultationsDuJour(List<consultation> consultations) {
        CJModel.setDonnees(consultations);
        nbConsult.setText(String.valueOf(CJModel.getSize()));
        erreurDejaSignalee = false;
    }

    @Override
    public void afficherErreur(String message) {
        if (!erreurDejaSignalee) {
            erreurDejaSignalee = true;
            Dialogues.erreur(this, message);
        }
    }

    @Override
    public void afficherInformation(String message) {
        Dialogues.information(this, message);
    }

    public void refresh() {
        if (pagePatients != null && pagePatients.isShowing()) {
            pagePatients.rafraichir();
            return;
        }
        if (pageConsultations != null && pageConsultations.isShowing()) {
            pageConsultations.rafraichir();
            return;
        }
        if (pageRendezVous != null && pageRendezVous.isShowing()) {
            pageRendezVous.rafraichir();
            return;
        }
        if (pageStatistiques != null && pageStatistiques.isShowing()) {
            pageStatistiques.rafraichir();
            return;
        }
        if (pageMembres != null) {
            pageMembres.rafraichir();
            return;
        }
        controleur.rafraichir();
    }

    /**
     * Habillage de la fenêtre principale (comme l'agenda) : bandeau en haut,
     * menu latéral à gauche et en-tête de page. L'ancienne barre d'icônes
     * sans libellés est remplacée par le menu, qui reprend ses actions.
     * <p>
     * Le contenu dépend du rôle : le médecin a en plus la page
     * « Consultations » (date au choix) ; l'administrateur
     * arrive sur les membres du cabinet.
     */
    private void presenterNavigation() {
        Fnord.setVisible(false);
        Fsud.setVisible(false);

        Container contenu = getContentPane();
        if (role == Role.ADMIN) {
            entete = new EnTetePage("users", TITRE_MEMBRES, SOUS_TITRE_MEMBRES);
            pageMembres = new PageMembres(ctx.getMembresService());
            pageStatistiques = new PageStatistiques(ctx.getStatistiquesService(), ctx.getRapportService());
            pagesAdmin = new JPanel(new CardLayout());
            pagesAdmin.setOpaque(false);
            pagesAdmin.add(pageMembres, "Membres");
            pagesAdmin.add(pageStatistiques, "Statistiques");
            contenu = pagesAdmin;
        } else {
            entete = new EnTetePage("home", "Accueil", SOUS_TITRE_ACCUEIL);
            // Plus de panneau des consultations à l'accueil : le médecin a le
            // menu « Consultations » (date au choix) ; la secrétaire ne les voit
            // pas (dossier médical).
            centre.remove(jSplitPane1);
            centre.add(jPanel2, BorderLayout.CENTER);
            ((JComponent) contenu).setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
            pagesCabinet = new JPanel(new CardLayout());
            pagesCabinet.setOpaque(false);
            pagesCabinet.add(contenu, "Accueil");
            pagePatients = new PagePatients(ctx.getPatientService(),
                    v -> new ListePatientController(v, ctx.getPatientService(), ctx.getDossierService(),
                            ctx.getSession(), ctx.getSalleAttenteService(), ctx.getRendezVousService()),
                    role == Role.MEDECIN, this::ouvrirDossier, this::rendezVousPour);
            pagesCabinet.add(pagePatients, "Patients");
            pageRendezVous = new PageRendezVous(ctx.getSession().getUtilisateur(), ctx.getRendezVousService(),
                    ctx.getRapportService(), () -> {
                        menu.selectionner("Patients");
                        pageCabinet("Patients");
                    }, role == Role.MEDECIN ? ctx.getSession().getIdMedecinDuPoste() : null);
            pagesCabinet.add(pageRendezVous, "Rendez-vous");
            if (role == Role.MEDECIN) {
                pageConsultations = new PageConsultations(ctx.getDossierService(), ctx.getPatientService(),
                        ctx.getRendezVousService(), ctx.getSession()::getMedecinTraitant,
                        this::ouvrirDossier);
                pagesCabinet.add(pageConsultations, "Consultations");
            }
            contenu = pagesCabinet;
        }
        entete.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        if (pageRendezVous != null) {
            boutonsAgenda.add(entete.ajouterBouton("Aujourd'hui", "calendar", pageRendezVous::allerAujourdhui));
            boutonsAgenda.add(entete.ajouterBouton("Liste du jour", "printer", pageRendezVous::listeDuJour));
            for (JButton b : boutonsAgenda) {
                b.setVisible(false);
            }
        }
        entete.ajouterBouton("Actualiser", "refresh", this::refresh);
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Theme.FOND);
        page.add(entete, BorderLayout.NORTH);
        page.add(contenu, BorderLayout.CENTER);

        menu = new MenuLateral();
        if (role == Role.ADMIN) {
            menu.ajouter("users", "Membres", true, () -> pageAdmin(entete, "Membres"));
            menu.ajouter("chart-bar", "Statistiques", true, () -> pageAdmin(entete, "Statistiques"));
            menu.selectionner("Membres");
        } else {
            menu.ajouter("home", "Accueil", true, () -> pageCabinet("Accueil"));
            menu.ajouter("users", "Patients", true, () -> pageCabinet("Patients"));
            if (pageConsultations != null) {
                menu.ajouter("stethoscope", "Consultations", true, () -> pageCabinet("Consultations"));
            }
            menu.ajouter("calendar-event", "Rendez-vous", true, () -> pageCabinet("Rendez-vous"));
            menu.ajouter("message-circle", "Messagerie", false, () -> ChatBtn.doClick());
            menu.selectionner("Accueil");
        }
        if (ctx.getSession().estOuverte()) {
            menu.ajouterEnBas("key", "Mot de passe", this::changerMotDePasse);
            menu.ajouterEnBas("logout", "Se déconnecter", this::seDeconnecter);
        }
        menu.setPied(Theme.NOM_LOGICIEL + "<br>Gestion du cabinet médical");

        Bandeau bandeau = new Bandeau(Theme.NOM_LOGICIEL, "Cabinet médical");
        if (ctx.getSession().estOuverte()) {
            bandeau.ajouterInfo("user", ctx.getSession().getUtilisateur() + " · " + role.getLibelle());
        }

        JPanel racine = new JPanel(new BorderLayout());
        racine.add(bandeau, BorderLayout.NORTH);
        racine.add(menu, BorderLayout.WEST);
        racine.add(page, BorderLayout.CENTER);
        setContentPane(racine);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Fcenter = new javax.swing.JPanel();
        North = new javax.swing.JPanel();
        centre = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        TableConsult = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        nbConsult = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel6 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        TableRendezvous = new javax.swing.JTable();
        jPanel9 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        nbRendez = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        nbAttente = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        TableSalleAttente = new javax.swing.JTable();
        Fnord = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        userMenuBtn = new javax.swing.JButton();
        ChatBtn = new javax.swing.JButton();
        Fsud = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        Fcenter.setLayout(new java.awt.BorderLayout());

        North.setBackground(new java.awt.Color(102, 255, 102));
        Fcenter.add(North, java.awt.BorderLayout.NORTH);

        centre.setLayout(new java.awt.BorderLayout());

        jSplitPane1.setDividerLocation(600);
        jSplitPane1.setDividerSize(3);

        jPanel1.setBackground(new java.awt.Color(255, 204, 204));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Consultations journalières"));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.BorderLayout());

        TableConsult.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Genre", "Nom", "Prénom", "Age", "Heure consultation"
            }
        ));
        TableConsult.setRowHeight(40);
        TableConsult.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        TableConsult.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(TableConsult);

        jPanel4.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel2.setText("Nombre de consultations :");
        jPanel5.add(jLabel2);

        nbConsult.setEditable(false);
        nbConsult.setForeground(new java.awt.Color(153, 0, 0));
        nbConsult.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nbConsult.setPreferredSize(new java.awt.Dimension(40, 25));
        jPanel5.add(nbConsult);

        jLabel6.setText("Consultation(s)");
        jPanel5.add(jLabel6);

        jPanel1.add(jPanel5, java.awt.BorderLayout.SOUTH);

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setBackground(new java.awt.Color(255, 255, 204));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jSplitPane2.setDividerLocation(250);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel6.setBackground(new java.awt.Color(204, 255, 204));
        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Rendez-vous du jour"));
        jPanel6.setLayout(new java.awt.BorderLayout());

        jPanel8.setLayout(new java.awt.BorderLayout());

        TableRendezvous.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Genre", "Nom", "Prénom", "Age", "Horaire"
            }
        ));
        TableRendezvous.setIntercellSpacing(new java.awt.Dimension(1, 2));
        TableRendezvous.setRowHeight(40);
        TableRendezvous.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        TableRendezvous.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(TableRendezvous);

        jPanel8.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jPanel6.add(jPanel8, java.awt.BorderLayout.CENTER);

        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel3.setText("Nombre de patients:");
        jPanel9.add(jLabel3);

        nbRendez.setEditable(false);
        nbRendez.setForeground(new java.awt.Color(153, 0, 0));
        nbRendez.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nbRendez.setPreferredSize(new java.awt.Dimension(40, 25));
        jPanel9.add(nbRendez);

        jLabel1.setText("Patient(s)");
        jPanel9.add(jLabel1);

        jPanel6.add(jPanel9, java.awt.BorderLayout.SOUTH);

        jSplitPane2.setLeftComponent(jPanel6);

        jPanel7.setBackground(new java.awt.Color(255, 204, 204));
        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Salle d'attente"));
        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel10.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel4.setText("Nombre de patients:");
        jPanel10.add(jLabel4);

        nbAttente.setEditable(false);
        nbAttente.setForeground(new java.awt.Color(153, 0, 0));
        nbAttente.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nbAttente.setPreferredSize(new java.awt.Dimension(40, 25));
        jPanel10.add(nbAttente);

        jLabel5.setText("Patient(s)");
        jPanel10.add(jLabel5);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/chat24.png"))); // NOI18N
        jPanel10.add(jButton2);

        jPanel7.add(jPanel10, java.awt.BorderLayout.SOUTH);

        jPanel11.setLayout(new java.awt.BorderLayout());

        TableSalleAttente.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Genre", "Nom", "Prénom", "Age"
            }
        ));
        TableSalleAttente.setRowHeight(40);
        jScrollPane3.setViewportView(TableSalleAttente);

        jPanel11.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel7.add(jPanel11, java.awt.BorderLayout.CENTER);

        jSplitPane2.setRightComponent(jPanel7);

        jPanel2.add(jSplitPane2, java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanel2);

        centre.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        Fcenter.add(centre, java.awt.BorderLayout.CENTER);

        getContentPane().add(Fcenter, java.awt.BorderLayout.CENTER);

        Fnord.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        Fnord.setPreferredSize(new java.awt.Dimension(972, 65));
        Fnord.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/consult48.png"))); // NOI18N
        jButton1.setPreferredSize(new java.awt.Dimension(60, 60));
        Fnord.add(jButton1);

        userMenuBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/manicon48.png"))); // NOI18N
        userMenuBtn.setPreferredSize(new java.awt.Dimension(60, 60));
        userMenuBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userMenuBtnActionPerformed(evt);
            }
        });
        Fnord.add(userMenuBtn);

        ChatBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/chat48.png"))); // NOI18N
        ChatBtn.setPreferredSize(new java.awt.Dimension(60, 60));
        ChatBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ChatBtnActionPerformed(evt);
            }
        });
        Fnord.add(ChatBtn);

        getContentPane().add(Fnord, java.awt.BorderLayout.NORTH);

        Fsud.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        Fsud.setPreferredSize(new java.awt.Dimension(10, 30));
        getContentPane().add(Fsud, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void userMenuBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userMenuBtnActionPerformed
     
        // Bouton caché de l'ancienne barre d'icônes (repris par « Patient sans
        // rendez-vous… ») : montre la page « Patients » du panneau central.
        menu.selectionner("Patients");
        pageCabinet("Patients");
     
        
    }//GEN-LAST:event_userMenuBtnActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        refresh();
    }//GEN-LAST:event_formWindowActivated

    private void ChatBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChatBtnActionPerformed
        
                
        if (messagerie != null) {
            messagerie.montrer();
        }
    }//GEN-LAST:event_ChatBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (surveillance != null) {
            surveillance.stop();
        }
        if (messagerie != null) {
            messagerie.quitter();
            messagerie.dispose();
        }
        ctx.close();
    }//GEN-LAST:event_formWindowClosing

    

    

    
 
    static final String BASE_EN_RETARD = "La base de données est plus ancienne que cette version de " + Theme.NOM_LOGICIEL + ".\n"
            + "Faites une sauvegarde, puis mettez-la à jour avec la commande :\n"
            + "  java -cp gestimed-3.5-jar-with-dependencies.jar My_application.outils.MigrationBase migrer\n\n";

    /**
     * Compare la version de la base aux migrations livrées. Si la vérification
     * est impossible (serveur arrêté...), on laisse l'écran de connexion
     * signaler l'erreur habituelle.
     */
    private static boolean baseAJour() {
        Logger log = Logger.getLogger("GestiMed");
        try {
            Migrations.Etat etat = new Migrations(DatabaseConfig.charger()).etat();
            if (etat.estEnRetard()) {
                log.severe("Schéma en retard : " + etat);
                Dialogues.erreur(null, BASE_EN_RETARD + etat);
                return false;
            }
            if (!etat.estSuivie()) {
                log.warning("Schéma non suivi par les migrations (" + etat + ") : lancer une fois "
                        + "My_application.outils.MigrationBase migrer pour l'adopter.");
            }
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Vérification de la version du schéma impossible", e);
        }
        return true;
    }

    public static void main(String args[]) {
        // Détail technique des erreurs : dans ~/GestiMed/gestimed0.log, pas à l'écran.
        Journal.installer();
        // Charte graphique GestiMed (FlatLaf, palette bleu-vert médical).
        Theme.installer();

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Une base en retard sur le code ferait échouer les saisies : on s'arrête avant.
                // Personne n'accède aux dossiers sans s'être identifié.
                // Seul endroit qui assemble l'application : tout le reste reçoit ses dépendances.
                AppContext ctx = AppContext.mysql();
                if (!baseAJour() || ConnexionForm.demander(ctx.getAuthentificationService()) == null) {
                    ctx.close();
                    System.exit(0);
                }
                new PrincipalForm(ctx).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ChatBtn;
    private javax.swing.JPanel Fcenter;
    private javax.swing.JPanel Fnord;
    private javax.swing.JPanel Fsud;
    private javax.swing.JPanel North;
    private javax.swing.JTable TableConsult;
    private javax.swing.JTable TableRendezvous;
    private javax.swing.JTable TableSalleAttente;
    private javax.swing.JPanel centre;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTextField nbAttente;
    private javax.swing.JTextField nbConsult;
    private javax.swing.JTextField nbRendez;
    private javax.swing.JButton userMenuBtn;
    // End of variables declaration//GEN-END:variables
}
