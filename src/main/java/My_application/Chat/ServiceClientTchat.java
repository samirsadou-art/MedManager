
package My_application.Chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;




public class ServiceClientTchat extends Thread{


    Socket        Sock;
    DataInputStream   input;
    DataOutputStream output;
    int port=7777;
    String host="127.0.0.1";
    boolean connected=false;
    boolean paused=false;    
    Descussion disc;

    public ServiceClientTchat(Descussion disc) {
        this.disc=disc;
    }
    
    
    
    @Override
    public void run() {
        
        try {
            Sock=new Socket(host, port);
            
            input=new DataInputStream(Sock.getInputStream());
            output=new DataOutputStream(Sock.getOutputStream());
            connected=true;
            String str="";
            while(!str.equals("EXIT")&& connected){
            str=GetMessage();
            System.out.println(" Message recu du serveur : "+str);
            disc.sendMessage(str);
            
        }
            
          this.stopService();
            
            
            
        } catch (IOException ex) {
            Logger.getLogger(ServiceClientTchat.class.getName()).log(Level.SEVERE, null, ex);
           
        }
        
       
        
        
        
        
    }
    
   public void SendMessage(String message) {
        try { 
            output.writeUTF(message);
            output.flush();   
   
        } catch (IOException ex) {
            Logger.getLogger(ServiceClientTchat.class.getName()).log(Level.SEVERE, null, ex);
        }
             
   }
   
   public String GetMessage(){
       String str;
       
        try {
            str=input.readUTF();
            return str;
        } catch (IOException ex) {
            Logger.getLogger(ServiceClientTchat.class.getName()).log(Level.SEVERE, null, ex);
        }
    return null;  
   }
    
    public void startService(){
        this.start();
    }
    
    public void stopService(){
        try {
            connected=false;
            SendMessage("EXIT");
            input.close();
            output.close();
            Sock.close();
        } catch (IOException ex) {
            Logger.getLogger(ServiceClientTchat.class.getName()).log(Level.SEVERE, null, ex);
        }
      
    }
    
    

    
    
    
    
}
