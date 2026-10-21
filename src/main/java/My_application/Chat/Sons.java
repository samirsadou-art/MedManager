package My_application.Chat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Sonnerie du chat. Un seul fil (démon) joue les sons, au lieu d'un nouveau
 * {@code Thread} à chaque appui, et chaque {@link Clip} est fermé une fois
 * joué. Une erreur va au journal plutôt que sur {@code System.err}.
 */
final class Sons {

    private static final Logger LOG = Logger.getLogger(Sons.class.getName());

    private static final ExecutorService LECTEUR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GestiMed-sons");
        t.setDaemon(true);
        return t;
    });

    private Sons() {
    }

    /** Joue {@code /res/sound/<nom>} sans bloquer l'appelant. */
    static void jouer(String nom) {
        LECTEUR.execute(() -> lire(nom));
    }

    private static void lire(String nom) {
        InputStream ressource = Sons.class.getResourceAsStream("/res/sound/" + nom);
        if (ressource == null) {
            LOG.warning("Son introuvable : " + nom);
            return;
        }
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(ressource))) {
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.open(audio);
            clip.start();
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Lecture du son " + nom + " impossible", e);
        }
    }
}
