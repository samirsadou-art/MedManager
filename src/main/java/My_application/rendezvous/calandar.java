/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.rendezvous;

import My_application.vue.Theme;
import My_application.controller.RendezVousController;
import My_application.controller.Vue;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.patient;
import My_application.service.RendezVousService;
import My_application.vue.Dialogues;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.table.JTableHeader;

/**
 * Agenda hebdomadaire (Vue).
 * <p>
 * Les deux gestionnaires de clic contenaient chacun ≈ 150 lignes de calcul de
 * créneau et une insertion SQL. Ce calcul et les règles (créneau complet,
 * un seul rendez-vous par heure et par patient, pas de rendez-vous dans le
 * passé) sont maintenant dans {@link RendezVousService}, via
 * {@link RendezVousController}. Autre correction : le rendez-vous n'était
 * ajouté à l'écran qu'avant l'insertion en base, donc affiché même si
 * l'insertion échouait ; l'agenda est désormais rechargé depuis la base.
 */
public class calandar extends javax.swing.JFrame implements Vue {

    private final RendezVousController controleur;
    private final RendezVousTableModel model;
    private LocalDate jourCourant;
    private int nombreJour = 0;

    public calandar(patient Patient, User utilisateur, RendezVousService service) {
        initComponents();
        Theme.harmoniser(this);
        this.setExtendedState(MAXIMIZED_BOTH);
        controleur = new RendezVousController(this, service, Patient, utilisateur);
        model = new RendezVousTableModel(service.getMaxParHeure(), service.getHeureOuverture(),
                service.getNombreHeures());
        table.setModel(model);
        table.setDefaultRenderer(Object.class, new RendezVousRondu(Patient, model.getMaxParH()));
        JTableHeader th = table.getTableHeader();
        th.setDefaultRenderer(new HeaderRondu());
        jourCourant = service.aujourdhui();
        rafraichir();
    }

    /** Recharge la semaine contenant le jour courant. */
    void rafraichir() {
        LocalDate dimanche = RendezVousService.dimancheDeLaSemaine(jourCourant);
        model.afficherSemaine(dimanche, controleur.semaine(dimanche));
        table.getColumnModel().getColumn(0).setMaxWidth(50);
    }

    private void decaler(int jours) {
        nombreJour += jours;
        nbrJourLab.setText(nombreJour + " Jours");
        jourCourant = jourCourant.plusDays(jours);
        rafraichir();
    }

    private void afficherApercu(Rendez_Vous rendez) {
        long reste = ChronoUnit.DAYS.between(controleur.getService().aujourdhui(), rendez.getDate().toLocalDate());
        restTxt.setText(String.valueOf(reste));
        timeTxt.setText(rendez.getHeureString());
        dateText.setText(rendez.getDateString());
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

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jPanel2 = new javax.swing.JPanel();
        validerBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        deparBtn = new javax.swing.JButton();
        prevBtn = new javax.swing.JButton();
        jSpinner1 = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        nextBtn = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        nbrJourLab = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        dateText = new javax.swing.JTextField();
        timeTxt = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        restTxt = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();

        jMenuItem1.setText("Voir Détails");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jMenuItem1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setType(java.awt.Window.Type.UTILITY);

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        validerBtn.setText("Valider");
        validerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validerBtnActionPerformed(evt);
            }
        });
        jPanel2.add(validerBtn);

        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_END);

        table.setBackground(new java.awt.Color(0, 204, 204));
        table.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Jours/Temps", "Samedi", "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi"
            }
        ));
        table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setFillsViewportHeight(true);
        table.setGridColor(new java.awt.Color(0, 0, 0));
        table.setRowHeight(85);
        table.setRowSelectionAllowed(false);
        table.setSelectionBackground(new java.awt.Color(255, 255, 255));
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tableMousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(table);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        deparBtn.setText("Réinitialiser");
        deparBtn.setPreferredSize(new java.awt.Dimension(100, 40));
        deparBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deparBtnActionPerformed(evt);
            }
        });
        jPanel3.add(deparBtn);

        prevBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/previous32.png"))); // NOI18N
        prevBtn.setText("Reculer");
        prevBtn.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        prevBtn.setPreferredSize(new java.awt.Dimension(120, 40));
        prevBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prevBtnActionPerformed(evt);
            }
        });
        jPanel3.add(prevBtn);

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(5, 0, null, 5));
        jSpinner1.setPreferredSize(new java.awt.Dimension(80, 30));
        jPanel3.add(jSpinner1);

        jLabel2.setFont(new java.awt.Font("Goudy Old Style", 1, 18)); // NOI18N
        jLabel2.setText("Jours");
        jPanel3.add(jLabel2);

        nextBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/next32.png"))); // NOI18N
        nextBtn.setText("Avancer");
        nextBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        nextBtn.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        nextBtn.setIconTextGap(2);
        nextBtn.setPreferredSize(new java.awt.Dimension(120, 40));
        nextBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextBtnActionPerformed(evt);
            }
        });
        jPanel3.add(nextBtn);

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jPanel1.setMinimumSize(new java.awt.Dimension(62, 35));
        jPanel1.setPreferredSize(new java.awt.Dimension(80, 40));
        jPanel1.setLayout(new java.awt.BorderLayout(5, 0));

        nbrJourLab.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        nbrJourLab.setForeground(new java.awt.Color(153, 0, 0));
        nbrJourLab.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        nbrJourLab.setText("0 Jours");
        jPanel1.add(nbrJourLab, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel1);

        jLabel1.setFont(new java.awt.Font("Goudy Old Style", 1, 18)); // NOI18N
        jLabel1.setText("Rendez-vous fixé pour le :");
        jLabel1.setPreferredSize(new java.awt.Dimension(200, 35));
        jPanel3.add(jLabel1);

        dateText.setBackground(new java.awt.Color(0, 0, 0));
        dateText.setFont(new java.awt.Font("Goudy Old Style", 1, 24)); // NOI18N
        dateText.setForeground(new java.awt.Color(255, 0, 51));
        dateText.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        dateText.setPreferredSize(new java.awt.Dimension(290, 35));
        jPanel3.add(dateText);

        timeTxt.setBackground(new java.awt.Color(0, 0, 0));
        timeTxt.setFont(new java.awt.Font("Goudy Old Style", 1, 24)); // NOI18N
        timeTxt.setForeground(new java.awt.Color(255, 0, 51));
        timeTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        timeTxt.setPreferredSize(new java.awt.Dimension(100, 35));
        jPanel3.add(timeTxt);

        jLabel3.setFont(new java.awt.Font("Goudy Old Style", 1, 18)); // NOI18N
        jLabel3.setText("Reste :");
        jPanel3.add(jLabel3);

        restTxt.setEditable(false);
        restTxt.setBackground(new java.awt.Color(0, 0, 0));
        restTxt.setFont(new java.awt.Font("Goudy Old Style", 1, 24)); // NOI18N
        restTxt.setForeground(new java.awt.Color(255, 0, 0));
        restTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        restTxt.setPreferredSize(new java.awt.Dimension(60, 35));
        jPanel3.add(restTxt);

        jLabel4.setFont(new java.awt.Font("Goudy Old Style", 1, 18)); // NOI18N
        jLabel4.setText("jour(s)");
        jPanel3.add(jLabel4);

        getContentPane().add(jPanel3, java.awt.BorderLayout.PAGE_START);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void validerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validerBtnActionPerformed

     this.dispose();


    }//GEN-LAST:event_validerBtnActionPerformed

    private void tableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMousePressed
        int row = table.rowAtPoint(evt.getPoint());
        int col = table.columnAtPoint(evt.getPoint());
        if (row < 0 || col < 1) {
            return;
        }
        if (evt.getButton() == MouseEvent.BUTTON1) {
            // Simple clic : aperçu du créneau qui serait attribué.
            Rendez_Vous rendez = controleur.apercu(model.jourDeColonne(col), model.heureDeLigne(row));
            if (rendez != null) {
                afficherApercu(rendez);
            }
        } else {
            // Clic droit : détail des rendez-vous de l'heure.
            DefaultListModel<Rendez_Vous> liste = model.getListModel(row, col);
            if (liste != null) {
                DetailRendez dr = new DetailRendez(liste, model.getMaxParH(), controleur, new Runnable() {
                    @Override
                    public void run() {
                        rafraichir();
                    }
                });
                dr.setVisible(true);
            }
        }
    }//GEN-LAST:event_tableMousePressed

    private void nextBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextBtnActionPerformed
        decaler((Integer) jSpinner1.getValue());
    }//GEN-LAST:event_nextBtnActionPerformed

    private void prevBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prevBtnActionPerformed
        decaler(-(Integer) jSpinner1.getValue());
    }//GEN-LAST:event_prevBtnActionPerformed

    private void deparBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deparBtnActionPerformed
        jourCourant = controleur.getService().aujourdhui();
        nombreJour = 0;
        nbrJourLab.setText("0 Jours");
        restTxt.setText("");
        dateText.setText("");
        timeTxt.setText("");
        rafraichir();
    }//GEN-LAST:event_deparBtnActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
       
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void tableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseClicked
        if (evt.getClickCount() != 2 || evt.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        int row = table.rowAtPoint(evt.getPoint());
        int col = table.columnAtPoint(evt.getPoint());
        if (row < 0 || col < 1) {
            return;
        }
        Rendez_Vous rendez = controleur.proposer(model.jourDeColonne(col), model.heureDeLigne(row));
        if (rendez == null) {
            return; // la règle refusée a déjà été affichée
        }
        afficherApercu(rendez);
        JLabel label = new JLabel("<html>Veuillez confirmer le rendez-vous suivant :<br>Patient : <b>"
                + rendez.getPatient().getNom() + "  " + rendez.getPatient().getPrenom()
                + "</b><br>Date : <b>" + rendez.getDateString() + "</b><br>Heure : <b>" + rendez.getHeureString()
                + "</b><br>Reste : <font color=\"#C62828\"><b>" + restTxt.getText() + " jour(s)</b></font></html>");
        label.setFont(Theme.police(Font.PLAIN, Theme.TAILLE_BASE + 1));
        int result = JOptionPane.showConfirmDialog(this, label, "Confirmation du rendez-vous ?",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION && controleur.confirmer(rendez)) {
            rafraichir();
        }
    }//GEN-LAST:event_tableMouseClicked

    


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField dateText;
    private javax.swing.JButton deparBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JLabel nbrJourLab;
    private javax.swing.JButton nextBtn;
    private javax.swing.JButton prevBtn;
    private javax.swing.JTextField restTxt;
    private javax.swing.JTable table;
    private javax.swing.JTextField timeTxt;
    private javax.swing.JButton validerBtn;
    // End of variables declaration//GEN-END:variables
}
