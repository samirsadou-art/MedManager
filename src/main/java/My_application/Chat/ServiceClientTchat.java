
package My_application.Chat;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;


public class ServiceClientTchat extends Thread{

    private static final Logger LOG = Logger.getLogger(ServiceClientTchat.class.getName());

    volatile Socket        Sock;
    DataInputStream   input;
    volatile DataOutputStream output;
    int port=7777;
    String host="127.0.0.1";
    volatile boolean connected=false;
    boolean paused=false;
    Descussion disc;

    public ServiceClientTchat(Descussion disc) {
        this.disc=disc;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            Sock=new Socket(host, port);
            input=new DataInputStream(Sock.getInputStream());
            output=new DataOutputStream(Sock.getOutputStream());
            connected=true;
            while(connected){
                final String str=GetMessage();
                if (str == null || ServiceTchatServeur.FIN.equals(str)) {
                    // Le serveur est parti : inutile de lui renvoyer FIN.
                    connected=false;
                    break;
                }
                LOG.fine("Message reçu du serveur : " + str);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        disc.sendMessage(str);
                    }
                });
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Connexion au serveur de discussion " + host + ":" + port + " impossible", ex);
        } finally {
            fermerTout();
        }
    }

    public void SendMessage(String message) {
        DataOutputStream out = output;
        if (!connected || out == null) {
            LOG.fine("Non connecté au serveur : message non envoyé.");
            return;
        }
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Envoi du message impossible", ex);
        }
    }

    /** Message reçu, ou null si la connexion est perdue. */
    public String GetMessage(){
        try {
            return input.readUTF();
        } catch (IOException ex) {
            if (connected) {
                LOG.log(Level.FINE, "Connexion de discussion interrompue", ex);
            }
            return null;
        }
    }

    public void startService(){
        this.start();
    }

    public void stopService(){
        if (connected) {
            SendMessage(ServiceTchatServeur.FIN);
        }
        connected=false;
        fermerTout();
    }

    /** Ferme tout ce qui a été ouvert ; peut être appelé plusieurs fois. */
    private void fermerTout() {
        fermer(output);
        fermer(input);
        fermer(Sock);
    }

    private static void fermer(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException ex) {
            LOG.log(Level.FINE, "Fermeture impossible", ex);
        }
    }

}
