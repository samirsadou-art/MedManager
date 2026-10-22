package My_application.controller;

import My_application.entities.patient;
import My_application.service.MetierException;
import My_application.service.PatientService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

/**
 * Inscription d'un nouveau patient et modification de sa fiche
 * administrative (état civil et coordonnées, jamais le dossier médical).
 * <p>
 * À l'inscription, les fiches qui lui ressemblent (même nom et date de
 * naissance, ou même téléphone) sont proposées avant d'en créer une autre.
 */
public class FichePatientController extends ControleurBase {

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Données saisies, telles que lues dans le formulaire. */
    public static class Saisie {
        public String numero = "";
        public String numeroSecuriteSociale = "";
        public String nom = "";
        public String prenom = "";
        public String dateNaissance = "";
        public boolean masculin = true;
        public String situationMaritale = "";
        public String telephone = "";
        public String email = "";
        public String adresse = "";
        public String ville = "";
        public String wilaya = "";
        public String pays = "";

        /** Saisie pré-remplie avec la fiche d'un patient (modification). */
        public static Saisie depuis(patient p) {
            Saisie s = new Saisie();
            s.numero = String.valueOf(p.getIdentPat());
            s.numeroSecuriteSociale = texte(p.getNum_soc());
            s.nom = texte(p.getNom());
            s.prenom = texte(p.getPrenom());
            s.dateNaissance = p.getDat_nais() == null ? "" : FORMAT_DATE.format(p.getDat_nais());
            s.masculin = p.isMasculin();
            s.situationMaritale = texte(p.getSituatMarit());
            s.telephone = texte(p.getTelephone());
            s.email = texte(p.getEmail());
            s.adresse = texte(p.getLocalite());
            s.ville = texte(p.getVille());
            s.wilaya = texte(p.getWilaya());
            s.pays = texte(p.getPays());
            return s;
        }

        private static String texte(String s) {
            return s == null ? "" : s;
        }
    }

    /** Issue d'une inscription. */
    public static final class Resultat {
        /** Patient créé, ou fiche existante choisie parmi les doublons. */
        public final patient patient;
        /** Vrai si la secrétaire a choisi une fiche existante au lieu d'en créer une. */
        public final boolean existant;

        Resultat(patient patient, boolean existant) {
            this.patient = patient;
            this.existant = existant;
        }
    }

    private final FichePatientVue vue;
    private final PatientService patientService;

    public FichePatientController(FichePatientVue vue, PatientService patientService) {
        super(vue);
        this.vue = vue;
        this.patientService = patientService;
    }

    /**
     * Inscrit le patient, après avoir proposé les fiches qui lui ressemblent.
     *
     * @return le patient créé ou la fiche existante choisie ; null si la saisie
     *         est invalide (message déjà affiché) ou si l'inscription est annulée
     */
    public Resultat inscrire(Saisie s) {
        return tenter(() -> {
            patient p = versPatient(s);
            List<patient> doublons = patientService.doublonsPossibles(p);
            if (!doublons.isEmpty()) {
                int choix = vue.proposerDoublons(doublons);
                if (choix >= 0 && choix < doublons.size()) {
                    return new Resultat(doublons.get(choix), true);
                }
                if (choix != FichePatientVue.CREER_QUAND_MEME) {
                    return null;
                }
            }
            return new Resultat(patientService.creer(p), false);
        }, null);
    }

    /** Enregistre la fiche modifiée ; renvoie le patient à jour, ou null (message affiché). */
    public patient modifier(patient original, Saisie s) {
        patient modifie = tenter(() -> {
            patient p = versPatient(s);
            p.setIdentPat(original.getIdentPat());
            return patientService.modifier(p);
        }, null);
        if (modifie != null) {
            vue.afficherInformation("Fiche de " + modifie + " enregistrée.");
        }
        return modifie;
    }

    static patient versPatient(Saisie s) {
        patient p = new patient();
        String num = s.numero == null ? "" : s.numero.trim();
        if (!num.isEmpty()) {
            try {
                p.setIdentPat(Integer.parseInt(num));
            } catch (NumberFormatException e) {
                throw new MetierException("Le numéro de dossier doit être un nombre.");
            }
        }
        p.setNum_soc(s.numeroSecuriteSociale);
        p.setNom(s.nom);
        p.setPrenom(s.prenom);
        p.setSexe(s.masculin);
        p.setSituatMarit(s.situationMaritale);
        p.setTelephone(s.telephone);
        p.setEmail(s.email);
        p.setLocalite(s.adresse);
        p.setVille(s.ville);
        p.setWilaya(s.wilaya);
        p.setPays(s.pays);
        String d = s.dateNaissance == null ? "" : s.dateNaissance.trim();
        if (!d.isEmpty()) {
            try {
                p.setDat_nais(LocalDate.parse(d, FORMAT_DATE));
            } catch (DateTimeParseException e) {
                throw new MetierException("Date de naissance invalide : utilisez le format jj/mm/aaaa.");
            }
        }
        return p;
    }
}
