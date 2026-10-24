package My_application.controller;

import My_application.entities.MessageInterne;
import My_application.entities.patient;
import My_application.service.MessagerieService;
import My_application.service.MessagerieService.Conversation;
import My_application.service.PatientService;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagerieController extends ControleurBase {

    private final MessagerieVue vue;
    private final MessagerieService messagerie;
    private final PatientService patients;

    /** Fil affiché : membre, ou null pour « Tout le cabinet ». */
    private Integer filOuvert;
    private boolean filChoisi;
    private long dernierVu = -1;
    /** Non-lus au dernier passage (-1 avant la première lecture : pas de son au démarrage). */
    private int nonLusAvant = -1;
    private boolean verificationEnCours;

    public MessagerieController(MessagerieVue vue, MessagerieService messagerie, PatientService patients) {
        this(vue, messagerie, patients, Executeur.SWING);
    }

    public MessagerieController(MessagerieVue vue, MessagerieService messagerie, PatientService patients,
            Executeur executeur) {
        super(vue, executeur);
        this.vue = vue;
        this.messagerie = messagerie;
        this.patients = patients;
    }

    /** Lecture d'un tour : les fils, et les messages du fil ouvert si la fenêtre est affichée. */
    private static final class Etat {
        long dernier;
        List<Conversation> conversations;
        List<MessageInterne> messages;
    }

    private Etat lire(boolean avecFil) {
        Etat e = new Etat();
        e.dernier = messagerie.dernierMessage();
        // Le fil est lu (et marqué lu) avant de compter les non-lus.
        e.messages = avecFil ? messagerie.lire(filOuvert) : null;
        e.conversations = messagerie.conversations();
        return e;
    }

    private void afficher(Etat e) {
        dernierVu = e.dernier;
        if (e.messages != null) {
            vue.afficherMessages(filOuvert, e.messages);
        }
        vue.afficherConversations(e.conversations);
        int total = MessagerieService.total(e.conversations);
        if (nonLusAvant >= 0 && total > nonLusAvant) {
            vue.signalerNouveauMessage();
        }
        nonLusAvant = total;
        vue.afficherNonLus(total);
    }

    /** Ouvre un fil (null : tout le cabinet) et le marque lu. */
    public void ouvrir(Integer autre) {
        filOuvert = autre;
        filChoisi = true;
        Etat e = tenter(() -> lire(true), null);
        if (e != null) {
            afficher(e);
        }
    }

    /** Relit les fils (et le fil ouvert si la fenêtre est affichée). */
    public void rafraichir() {
        final boolean avecFil = filChoisi && vue.estAffichee();
        enArrierePlan(() -> lire(avecFil), this::afficher);
    }

    /**
     * Toutes les 3 secondes : relit seulement le numéro du dernier message,
     * et les fils s'il a changé. Une panne est seulement journalisée.
     */
    public void verifier() {
        if (verificationEnCours) {
            return;
        }
        verificationEnCours = true;
        final boolean avecFil = filChoisi && vue.estAffichee();
        enArrierePlanSilencieux(() -> {
            long dernier = messagerie.dernierMessage();
            return dernier == dernierVu ? null : lire(avecFil);
        }, e -> {
            if (e != null) {
                afficher(e);
            }
        }, () -> verificationEnCours = false);
    }

    /** Envoie le message dans le fil ouvert ; renvoie vrai s'il est parti (la saisie peut être vidée). */
    public boolean envoyer(String texte, patient cite) {
        if (!filChoisi) {
            vue.afficherErreur("Choisissez d'abord à qui écrire.");
            return false;
        }
        final Integer fil = filOuvert;
        if (tenter(() -> {
            messagerie.envoyer(fil, texte, cite);
        })) {
            ouvrir(fil);
            return true;
        }
        return false;
    }

    /** Patients à citer dans un message (nom ou téléphone). */
    public List<patient> rechercherPatients(String texte) {
        return tenter(() -> patients.rechercher(texte), Collections.<patient>emptyList());
    }

    /** Fiche complète du patient cité, ou null. */
    public patient fichePatient(patient cite) {
        return tenter(() -> patients.trouver(cite.getIdentPat()), null);
    }

    /** Signe de vie du poste (toutes les 30 s), sans message en cas de panne. */
    public void signalerPresence(String poste) {
        enArrierePlanSilencieux(() -> {
            messagerie.signalerPresence(poste);
            return Boolean.TRUE;
        }, ok -> { }, () -> { });
    }

    /** Au démarrage : efface les messages trop anciens, sans message en cas de panne. */
    public void purger() {
        enArrierePlanSilencieux(messagerie::purger, n -> { }, () -> { });
    }

    /** Fermeture de GestiMed (sur l'EDT, sans attendre de réponse à l'écran). */
    public void quitter() {
        try {
            messagerie.quitter();
        } catch (RuntimeException e) {
            // L'application se ferme : la présence expirera seule au bout d'une minute.
            Logger.getLogger("GestiMed").log(Level.FINE, "Présence non effacée", e);
        }
    }

    public Integer getFilOuvert() {
        return filOuvert;
    }

    public boolean isFilChoisi() {
        return filChoisi;
    }
}
