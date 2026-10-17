
package My_application.Chat;

import My_application.vue.Theme;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;


public class Tchat extends javax.swing.JFrame {
    
    final static int AUT_SERV=0;
    final static int AUT_CLI=1;
    
    Component princ=null;
    ServiceTchatServeur serviceTchat;


    public Tchat(Component princ) {
        this.princ=princ;
        this.serviceTchat=new ServiceTchatServeur(this);
        this.serviceTchat.startService();
        initComponents();
        Theme.harmoniser(this);
        ActivBtn.setEnabled(false);
        messageTxt.requestFocus();
        
       setIconImage(Toolkit.getDefaultToolkit().getImage("chat32.png"));

 
 
    }

    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        messageTxt = new javax.swing.JTextField();
        EnvoyerBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        SonnerBtn = new javax.swing.JButton();
        supprimerBtn = new javax.swing.JButton();
        ActivBtn = new javax.swing.JButton();
        DisactivBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        ListMsgTxt = new javax.swing.JTextArea();

        setTitle("Discussion Instantanée");
        setPreferredSize(new java.awt.Dimension(340, 350));
        setResizable(false);
        setType(java.awt.Window.Type.UTILITY);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 5));

        messageTxt.setFont(new java.awt.Font("Iskoola Pota", 2, 18)); // NOI18N
        messageTxt.setForeground(new java.awt.Color(0, 0, 51));
        messageTxt.setPreferredSize(new java.awt.Dimension(250, 40));
        messageTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                messageTxtActionPerformed(evt);
            }
        });
        jPanel1.add(messageTxt);

        EnvoyerBtn.setText("Envoyer");
        EnvoyerBtn.setPreferredSize(new java.awt.Dimension(73, 40));
        EnvoyerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnvoyerBtnActionPerformed(evt);
            }
        });
        jPanel1.add(EnvoyerBtn);

        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        SonnerBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/Bell.png"))); // NOI18N
        SonnerBtn.setToolTipText("Envoyer un signal sonnore");
        SonnerBtn.setPreferredSize(new java.awt.Dimension(40, 40));
        SonnerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SonnerBtnActionPerformed(evt);
            }
        });
        jPanel2.add(SonnerBtn);

        supprimerBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/corbeilleBlue.png"))); // NOI18N
        supprimerBtn.setToolTipText("Supprimer tout");
        supprimerBtn.setPreferredSize(new java.awt.Dimension(40, 40));
        supprimerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                supprimerBtnActionPerformed(evt);
            }
        });
        jPanel2.add(supprimerBtn);

        ActivBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/ActivChat32.png"))); // NOI18N
        ActivBtn.setToolTipText("Activer le Tchat");
        ActivBtn.setPreferredSize(new java.awt.Dimension(40, 40));
        ActivBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ActivBtnActionPerformed(evt);
            }
        });
        jPanel2.add(ActivBtn);

        DisactivBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/icons/stop.png"))); // NOI18N
        DisactivBtn.setToolTipText("Disactiver le Tchat");
        DisactivBtn.setPreferredSize(new java.awt.Dimension(40, 40));
        DisactivBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DisactivBtnActionPerformed(evt);
            }
        });
        jPanel2.add(DisactivBtn);

        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_START);

        ListMsgTxt.setEditable(false);
        ListMsgTxt.setColumns(20);
        ListMsgTxt.setFont(new java.awt.Font("Goudy Old Style", 0, 24)); // NOI18N
        ListMsgTxt.setForeground(new java.awt.Color(0, 51, 51));
        ListMsgTxt.setLineWrap(true);
        ListMsgTxt.setRows(5);
        ListMsgTxt.setWrapStyleWord(true);
        ListMsgTxt.setSelectionColor(new java.awt.Color(153, 153, 255));
        jScrollPane1.setViewportView(ListMsgTxt);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void EnvoyerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnvoyerBtnActionPerformed
      
        addMsg(0);
    }//GEN-LAST:event_EnvoyerBtnActionPerformed

    private void messageTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_messageTxtActionPerformed
       addMsg(0);
    }//GEN-LAST:event_messageTxtActionPerformed

    private void SonnerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SonnerBtnActionPerformed
       playSound("call.wav");
    }//GEN-LAST:event_SonnerBtnActionPerformed

    private void DisactivBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DisactivBtnActionPerformed
        
        ActivBtn.setEnabled(true);
        SonnerBtn.setEnabled(false);
        supprimerBtn.setEnabled(false);
        DisactivBtn.setEnabled(false);
        ListMsgTxt.setEnabled(false);
        EnvoyerBtn.setEnabled(false);
        messageTxt.setEnabled(false);
        messageTxt.setText("");
    }//GEN-LAST:event_DisactivBtnActionPerformed

    private void ActivBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ActivBtnActionPerformed
        ActivBtn.setEnabled(false);
        SonnerBtn.setEnabled(true);
        supprimerBtn.setEnabled(true);
        DisactivBtn.setEnabled(true);
        ListMsgTxt.setEnabled(true);
        EnvoyerBtn.setEnabled(true);
        messageTxt.setEnabled(true);
    }//GEN-LAST:event_ActivBtnActionPerformed

    private void supprimerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_supprimerBtnActionPerformed
       ListMsgTxt.setText("");
    }//GEN-LAST:event_supprimerBtnActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated

    }//GEN-LAST:event_formWindowActivated

    private void addMsg(int Auteur){
        
        
       if(!messageTxt.getText().isEmpty()){
           
       addEnteteMsg(Auteur);
     
       ListMsgTxt.append(messageTxt.getText()+"\n");
       serviceTchat.SendMessage(messageTxt.getText());
       messageTxt.setText("");
     
       
       }
       
         messageTxt.requestFocus(); 
    }
    
    private void addEnteteMsg(int Auteur){

        SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm");
        String Source="Moi";
        if(Auteur==AUT_CLI)
            Source="Toi";
  
        ListMsgTxt.append("["+Source+"("+dt1.format(new Date())+")]: ");
        
    }
    
    
     public static synchronized void playSound(final String url) {
  new Thread(new Runnable() {
  // The wrapper thread is unnecessary, unless it blocks on the
  // Clip finishing; see comments.
    public void run() {
      try {
        Clip clip = AudioSystem.getClip();
        AudioInputStream inputStream = AudioSystem.getAudioInputStream(
          new java.io.BufferedInputStream(Tchat.class.getResourceAsStream("/res/sound/" + url)));
        clip.open(inputStream);
        clip.start(); 
      } catch (Exception e) {
        System.err.println(e.getMessage());
      }
    }
  }).start();
}

    public void montrer(){
     if(princ!=null){
     Rectangle rec=princ.getBounds();
     this.setLocation((int)(rec.getX()+rec.getWidth()-this.getWidth()-15),(int)(rec.getY()+rec.getHeight()-this.getHeight()-15) );
     }
        
        
        this.setVisible(true);
    }  
    
     void sendMessage(String msg) {
     
                  
       addEnteteMsg(AUT_CLI);
     
       ListMsgTxt.append(msg+"\n");  
    }   
    
     public void Close(){
         
         this.serviceTchat.stopService();
         
     }
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ActivBtn;
    private javax.swing.JButton DisactivBtn;
    private javax.swing.JButton EnvoyerBtn;
    private javax.swing.JTextArea ListMsgTxt;
    private javax.swing.JButton SonnerBtn;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField messageTxt;
    private javax.swing.JButton supprimerBtn;
    // End of variables declaration//GEN-END:variables


}
