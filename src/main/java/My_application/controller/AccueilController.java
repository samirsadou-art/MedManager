package My_application.controller;

import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.service.DossierService;
import My_application.service.RendezVousService;
import My_application.service.SalleAttenteService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class AccueilController extends ControleurBase {

    private final AccueilVue vue;
    private final DossierService dossierService;
    private final RendezVousService rendezVousService;
    /** Faux pour la secrétaire : les consultations (motifs, diagnostics) relèvent du dossier médical. */
    private final boolean avecConsultations;

    // Salle d'attente : branchée par brancherSalleAttente (absente des anciens écrans de test).
    private SalleAttenteService salle;
    /** File affichée : celle de ce médecin, ou de tous si null (secrétaire). */
    private Integer medecinDeLaFile;
    private Supplier<medecin> medecinTraitant;
    private boolean annoncerAppels;

    /** Dernier événement pris en compte : un autre poste a changé quelque chose s'il augmente. */
    private long dernierEvenementVu = -1;
    private boolean verificationEnCours;
    /** Passages déjà vus en consultation (null avant la première lecture : rien à annoncer). */
    private Set<Integer> appelsConnus;

    public AccueilController(AccueilVue vue, DossierService dossierService, RendezVousService rendezVousService) {
        this(vue, dossierService, rendezVousService, Executeur.SWING);
    }

    public AccueilController(AccueilVue vue, DossierService dossierService, RendezVousService rendezVousService,
            Executeur executeur) {
        this(vue, dossierService, rendezVousService, executeur, true);
    }

    public AccueilController(AccueilVue vue, DossierService dossierService, RendezVousService rendezVousService,
            Executeur executeur, boolean avecConsultations) {
        super(vue, executeur);
        this.vue = vue;
        this.dossierService = dossierService;
        this.rendezVousService = rendezVousService;
        this.avecConsultations = avecConsultations;
    }

    /**
     * Ajoute la salle d'attente à l'accueil.
     *
     * @param medecinDeLaFile médecin dont la file est affichée et appelée (null : tous, pour la secrétaire)
     * @param medecinTraitant médecin qui ouvre le dossier du patient appelé
     * @param annoncerAppels vrai pour annoncer chaque patient appelé par un médecin
     */
    public AccueilController brancherSalleAttente(SalleAttenteService salle, Integer medecinDeLaFile,
            Supplier<medecin> medecinTraitant, boolean annoncerAppels) {
        this.salle = salle;
        this.medecinDeLaFile = medecinDeLaFile;
        this.medecinTraitant = medecinTraitant;
        this.annoncerAppels = annoncerAppels;
        return this;
    }

    /** Activité du jour, lue d'un bloc hors de l'EDT. */
    private static final class Activite {
        List<Rendez_Vous> rendezVous;
        List<consultation> consultations;
        long evenement;
        List<Passage> file;
        Map<Integer, String> medecins;
        LocalDateTime maintenant;
    }

    /** Relit l'activité du jour en arrière-plan : la fenêtre reste utilisable pendant la lecture. */
    public void rafraichir() {
        enArrierePlan(this::lire, a -> {
            vue.afficherRendezVousDuJour(a.rendezVous);
            vue.afficherConsultationsDuJour(a.consultations);
            if (a.file != null) {
                dernierEvenementVu = a.evenement;
                vue.afficherSalleAttente(a.file, a.medecins, a.maintenant);
                annoncerLesAppels(a);
            }
        });
    }

    private Activite lire() {
        Activite a = new Activite();
        if (salle != null) {
            // Lu en premier : un changement fait pendant la lecture sera revu au prochain tour.
            a.evenement = salle.dernierEvenement();
            a.file = salle.file(medecinDeLaFile);
            a.medecins = new HashMap<>();
            for (medecin m : rendezVousService.tousLesMedecins()) {
                a.medecins.put(m.getIdmedecin(), m.toString());
            }
            a.maintenant = salle.getMaintenant();
        }
        LocalDate jour = rendezVousService.aujourdhui();
        a.rendezVous = rendezVousService.duJour(jour);
        a.consultations = avecConsultations
                ? dossierService.consultationsDuJour(jour) : Collections.<consultation>emptyList();
        return a;
    }

    private void annoncerLesAppels(Activite a) {
        Set<Integer> appels = new HashSet<>();
        for (Passage p : a.file) {
            if (p.getEtat() == Passage.EN_CONSULTATION) {
                appels.add(p.getIdPassage());
                if (annoncerAppels && appelsConnus != null && !appelsConnus.contains(p.getIdPassage())) {
                    String medecin = a.medecins.get(p.getIdMedecin());
                    vue.annoncerAppel(p.getPatient().getNom() + " " + p.getPatient().getPrenom()
                            + " est appelé(e) en consultation" + (medecin == null ? "" : " par " + medecin) + ".");
                }
            }
        }
        appelsConnus = appels;
    }

    /**
     * Appelé toutes les 3 secondes : relit le numéro du dernier événement et
     * ne recharge l'accueil que si un poste a changé quelque chose. En cas de
     * panne, rien n'est affiché (le prochain tour réessaiera).
     */
    public void verifierEvenements() {
        if (salle == null || verificationEnCours) {
            return;
        }
        verificationEnCours = true;
        enArrierePlanSilencieux(salle::dernierEvenement, n -> {
            if (n != dernierEvenementVu) {
                rafraichir();
            }
        }, () -> verificationEnCours = false);
    }

    /** Au démarrage : supprime les événements de plus de 7 jours (sans prévenir en cas d'échec). */
    public void purgerEvenements() {
        if (salle != null) {
            enArrierePlanSilencieux(salle::purgerEvenements, n -> { }, () -> { });
        }
    }

    // ----- secrétaire : rendez-vous du jour -----

    public boolean peutPointer(Rendez_Vous r) {
        return salle != null && salle.peutEtrePointe(r);
    }

    /** Le patient du rendez-vous est arrivé : il entre en salle d'attente. */
    public void arrivee(Rendez_Vous r) {
        if (tenter(() -> {
            salle.arrivee(r);
        })) {
            rafraichir();
        }
    }

    public boolean peutMarquerAbsent(Rendez_Vous r) {
        return r != null && rendezVousService.peutEtreMarqueAbsent(r);
    }

    public void absent(Rendez_Vous r) {
        if (tenter(() -> rendezVousService.marquerAbsent(r))) {
            rafraichir();
        }
    }

    /** Rendez-vous non honorés marqués absents, puis bilan du jour. */
    public void finDeJournee() {
        Integer absents = tenter(salle::finDeJournee, null);
        if (absents == null) {
            return;
        }
        SalleAttenteService.Bilan b = tenter(salle::bilan, null);
        if (b != null) {
            vue.afficherInformation(texteBilan(absents, b));
        }
        rafraichir();
    }

    static String texteBilan(int marquesAbsents, SalleAttenteService.Bilan b) {
        return (marquesAbsents == 0 ? "Aucun rendez-vous en suspens."
                : marquesAbsents + " rendez-vous non honoré(s) marqué(s) absent(s).")
                + "\n\nBilan du jour :"
                + "\n  patients vus : " + b.getVus()
                + "\n  dont sans rendez-vous : " + b.getSansRendezVous()
                + "\n  absents : " + b.getAbsents()
                + "\n  partis sans consultation : " + b.getPartis()
                + "\n  encore en attente : " + b.getEnAttente()
                + "\n  attente moyenne : " + b.getAttenteMoyenne() + " min";
    }

    // ----- salle d'attente -----

    public void urgence(Passage p) {
        if (p != null && tenter(() -> salle.urgence(p, !p.isUrgence()))) {
            rafraichir();
        }
    }

    public void parti(Passage p) {
        if (p != null && tenter(() -> salle.partiSansConsultation(p))) {
            rafraichir();
        }
    }

    /** Le médecin appelle le premier patient de sa file et ouvre son dossier. */
    public void appelerSuivant() {
        Passage p = tenter(() -> salle.appelerSuivant(medecinDeLaFile), null);
        if (p != null) {
            rafraichir();
            ouvrirDossier(p);
        }
    }

    /** Le médecin appelle ce patient (ou reprend celui déjà en consultation) et ouvre son dossier. */
    public void appeler(Passage p) {
        if (p == null) {
            return;
        }
        if (p.getEtat() == Passage.EN_CONSULTATION) {
            ouvrirDossier(p);
        } else if (tenter(() -> salle.appeler(p))) {
            rafraichir();
            ouvrirDossier(p);
        }
    }

    private void ouvrirDossier(Passage p) {
        tenter(() -> vue.ouvrirDossier(dossierService.ouvrirDossier(p.getPatient(), medecinTraitant.get())));
    }
}
