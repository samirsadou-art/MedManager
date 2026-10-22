package My_application.dao;

import My_application.entities.MessageInterne;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Messagerie interne (table message_interne et accusés de lecture message_lu). */
public interface MessageDao {

    /** Clé du fil « Tout le cabinet » dans {@link #nonLus(int)} (aucun utilisateur n'a l'identifiant 0). */
    int TOUT_LE_CABINET = 0;

    /** Enregistre le message et lui donne son identifiant. */
    void insert(MessageInterne m);

    /**
     * Derniers messages d'un fil, du plus ancien au plus récent : le fil
     * privé entre {@code moi} et {@code autre}, ou « Tout le cabinet » si
     * {@code autre} est null.
     */
    List<MessageInterne> conversation(int moi, Integer autre, int limite);

    /**
     * Messages reçus et pas encore lus par {@code moi}, comptés par fil :
     * identifiant de l'expéditeur, ou {@link #TOUT_LE_CABINET}.
     */
    Map<Integer, Integer> nonLus(int moi);

    /** Marque lus tous les messages reçus de ce fil ; renvoie leur nombre. */
    int marquerLus(int moi, Integer autre, LocalDateTime quand);

    /** Identifiant du dernier message (0 s'il n'y en a aucun). */
    long dernier();

    /** Supprime les messages envoyés avant cette date ; renvoie leur nombre. */
    int purgerAvant(LocalDateTime limite);
}
