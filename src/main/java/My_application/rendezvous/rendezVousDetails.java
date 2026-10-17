/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.rendezvous;

import My_application.vue.Theme;
import My_application.vue.Icones;import My_application.entities.Rendez_Vous;


public class rendezVousDetails extends javax.swing.JDialog {
  
    public rendezVousDetails(Rendez_Vous rendez) {
        initComponents();
        Theme.harmoniser(this);
        this.setLocationRelativeTo(null);
        this.setModal(true);
        
        this.nomPatTxt.setText(rendez.getPatient().getNom());
        this.prenomTxt.setText(rendez.getPatient().getPrenom());
 
        this.ageTxt.setText(rendez.getPatient().getStringAge());
        this.nomRespTxt.setText(rendez.getUtilsateur().getNom());
        this.prenomRespTxt.setText(rendez.getUtilsateur().getPrenom());
        this.qualiteRespTxt.setText(rendez.getUtilsateur().getQuailite());
        this.dateRdvTxt.setText(rendez.getDateFixString());
        this.heureRdvTxt.setText(rendez.getHeureFixString());
        this.jourRdvTxt.setText(rendez.getDateString());
        this.timeRdvTxt.setText(rendez.getHeureString());
        if(rendez.getNombreJoursRestant()<0)
        this.restejourTxt.setText("Passé il ya : "+(-rendez.getNombreJoursRestant())+" jour(s)");
        else
         this.restejourTxt.setText(rendez.getNombreJoursRestant()+" jour(s)");
      
        

        
     
  
        this.imglab.setIcon(Icones.patient(rendez.getPatient(), 128));
        
    
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        imglab = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        nomPatTxt = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        prenomTxt = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        ageTxt = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        nomRespTxt = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        prenomRespTxt = new javax.swing.JTextField();
        qualiteRespTxt = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        dateRdvTxt = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        heureRdvTxt = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jourRdvTxt = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        timeRdvTxt = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        restejourTxt = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        AnnulerBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Détails Rendez Vous");
        setResizable(false);
        setType(java.awt.Window.Type.UTILITY);

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Patient"));
        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel6.setLayout(new java.awt.BorderLayout());

        imglab.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel6.add(imglab, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel6, java.awt.BorderLayout.EAST);

        jLabel2.setText("Nom:");

        nomPatTxt.setEditable(false);
        nomPatTxt.setFont(new java.awt.Font("Goudy Old Style", 0, 14)); // NOI18N

        jLabel3.setText("Prénom:");

        prenomTxt.setEditable(false);
        prenomTxt.setFont(new java.awt.Font("Goudy Old Style", 0, 14)); // NOI18N

        jLabel12.setText("Age:");

        ageTxt.setEditable(false);
        ageTxt.setFont(new java.awt.Font("Goudy Old Style", 0, 14)); // NOI18N

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel12)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2))
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(prenomTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ageTxt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nomPatTxt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nomPatTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(24, 24, 24)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(prenomTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(22, 22, 22)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ageTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.add(jPanel7, java.awt.BorderLayout.LINE_START);

        jPanel1.add(jPanel3);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Responsable"));

        jLabel5.setText("Nom:");

        nomRespTxt.setEditable(false);
        nomRespTxt.setFont(new java.awt.Font("Goudy Old Style", 0, 14)); // NOI18N

        jLabel6.setText("Prénom:");

        prenomRespTxt.setEditable(false);
        prenomRespTxt.setFont(new java.awt.Font("Goudy Old Style", 0, 14)); // NOI18N

        qualiteRespTxt.setEditable(false);
        qualiteRespTxt.setFont(new java.awt.Font("Goudy Old Style", 0, 14)); // NOI18N

        jLabel7.setText("Qualité:");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(prenomRespTxt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE)
                    .addComponent(qualiteRespTxt)
                    .addComponent(nomRespTxt))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(nomRespTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(prenomRespTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(qualiteRespTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel4);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Dates"));

        jLabel8.setText("Date: ");

        dateRdvTxt.setEditable(false);
        dateRdvTxt.setFont(new java.awt.Font("Goudy Old Style", 0, 14)); // NOI18N
        dateRdvTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel9.setText("Heure:");

        heureRdvTxt.setEditable(false);
        heureRdvTxt.setFont(new java.awt.Font("Goudy Old Style", 0, 14)); // NOI18N
        heureRdvTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel10.setText("Rendez-vous :");

        jourRdvTxt.setEditable(false);
        jourRdvTxt.setFont(new java.awt.Font("Goudy Old Style", 1, 14)); // NOI18N
        jourRdvTxt.setForeground(new java.awt.Color(0, 51, 255));
        jourRdvTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel11.setText("à : ");

        timeRdvTxt.setEditable(false);
        timeRdvTxt.setFont(new java.awt.Font("Goudy Old Style", 1, 14)); // NOI18N
        timeRdvTxt.setForeground(new java.awt.Color(0, 51, 255));
        timeRdvTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel13.setText("Le nombre de jours restants est :");

        restejourTxt.setEditable(false);
        restejourTxt.setFont(new java.awt.Font("Goudy Old Style", 1, 18)); // NOI18N
        restejourTxt.setForeground(new java.awt.Color(204, 0, 0));
        restejourTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10)
                            .addComponent(jLabel8))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jourRdvTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                            .addComponent(dateRdvTxt))
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel9)
                                .addGap(18, 18, 18))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(timeRdvTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                            .addComponent(heureRdvTxt)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(restejourTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(heureRdvTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(dateRdvTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(13, 13, 13)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jourRdvTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(timeRdvTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(restejourTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel1.add(jPanel5);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        AnnulerBtn.setText("OK");
        AnnulerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AnnulerBtnActionPerformed(evt);
            }
        });
        jPanel2.add(AnnulerBtn);

        getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void AnnulerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AnnulerBtnActionPerformed
       this.dispose();
    }//GEN-LAST:event_AnnulerBtnActionPerformed

 
        
        
    


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AnnulerBtn;
    private javax.swing.JTextField ageTxt;
    private javax.swing.JTextField dateRdvTxt;
    private javax.swing.JTextField heureRdvTxt;
    private javax.swing.JLabel imglab;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JTextField jourRdvTxt;
    private javax.swing.JTextField nomPatTxt;
    private javax.swing.JTextField nomRespTxt;
    private javax.swing.JTextField prenomRespTxt;
    private javax.swing.JTextField prenomTxt;
    private javax.swing.JTextField qualiteRespTxt;
    private javax.swing.JTextField restejourTxt;
    private javax.swing.JTextField timeRdvTxt;
    // End of variables declaration//GEN-END:variables
}
