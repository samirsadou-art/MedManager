package My_application.controller;

import My_application.entities.patient;
import My_application.service.MetierException;
import My_application.service.PatientService;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Création d'un patient. Le formulaire {@code NewPatient} existait mais
 * n'enregistrait rien : il est maintenant relié à {@link PatientService}.
 */
public class NouveauPatientController extends ControleurBase {

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
    }

    private final Vue vue;
    private final PatientService patientService;
    private patient cree;

    public NouveauPatientController(Vue vue, PatientService patientService) {
        super(vue);
        this.vue = vue;
        this.patientService = patientService;
    }

    /** Renvoie le patient créé, ou null si la saisie est invalide (message déjà affiché). */
    public patient enregistrer(final Saisie s) {
        cree = null;
        boolean ok = tenter(new Runnable() {
            @Override
            public void run() {
                cree = patientService.creer(versPatient(s));
            }
        });
        if (ok) {
            vue.afficherInformation("Patient " + cree + " enregistré (dossier n° " + cree.getIdentPat() + ").");
        }
        return cree;
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
        p.setNum_soc(s.numeroSecuriteSociale == null ? null : s.numeroSecuriteSociale.trim());
        p.setNom(s.nom);
        p.setPrenom(s.prenom);
        p.setSexe(s.masculin);
        p.setSituatMarit(s.situationMaritale);
        String d = s.dateNaissance == null ? "" : s.dateNaissance.trim();
        if (!d.isEmpty()) {
            try {
                p.setDat_nais(Date.valueOf(LocalDate.parse(d, FORMAT_DATE)));
            } catch (DateTimeParseException e) {
                throw new MetierException("Date de naissance invalide : utilisez le format jj/mm/aaaa.");
            }
        }
        return p;
    }
}
