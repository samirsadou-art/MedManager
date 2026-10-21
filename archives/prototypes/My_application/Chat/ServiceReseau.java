/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package My_application.Chat;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GLOBAL INFO LAGA
 */
public class ServiceReseau {
    int type=0;
    Socket client;
    ServerSocket server;
    DataInputStream dis;
    String host="";
    int port=7777;
    Thread process;

    public ServiceReseau() {
    process= new Thread(new Runnable() {
        
        
        
 
    public void run() {
        
        try {
            
            try {
                server=new ServerSocket(port);
                System.out.println("Démarrage .....");
                client=server.accept();
                System.out.println("Un client est connecté :"+client.getInetAddress().getHostName());
                dis=new DataInputStream(client.getInputStream());
               
                    String str=(String)dis.readUTF();
                    
                    System.out.println("Message du Client :"+client.getInetAddress().getHostName()+"\t"+str);
                
                
                
                
                
                
                
                
                
            } catch (IOException ex) {
                Logger.getLogger(ServiceReseau.class.getName()).log(Level.SEVERE, null, ex);
                
            }
            
            
            dis.close();
            
            
        } catch (IOException ex) {
            Logger.getLogger(ServiceReseau.class.getName()).log(Level.SEVERE, null, ex);
             
        }
        
    
    }
  });
        
    process.start();
        
    }
    
     public static void main(String[] args){
     new ServiceReseau();

       
   }   
}
