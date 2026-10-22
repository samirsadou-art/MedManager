

package My_application;

import My_application.vue.Theme;
import My_application.Chat.Tchat;
import My_application.Patient.ListePatient;
import My_application.TableModelPrincipale.ConsultJourTableModel;
import My_application.TableModelPrincipale.ConsultJourTableRondu;
import My_application.TableModelPrincipale.RendezJourTableModel;
import My_application.TableModelPrincipale.RendezJourTableRondu;
import My_application.controller.AccueilController;
import My_application.controller.AccueilVue;
import My_application.controller.Executeur;
import My_application.admin.PageMembres;
import My_application.entities.Role;
import My_application.entities.Rendez_Vous;
import My_application.dao.jdbc.DatabaseConfig;
import My_application.dao.jdbc.Migrations;
import My_application.entities.consultation;
import My_application.rendezvous.calandar;
import My_application.vue.Bandeau;
import My_application.vue.Dialogues;
import My_application.vue.EnTetePage;
import My_application.vue.MenuLateral;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fenêtre principale (Vue) : rendez-vous et consultations du jour.
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
    /** Page du compte administrateur (null pour les autres rôles). */
    private PageMembres pageMembres;
    private Tchat disc;
    /** Évite d'afficher la même erreur en boucle (le dialogue réactive la fenêtre). */
    private boolean erreurDejaSignalee;

    public PrincipalForm(AppContext ctx) {
        this.ctx = ctx;
        this.role = ctx.getSession().getRole() == null ? Role.MEDECIN : ctx.getSession().getRole();
        initComponents();
        Theme.harmoniser(this);
        presenterNavigation();
        this.setLocationRelativeTo(null);
        this.setExtendedState(Frame.MAXIMIZED_BOTH);
        disc = new Tchat(this);
        TableRendezvous.setModel(RJModel);
        TableRendezvous.setDefaultRenderer(Object.class, new RendezJourTableRondu());
        TableConsult.setModel(CJModel);
        TableConsult.setDefaultRenderer(Object.class, new ConsultJourTableRondu());
        controleur = new AccueilController(this, ctx.getDossierService(), ctx.getRendezVousService(),
                Executeur.SWING, role == Role.MEDECIN);
        refresh();
    }

    // ----- AccueilVue -----

    @Override
    public void afficherRendezVousDuJour(List<Rendez_Vous> rendezVous) {
        RJModel.setDonnees(rendezVous);
        nbRendez.setText(String.valueOf(RJModel.getSize()));
        erreurDejaSignalee = false;
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
        if (pageMembres != null) {
            pageMembres.rafraichir();
            return;
        }
        controleur.rafraichir();
        nbAttente.setText("0");
    }

    /**
     * Habillage de la fenêtre principale (comme l'agenda) : bandeau en haut,
     * menu latéral à gauche et en-tête de page. L'ancienne barre d'icônes
     * sans libellés est remplacée par le menu, qui reprend ses actions.
     * <p>
     * Le contenu dépend du rôle : le médecin voit tout ; la secrétaire n'a
     * pas les consultations du jour (dossier médical) ; l'administrateur
     * arrive sur les membres du cabinet.
     */
    private void presenterNavigation() {
        Fnord.setVisible(false);
        Fsud.setVisible(false);

        EnTetePage entete;
        Container contenu = getContentPane();
        if (role == Role.ADMIN) {
            entete = new EnTetePage("users", "Membres du cabinet",
                    "Comptes de connexion des médecins, de la secrétaire et des administrateurs");
            pageMembres = new PageMembres(ctx.getMembresService());
            contenu = pageMembres;
            jMenu2.setVisible(false);
        } else {
            entete = new EnTetePage("home", "Accueil", role == Role.SECRETAIRE
                    ? "Rendez-vous et salle d'attente du jour"
                    : "Consultations, rendez-vous et salle d'attente du jour");
            if (role == Role.SECRETAIRE) {
                // Les consultations (motifs, diagnostics) relèvent du dossier médical.
                centre.remove(jSplitPane1);
                centre.add(jPanel2, BorderLayout.CENTER);
            }
            ((JComponent) contenu).setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        }
        entete.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        entete.ajouterBouton("Actualiser", "refresh", this::refresh);
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Theme.FOND);
        page.add(entete, BorderLayout.NORTH);
        page.add(contenu, BorderLayout.CENTER);

        MenuLateral menu = new MenuLateral();
        if (role == Role.ADMIN) {
            menu.ajouter("users", "Membres", true, this::refresh);
            menu.selectionner("Membres");
        } else {
            menu.ajouter("home", "Accueil", true, this::refresh);
            menu.ajouter("users", "Patients", false, () -> userMenuBtn.doClick());
            menu.ajouter("calendar-event", "Rendez-vous", false, () -> new calandar(null, null, ctx.getRendezVousService()).setVisible(true));
            menu.ajouter("message-circle", "Messagerie", false, () -> ChatBtn.doClick());
            menu.selectionner("Accueil");
        }
        menu.setPied("GestiMed<br>Gestion du cabinet médical");

        Bandeau bandeau = new Bandeau("GestiMed", "Cabinet médical");
        String date = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH).format(LocalDate.now());
        bandeau.ajouterInfo("calendar", Character.toUpperCase(date.charAt(0)) + date.substring(1));
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
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();

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

        jMenu1.setText("Fichier");

        jMenuItem1.setText("Ouvrir Session");
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Patient");

        jMenuItem2.setText("Liste des patients");
        jMenu2.add(jMenuItem2);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void userMenuBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userMenuBtnActionPerformed
     
        new ListePatient(ctx).setVisible(true);
     
        
    }//GEN-LAST:event_userMenuBtnActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        refresh();
    }//GEN-LAST:event_formWindowActivated

    private void ChatBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChatBtnActionPerformed
        
                
        disc.montrer();
    }//GEN-LAST:event_ChatBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        disc.Close();
        ctx.close();
    }//GEN-LAST:event_formWindowClosing

    

    

    
 
    static final String BASE_EN_RETARD = "La base de données est plus ancienne que cette version de GestiMed.\n"
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
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
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
