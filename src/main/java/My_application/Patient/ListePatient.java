/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.Patient;

import My_application.vue.EnTetePage;
import My_application.vue.Theme;
import My_application.AppContext;
import My_application.DossierPatientForm;
import My_application.controller.ListePatientController;
import My_application.controller.ListePatientVue;
import My_application.entities.DossierPatient;
import My_application.entities.Role;
import My_application.entities.patient;
import My_application.rendezvous.calandar;
import My_application.vue.Dialogues;
import java.util.List;
import javax.swing.JButton;

/**
 * Liste des patients (Vue). La recherche (nom ou téléphone) et l'ouverture du
 * dossier sont déléguées à {@link ListePatientController} ; plus de SQL dans
 * la fenêtre. Inscription et modification passent par {@link FichePatient}.
 */
public class ListePatient extends javax.swing.JDialog implements ListePatientVue {

    private final ListePatientTableModel listeModel = new ListePatientTableModel();
    private final AppContext ctx;
    private final ListePatientController controleur;
    /** Seul un médecin ouvre le dossier ; la secrétaire ouvre la fiche administrative. */
    private final boolean medecin;
    private final JButton modifierBtn = new JButton("Modifier la fiche");
    private final JButton rendezVousBtn = new JButton("Prendre un rendez-vous");

    public ListePatient(AppContext ctx) {
        this.ctx = ctx;
        this.medecin = ctx.getSession().getRole() == null || ctx.getSession().getRole() == Role.MEDECIN;
        initComponents();
        Theme.harmoniser(this);
        EnTetePage.installer(this, "users", "Patients", medecin
                ? "Recherchez un patient (nom ou téléphone) pour ouvrir son dossier, ou inscrivez-le"
                : "Recherchez un patient (nom ou téléphone), inscrivez-le ou prenez-lui un rendez-vous");
        this.choix.requestFocus();
        this.setLocationRelativeTo(null);
        table.setModel(listeModel);
        table.setDefaultRenderer(Object.class, new ListePatientRondu());
        table.setRowHeight(42);
        controleur = new ListePatientController(this, ctx.getPatientService(), ctx.getDossierService(),
                ctx.getSession());
        ajouterBoutons();
        controleur.rechercher("");
    }

    /**
     * Boutons de la secrétaire comme du médecin : inscrire, modifier la fiche,
     * prendre un rendez-vous. Le dossier médical ne s'ouvre que pour un médecin.
     */
    private void ajouterBoutons() {
        JButton nouveau = new JButton("Nouveau patient");
        nouveau.addActionListener(e -> inscrire());
        modifierBtn.addActionListener(e -> modifierFiche());
        rendezVousBtn.addActionListener(e -> prendreRendezVous(selection()));
        modifierBtn.setEnabled(false);
        rendezVousBtn.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            boolean une = table.getSelectedRowCount() == 1;
            modifierBtn.setEnabled(une);
            rendezVousBtn.setEnabled(une);
        });
        jPanel2.add(nouveau, 0);
        jPanel2.add(modifierBtn, 1);
        jPanel2.add(rendezVousBtn, 2);
        OuvrirBtn.setVisible(medecin);
        pack();
        setLocationRelativeTo(null);
    }

    private void inscrire() {
        FichePatient fiche = new FichePatient(this, ctx.getPatientService());
        fiche.setVisible(true);
        patient p = fiche.getPatient();
        if (p == null) {
            return;
        }
        afficherSeul(p);
        if (fiche.getSuite() == FichePatient.Suite.RENDEZ_VOUS) {
            prendreRendezVous(p);
        }
    }

    private void modifierFiche() {
        patient p = selection();
        if (p == null) {
            return;
        }
        FichePatient fiche = new FichePatient(this, ctx.getPatientService(), p);
        fiche.setVisible(true);
        if (fiche.getPatient() != null) {
            afficherSeul(fiche.getPatient());
        }
    }

    private void prendreRendezVous(patient p) {
        if (p != null) {
            new calandar(p, ctx.getSession().getUtilisateur(), ctx.getRendezVousService(), ctx.getRapportService()).setVisible(true);
        }
    }

    /** Filtre la liste sur le patient et le sélectionne. */
    private void afficherSeul(patient p) {
        choix.setText(p.getNom());
        controleur.rechercher(p.getNom());
        for (int i = 0; i < listeModel.getRowCount(); i++) {
            if (listeModel.getElement(i).getIdentPat() == p.getIdentPat()) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
            }
        }
    }

    private patient selection() {
        int ligne = table.getSelectedRow();
        return ligne < 0 ? null : listeModel.getElement(table.convertRowIndexToModel(ligne));
    }

    // ----- ListePatientVue -----

    @Override
    public void afficherPatients(List<patient> patients) {
        listeModel.setDonnees(patients);
        nbLab.setText(String.valueOf(listeModel.getRowCount()));
        OuvrirBtn.setEnabled(false);
    }

    @Override
    public void afficherDossier(DossierPatient dossier) {
        new DossierPatientForm(dossier, ctx).setVisible(true);
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

        Center = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        North = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        choix = new javax.swing.JTextField();
        South = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        nbLab = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        AnnulerBtn = new javax.swing.JButton();
        OuvrirBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Liste des Patients");
        setResizable(false);

        Center.setLayout(new java.awt.BorderLayout());

        table.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        table.setFont(new java.awt.Font("Arial", 0, 36)); // NOI18N
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        table.setRowHeight(70);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tableMousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(table);

        Center.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(Center, java.awt.BorderLayout.CENTER);

        North.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        North.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel1.setText("Rechercher par Nom:");
        North.add(jLabel1);

        choix.setPreferredSize(new java.awt.Dimension(400, 30));
        choix.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                choixKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                choixKeyTyped(evt);
            }
        });
        North.add(choix);

        getContentPane().add(North, java.awt.BorderLayout.NORTH);

        South.setLayout(new javax.swing.BoxLayout(South, javax.swing.BoxLayout.LINE_AXIS));

        jLabel2.setFont(new java.awt.Font("Goudy Old Style", 1, 18)); // NOI18N
        jLabel2.setText("Nombre Total :");

        nbLab.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        nbLab.setForeground(new java.awt.Color(153, 0, 0));
        nbLab.setText("NB");

        jLabel3.setFont(new java.awt.Font("Goudy Old Style", 1, 18)); // NOI18N
        jLabel3.setText("Patient(s)");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jLabel2)
                .addGap(5, 5, 5)
                .addComponent(nbLab)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel2))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nbLab)
                    .addComponent(jLabel3)))
        );

        South.add(jPanel1);

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        AnnulerBtn.setText("Annuler");
        AnnulerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AnnulerBtnActionPerformed(evt);
            }
        });
        jPanel2.add(AnnulerBtn);

        OuvrirBtn.setText("Ouvrir Dossier");
        OuvrirBtn.setEnabled(false);
        OuvrirBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OuvrirBtnActionPerformed(evt);
            }
        });
        jPanel2.add(OuvrirBtn);

        South.add(jPanel2);

        getContentPane().add(South, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void AnnulerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AnnulerBtnActionPerformed
     this.dispose();
    }//GEN-LAST:event_AnnulerBtnActionPerformed

    private void choixKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_choixKeyTyped
       
   
        
    }//GEN-LAST:event_choixKeyTyped

    private void choixKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_choixKeyReleased
        controleur.rechercher(choix.getText());
        
    }//GEN-LAST:event_choixKeyReleased

    private void OuvrirBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OuvrirBtnActionPerformed
        if (ouvrirDossier()) {
            this.dispose();
        }
    }//GEN-LAST:event_OuvrirBtnActionPerformed

    private void tableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMousePressed
       if(table.getSelectedRowCount()==1)
           this.OuvrirBtn.setEnabled(true);
    }//GEN-LAST:event_tableMousePressed

    private void tableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseClicked
      if (evt.getClickCount() == 2) {
          if (!medecin) {
              modifierFiche();
          } else if (ouvrirDossier()) {
              this.dispose();
          }
      }
    }//GEN-LAST:event_tableMouseClicked

    
    
    private boolean ouvrirDossier() {
        int ligne = table.getSelectedRow();
        if (ligne < 0) {
            return false;
        }
        return controleur.ouvrirDossier(listeModel.getElement(table.convertRowIndexToModel(ligne)));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AnnulerBtn;
    private javax.swing.JPanel Center;
    private javax.swing.JPanel North;
    private javax.swing.JButton OuvrirBtn;
    private javax.swing.JPanel South;
    private javax.swing.JTextField choix;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel nbLab;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
