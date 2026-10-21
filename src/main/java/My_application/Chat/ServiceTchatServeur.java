
package My_application.Chat;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;


public class ServiceTchatServeur extends Thread {

    private static final Logger LOG = Logger.getLogger(ServiceTchatServeur.class.getName());
    /** Message de fin de discussion échangé entre le serveur et le client. */
    static final String FIN = "EXIT";

    volatile ServerSocket serverSock;
    volatile Socket        Sock;
    DataInputStream   input;
    volatile DataOutputStream output;
    final static int port=7777;
    volatile boolean connected=false;
    boolean paused=false;
    Tchat TchatWindow;

    public ServiceTchatServeur(Tchat WTchat) {
        this.TchatWindow=WTchat;
        // Ne doit pas empêcher la JVM de s'arrêter à la fermeture de l'application.
        setDaemon(true);
    }

    @Override
    public void run() {
        LOG.fine("Serveur de discussion en attente sur le port " + port);
        try {
            // Boucle locale uniquement : le client se connecte à 127.0.0.1, et le
            // serveur (sans authentification ni chiffrement) ne doit pas être
            // joignable depuis le réseau du cabinet.
            serverSock= new ServerSocket(port, 50, InetAddress.getLoopbackAddress());
            Sock=serverSock.accept();
            LOG.fine("Connexion établie par : " + Sock.getInetAddress().getHostName());
            input= new DataInputStream(Sock.getInputStream());
            output=new DataOutputStream(Sock.getOutputStream());
            connected=true;
            while(connected){
                final String msg=GetMessage();
                if (msg == null || FIN.equals(msg)) {
                    // Le client est parti : inutile de lui renvoyer FIN.
                    connected=false;
                    break;
                }
                LOG.fine(msg);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        TchatWindow.sendMessage(msg);
                    }
                });
            }
        } catch (BindException ex) {
            LOG.log(Level.WARNING, "Port " + port + " occupé (autre instance de GestiMed ?) : discussion désactivée.", ex);
        } catch (IOException ex) {
            if (serverSock == null || !serverSock.isClosed()) {
                LOG.log(Level.SEVERE, "Erreur du serveur de discussion", ex);
            }
            // sinon : accept() interrompu par stopService(), fermeture normale
        } finally {
            fermerTout();
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

    public void SendMessage(String message){
        DataOutputStream out = output;
        if (!connected || out == null) {
            LOG.fine("Aucun interlocuteur connecté : message non envoyé.");
            return;
        }
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Envoi du message impossible", ex);
        }
    }

    public void stopService(){
        if (connected) {
            SendMessage(FIN);
        }
        connected=false;
        fermerTout();
    }

    /** Ferme tout ce qui a été ouvert ; peut être appelé plusieurs fois. */
    private void fermerTout() {
        fermer(output);
        fermer(input);
        fermer(Sock);
        // Fermer le ServerSocket débloque accept() si aucun client n'est venu.
        fermer(serverSock);
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

    public void startService(){
        this.start();
    }

}
