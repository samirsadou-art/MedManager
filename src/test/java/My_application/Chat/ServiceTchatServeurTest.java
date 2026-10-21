package My_application.Chat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.ServerSocket;
import org.junit.Test;

public class ServiceTchatServeurTest {

    /** Le serveur de discussion n'a ni authentification ni chiffrement : il ne doit pas être joignable du réseau. */
    @Test(timeout = 5000)
    public void leServeurNEcouteQueSurLaBoucleLocale() throws Exception {
        ServiceTchatServeur serveur = new ServiceTchatServeur(null);
        serveur.startService();
        try {
            ServerSocket socket = null;
            while (socket == null || !socket.isBound()) {
                Thread.sleep(20);
                socket = serveur.serverSock;
                if (!serveur.isAlive() && socket == null) {
                    break; // port 7777 déjà occupé sur cette machine
                }
            }
            org.junit.Assume.assumeNotNull(socket);
            assertNotNull(socket.getInetAddress());
            assertTrue(socket.getInetAddress().isLoopbackAddress());
        } finally {
            serveur.stopService();
        }
    }
}
