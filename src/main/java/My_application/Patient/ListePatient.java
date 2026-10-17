/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.Patient;

import My_application.vue.Theme;
import My_application.AppContext;
import My_application.DossierPatientForm;
import My_application.controller.ListePatientController;
import My_application.controller.ListePatientVue;
import My_application.entities.DossierPatient;
import My_application.entities.patient;
import My_application.vue.Dialogues;
import java.util.List;
import javax.swing.JButton;

/**
 * Liste des patients (Vue). La recherche et l'ouverture du dossier sont
 * déléguées à {@link ListePatientController} ; plus de SQL dans la fenêtre.
 */
public class ListePatient extends javax.swing.JDialog implements ListePatientVue {

    private final ListePatientTableModel listeModel = new ListePatientTableModel();
    private final ListePatientController controleur;

    public ListePatient() {
        initComponents();
        Theme.harmoniser(this);
        this.choix.requestFocus();
        this.setLocationRelativeTo(null);
        table.setModel(listeModel);
        table.setDefaultRenderer(Object.class, new ListePatientRondu());
        table.setRowHeight(42);
        AppContext ctx = AppContext.get();
        controleur = new ListePatientController(this, ctx.getPatientService(), ctx.getDossierService(),
                ctx.getSession());
        ajouterBoutonNouveauPatient();
        controleur.rechercher("");
    }

    /** Le formulaire NewPatient existait mais n'était accessible depuis aucun écran. */
    private void ajouterBoutonNouveauPatient() {
        JButton nouveau = new JButton("Nouveau patient");
        nouveau.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                NewPatient fiche = new NewPatient(ListePatient.this);
                fiche.setVisible(true);
                patient cree = fiche.getPatientCree();
                if (cree != null) {
                    choix.setText(cree.getNom());
                    controleur.rechercher(cree.getNom());
                }
            }
        });
        jPanel2.add(nouveau, 0);
        pack();
        setLocationRelativeTo(null);
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
        new DossierPatientForm(dossier).setVisible(true);
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
      if(evt.getClickCount()==2){
          
        if (ouvrirDossier()) {
            this.dispose();
        }
          
          
      }
    }//GEN-LAST:event_tableMouseClicked

    
    
    private boolean ouvrirDossier() {
        int ligne = table.getSelectedRow();
        if (ligne < 0) {
            return false;
        }
        return controleur.ouvrirDossier(listeModel.getElement(ligne));
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
