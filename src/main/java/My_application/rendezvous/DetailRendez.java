/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.rendezvous;

import My_application.vue.Theme;
import My_application.controller.RendezVousController;
import My_application.entities.Rendez_Vous;
import java.awt.Color;
import java.awt.Font;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * Rendez-vous d'une heure de l'agenda (Vue) : détail et annulation.
 * L'annulation passe par {@link RendezVousController}, qui ne supprime plus
 * que le rendez-vous du patient concerné.
 */
public class DetailRendez extends javax.swing.JDialog {

    private final DefaultListModel<Rendez_Vous> model;
    private final RendezVousController controleur;
    private final Runnable rafraichirAgenda;

    public DetailRendez(DefaultListModel<Rendez_Vous> model, int maxParHeure, RendezVousController controleur,
            Runnable rafraichirAgenda) {
        initComponents();
        Theme.harmoniser(this);
        this.setLocationRelativeTo(null);
        this.setModal(true);
        this.DetailBtn.setEnabled(false);
        this.model = model;
        this.controleur = controleur;
        this.rafraichirAgenda = rafraichirAgenda;
        this.listRendezVous.setModel(model);
        this.listRendezVous.setCellRenderer(new listRendezVousRondu(1, controleur.getPatient()));
        if (model.size() == maxParHeure) {
            listRendezVous.setBackground(Theme.DANGER_CLAIR);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        AnnulerBtn = new javax.swing.JButton();
        annulerRdvBtn = new javax.swing.JButton();
        DetailBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listRendezVous = new javax.swing.JList<>();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Liste Rendez Vous");
        setPreferredSize(new java.awt.Dimension(400, 400));
        setResizable(false);
        setType(java.awt.Window.Type.UTILITY);

        AnnulerBtn.setText("Fermer");
        AnnulerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AnnulerBtnActionPerformed(evt);
            }
        });
        jPanel1.add(AnnulerBtn);

        annulerRdvBtn.setText("Annuler RDV");
        annulerRdvBtn.setEnabled(false);
        annulerRdvBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annulerRdvBtnActionPerformed(evt);
            }
        });
        jPanel1.add(annulerRdvBtn);

        DetailBtn.setText("Voir détails");
        DetailBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DetailBtnActionPerformed(evt);
            }
        });
        jPanel1.add(DetailBtn);

        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);

        jPanel2.setLayout(new java.awt.BorderLayout());

        listRendezVous.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        listRendezVous.setFont(new java.awt.Font("Goudy Old Style", 1, 20)); // NOI18N
        listRendezVous.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listRendezVous.setVisibleRowCount(4);
        listRendezVous.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                listRendezVousMousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(listRendezVous);

        jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel1.setText("Choisir un rendez vous pour voir les détails ");
        jPanel3.add(jLabel1);

        getContentPane().add(jPanel3, java.awt.BorderLayout.PAGE_START);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void AnnulerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AnnulerBtnActionPerformed
       this.dispose();
    }//GEN-LAST:event_AnnulerBtnActionPerformed

    private void DetailBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DetailBtnActionPerformed
        int index = listRendezVous.getSelectedIndex();
        if (index >= 0) {
            new rendezVousDetails(model.getElementAt(index)).setVisible(true);
        }
    }//GEN-LAST:event_DetailBtnActionPerformed

    private void listRendezVousMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listRendezVousMousePressed
        int index = listRendezVous.getSelectedIndex();
        if (index >= 0) {
            this.DetailBtn.setEnabled(true);
            this.annulerRdvBtn.setEnabled(controleur.peutAnnuler(model.get(index)));
        }
    }//GEN-LAST:event_listRendezVousMousePressed

    private void annulerRdvBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annulerRdvBtnActionPerformed
        int index = listRendezVous.getSelectedIndex();
        if (index < 0) {
            return;
        }
        Rendez_Vous rendez = model.get(index);
        JLabel label = new JLabel("<html>Voulez-vous annuler le rendez-vous de : <br> Patient : <b>"
                + rendez.getPatient().getNom() + "  " + rendez.getPatient().getPrenom() + "  </b></html>");
        label.setFont(Theme.police(Font.PLAIN, Theme.TAILLE_BASE + 1));
        int result = JOptionPane.showConfirmDialog(this, label, "Annulation du rendez-vous ...",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            if (controleur.annuler(rendez)) {
                model.remove(index);
                listRendezVous.setBackground(Theme.SURFACE);
                rafraichirAgenda.run();
            }
        } else {
            listRendezVous.clearSelection();
        }
        this.annulerRdvBtn.setEnabled(false);
        this.DetailBtn.setEnabled(false);
    }//GEN-LAST:event_annulerRdvBtnActionPerformed



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AnnulerBtn;
    private javax.swing.JButton DetailBtn;
    private javax.swing.JButton annulerRdvBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<My_application.entities.Rendez_Vous> listRendezVous;
    // End of variables declaration//GEN-END:variables
}
