package My_application.service;

import My_application.entities.analyse;
import My_application.entities.consultation;
import My_application.entities.medicament;
import My_application.entities.pathologie;
import My_application.entities.symptome;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ce que les onglets du dossier patient montrent de ses consultations :
 * dernières constantes et leur évolution, pathologies et symptômes les plus
 * fréquents, examens demandés, ordonnances. Tout est calculé sur le dossier
 * déjà chargé, sans accès à la base.
 */
public final class SyntheseDossier {

    private SyntheseDossier() {
    }

    // ----- constantes vitales -----

    /** Constante relevée à chaque consultation. */
    public enum Mesure {
        TEMPERATURE("Température", "°C"),
        POULS("Pouls", "bpm"),
        TENSION("Tension", "mmHg"),
        RESPIRATION("Respiration", "/min"),
        POIDS("Poids", "kg");

        private final String libelle;
        private final String unite;

        Mesure(String libelle, String unite) {
            this.libelle = libelle;
            this.unite = unite;
        }

        public String getLibelle() {
            return libelle;
        }

        public String getUnite() {
            return unite;
        }

        /** Valeur mesurée (la systolique pour la tension), ou NaN si non mesurée. */
        public double valeur(consultation c) {
            double v;
            switch (this) {
                case TEMPERATURE:
                    v = c.getTemperature();
                    break;
                case POULS:
                    v = c.getPul_Cardiaque();
                    break;
                case TENSION:
                    v = c.getPressionSystolique();
                    break;
                case RESPIRATION:
                    v = c.getTauxRespir();
                    break;
                default:
                    v = c.getPoids();
            }
            return v == consultation.NON_RENSEIGNE ? Double.NaN : v;
        }

        public boolean estMesuree(consultation c) {
            return !Double.isNaN(valeur(c));
        }

        /** « 37,2 », « 135/85 », ou "" si non mesurée. */
        public String texte(consultation c) {
            if (!estMesuree(c)) {
                return "";
            }
            return this == TENSION ? c.getPressionA() : nombre(valeur(c));
        }
    }

    /** Niveau de la tension, pour la colorer. */
    public enum NiveauTension {
        NORMALE, LIMITE, ELEVEE;

        /** Élevée à partir de 140/90, limite à partir de 130/85 ; null si non mesurée. */
        public static NiveauTension de(consultation c) {
            int s = c.getPressionSystolique();
            int d = c.getPressionDiastolique();
            if (s == consultation.NON_RENSEIGNE) {
                return null;
            }
            if (s >= 140 || d >= 90) {
                return ELEVEE;
            }
            return s >= 130 || d >= 85 ? LIMITE : NORMALE;
        }
    }

    /** Consultations de la plus récente à la plus ancienne. */
    public static List<consultation> plusRecentesDAbord(List<consultation> consultations) {
        List<consultation> l = new ArrayList<>(consultations);
        Collections.reverse(l); // le dossier les garde dans l'ordre chronologique
        l.sort(Comparator.comparing(consultation::getDate_cons, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(consultation::getHeure_cons, Comparator.nullsLast(Comparator.reverseOrder())));
        return l;
    }

    /**
     * Les deux dernières consultations où la constante a été mesurée : la
     * plus récente d'abord. La liste a 0, 1 ou 2 éléments.
     */
    public static List<consultation> deuxDernieresMesures(List<consultation> consultations, Mesure m) {
        List<consultation> l = new ArrayList<>(2);
        for (consultation c : plusRecentesDAbord(consultations)) {
            if (m.estMesuree(c)) {
                l.add(c);
                if (l.size() == 2) {
                    break;
                }
            }
        }
        return l;
    }

    /**
     * Évolution depuis la mesure précédente : « ▲ 5 mmHg », « ▼ 1,5 kg »,
     * « = stable », ou "" sans mesure précédente.
     */
    public static String tendance(List<consultation> consultations, Mesure m) {
        List<consultation> l = deuxDernieresMesures(consultations, m);
        if (l.size() < 2) {
            return "";
        }
        double ecart = m.valeur(l.get(0)) - m.valeur(l.get(1));
        if (Math.abs(ecart) < 0.05) {
            return "= stable";
        }
        return (ecart > 0 ? "▲ " : "▼ ") + nombre(Math.abs(ecart)) + " " + m.getUnite();
    }

    /** Écart signé avec la mesure précédente (0 sans mesure précédente). */
    public static double ecart(List<consultation> consultations, Mesure m) {
        List<consultation> l = deuxDernieresMesures(consultations, m);
        return l.size() < 2 ? 0 : m.valeur(l.get(0)) - m.valeur(l.get(1));
    }

    /** « 37,2 », « 82 » : virgule décimale, au plus une décimale. */
    public static String nombre(double v) {
        DecimalFormat f = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.FRENCH));
        return f.format(v);
    }

    // ----- pathologies et symptômes -----

    /** Une pathologie ou un symptôme noté dans plusieurs consultations. */
    public static final class Frequence {

        private final String designation;
        private int nombre;
        private LocalDate premiere;
        private LocalDate derniere;

        Frequence(String designation) {
            this.designation = designation;
        }

        void compter(LocalDate jour) {
            nombre++;
            if (jour != null) {
                premiere = premiere == null || jour.isBefore(premiere) ? jour : premiere;
                derniere = derniere == null || jour.isAfter(derniere) ? jour : derniere;
            }
        }

        public String getDesignation() {
            return designation;
        }

        public int getNombre() {
            return nombre;
        }

        public LocalDate getPremiere() {
            return premiere;
        }

        public LocalDate getDerniere() {
            return derniere;
        }
    }

    /** Pathologies diagnostiquées, de la plus fréquente à la moins fréquente. */
    public static List<Frequence> pathologies(List<consultation> consultations) {
        Map<String, Frequence> parNom = new LinkedHashMap<>();
        for (consultation c : consultations) {
            for (pathologie p : c.getPathologies()) {
                compter(parNom, p.getDesignation(), c.getDate_cons());
            }
        }
        return trier(parNom);
    }

    /** Symptômes notés, du plus fréquent au moins fréquent. */
    public static List<Frequence> symptomes(List<consultation> consultations) {
        Map<String, Frequence> parNom = new LinkedHashMap<>();
        for (consultation c : consultations) {
            for (symptome s : c.getSymptomes()) {
                compter(parNom, s.getDesignation(), c.getDate_cons());
            }
        }
        return trier(parNom);
    }

    private static void compter(Map<String, Frequence> parNom, String designation, LocalDate jour) {
        if (designation == null || designation.trim().isEmpty()) {
            return;
        }
        String cle = designation.trim().toLowerCase(Locale.FRENCH);
        Frequence f = parNom.get(cle);
        if (f == null) {
            f = new Frequence(designation.trim());
            parNom.put(cle, f);
        }
        f.compter(jour);
    }

    private static List<Frequence> trier(Map<String, Frequence> parNom) {
        List<Frequence> l = new ArrayList<>(parNom.values());
        l.sort(Comparator.comparingInt(Frequence::getNombre).reversed()
                .thenComparing(Frequence::getDerniere, Comparator.nullsLast(Comparator.reverseOrder())));
        return l;
    }

    /** « Céphalées, vertiges · Hypertension artérielle · TA 135/85 » : résumé d'une consultation. */
    public static String resume(consultation c) {
        List<String> parties = new ArrayList<>();
        List<String> symptomes = new ArrayList<>();
        for (symptome s : c.getSymptomes()) {
            symptomes.add(s.getDesignation());
        }
        if (!symptomes.isEmpty()) {
            parties.add(String.join(", ", symptomes));
        }
        List<String> pathologies = new ArrayList<>();
        for (pathologie p : c.getPathologies()) {
            pathologies.add(p.getDesignation());
        }
        if (!pathologies.isEmpty()) {
            parties.add(String.join(", ", pathologies));
        }
        if (Mesure.TENSION.estMesuree(c)) {
            parties.add("TA " + c.getPressionA());
        }
        return parties.isEmpty() ? "Rien de noté" : String.join(" · ", parties);
    }

    // ----- examens -----

    /** Interprétation d'un résultat d'analyse (la note de couleur saisie). */
    public enum Interpretation {
        NORMAL("Normal"),
        A_SURVEILLER("À surveiller"),
        ANORMAL("Anormal"),
        NON_INTERPRETE("Non interprété"),
        EN_ATTENTE("En attente");

        private final String libelle;

        Interpretation(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }

        /** Note 1 vert, 2 orange, 3 rouge ; sans résultat : en attente. */
        public static Interpretation de(analyse a) {
            if (a.getResultat() == null || a.getResultat().trim().isEmpty()) {
                return EN_ATTENTE;
            }
            switch (a.getNote()) {
                case 1:
                    return NORMAL;
                case 2:
                    return A_SURVEILLER;
                case 3:
                    return ANORMAL;
                default:
                    return NON_INTERPRETE;
            }
        }
    }

    /** Une analyse demandée lors d'une consultation. */
    public static final class Examen {

        private final consultation consultation;
        private final analyse analyse;

        Examen(consultation consultation, analyse analyse) {
            this.consultation = consultation;
            this.analyse = analyse;
        }

        public consultation getConsultation() {
            return consultation;
        }

        public analyse getAnalyse() {
            return analyse;
        }

        public Interpretation getInterpretation() {
            return Interpretation.de(analyse);
        }
    }

    /** Toutes les analyses demandées, de la plus récente à la plus ancienne. */
    public static List<Examen> examens(List<consultation> consultations) {
        List<Examen> l = new ArrayList<>();
        for (consultation c : plusRecentesDAbord(consultations)) {
            if (c.hasAnalyse()) {
                for (analyse a : c.getAnaPat().getListeAnaly()) {
                    l.add(new Examen(c, a));
                }
            }
        }
        return l;
    }

    // ----- ordonnances -----

    /** Consultations qui ont une ordonnance, de la plus récente à la plus ancienne. */
    public static List<consultation> ordonnances(List<consultation> consultations) {
        List<consultation> l = new ArrayList<>();
        for (consultation c : plusRecentesDAbord(consultations)) {
            if (c.hasOrdonnance()) {
                l.add(c);
            }
        }
        return l;
    }

    /** « 1 U · 3 fois/jour le matin · pendant 5 jours », comme sur l'ordonnance imprimée. */
    public static String posologie(medicament m) {
        StringBuilder b = new StringBuilder();
        b.append(m.getUnite()).append(" U · ").append(m.getPosologie()).append(" fois/jour");
        if (m.getMode() == medicament.MODE_MATIN) {
            b.append(" le matin");
        } else if (m.getMode() == medicament.MODE_SOIR) {
            b.append(" le soir");
        }
        if (m.getDuree() > 0) {
            b.append(" · pendant ").append(m.getDuree()).append(" jours");
        }
        return b.toString();
    }
}
