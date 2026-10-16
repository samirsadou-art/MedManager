
package My_application.Chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServiceTchatServeur extends Thread {
    
    ServerSocket serverSock;
    Socket        Sock;
    DataInputStream   input;
    DataOutputStream output;
    final static int port=7777;
    boolean connected=false;
    boolean paused=false;
    Tchat TchatWindow;

    public ServiceTchatServeur(Tchat WTchat) {
        this.TchatWindow=WTchat;
    }
    
    
    
    

    @Override
    public void run() {
        
        System.out.println(" Serveur en attente......");
        try {
            serverSock= new ServerSocket(port);
            
            Sock=serverSock.accept();
            System.out.println("Connection établie par:"+Sock.getInetAddress().getHostName());
            connected=true;
            input= new DataInputStream(Sock.getInputStream());
            output=new DataOutputStream(Sock.getOutputStream());
            String msg="";
            while(!msg.equals("EXIT") && connected){
                msg=GetMessage();
                System.out.println(msg);
                this.TchatWindow.sendMessage(msg);
            }
            this.stopService();
            

        } catch (IOException ex) {
            Logger.getLogger(ServiceTchatServeur.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
    }
    
  public String GetMessage(){
      String str;
        try {
            str=input.readUTF();
            return str;
        } catch (IOException ex) {
            Logger.getLogger(ServiceTchatServeur.class.getName()).log(Level.SEVERE, null, ex);
        }
     
     return null; 
  }   
    
  public void SendMessage(String message){
        try {
            output.writeUTF(message);
            output.flush();
        } catch (IOException ex) {
            Logger.getLogger(ServiceTchatServeur.class.getName()).log(Level.SEVERE, null, ex);
        }
      
  }       
  
   
    
 
      
   public void stopService(){
        try {
            
            
            SendMessage("EXIT");
            connected=false;
            output.close();
            input.close();
            
            Sock.close();
            serverSock.close();
        } catch (IOException ex) {
            Logger.getLogger(ServiceTchatServeur.class.getName()).log(Level.SEVERE, null, ex);
        }
       
       
   }
   
   public void startService(){
       this.start();
   }
   
}
