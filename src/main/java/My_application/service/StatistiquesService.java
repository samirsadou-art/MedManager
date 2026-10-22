package My_application.service;

import My_application.dao.DAOFactory;
import My_application.dao.StatistiquesDao;
import My_application.entities.Rendez_Vous;
import My_application.entities.Role;
import My_application.entities.Statistiques;
import My_application.entities.medecin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tableau de bord de l'administrateur : activité du cabinet sur une période,
 * pour tout le cabinet ou un médecin. Uniquement des nombres agrégés : aucun
 * nom de patient ne sort de ce service (secret médical).
 */
public class StatistiquesService {

    /** Période la plus longue acceptée (trois ans). */
    static final int JOURS_MAX = 3 * 366;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH);
    private static final DateTimeFormatter JOUR_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter MOIS_LONG = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    /** Périodes proposées dans la liste du tableau de bord. */
    public enum Periode {
        AUJOURDHUI("Aujourd'hui"),
        SEMAINE("Cette semaine"),
        MOIS("Ce mois-ci"),
        MOIS_PRECEDENT("Le mois dernier"),
        TRIMESTRE("Les 3 derniers mois"),
        ANNEE("Cette année"),
        ANNEE_PRECEDENTE("L'année dernière");

        private final String libelle;

        Periode(String libelle) {
            this.libelle = libelle;
        }

        @Override
        public String toString() {
            return libelle;
        }

        /** Premier et dernier jour inclus de la période, vue depuis {@code aujourdhui}. */
        public LocalDate[] bornes(LocalDate aujourdhui) {
            switch (this) {
                case AUJOURDHUI:
                    return new LocalDate[] {aujourdhui, aujourdhui};
                case SEMAINE:
                    LocalDate lundi = aujourdhui.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    return new LocalDate[] {lundi, lundi.plusDays(6)};
                case MOIS:
                    return new LocalDate[] {aujourdhui.withDayOfMonth(1),
                        aujourdhui.with(TemporalAdjusters.lastDayOfMonth())};
                case MOIS_PRECEDENT:
                    LocalDate m = aujourdhui.minusMonths(1);
                    return new LocalDate[] {m.withDayOfMonth(1), m.with(TemporalAdjusters.lastDayOfMonth())};
                case TRIMESTRE:
                    return new LocalDate[] {aujourdhui.minusMonths(3).plusDays(1), aujourdhui};
                case ANNEE:
                    return new LocalDate[] {aujourdhui.withDayOfYear(1), aujourdhui.with(TemporalAdjusters.lastDayOfYear())};
                default:
                    LocalDate a = aujourdhui.minusYears(1);
                    return new LocalDate[] {a.withDayOfYear(1), a.with(TemporalAdjusters.lastDayOfYear())};
            }
        }
    }

    /** Une barre du graphique des consultations : un jour, une semaine ou un mois. */
    public static final class Barre {
        private final String libelle;
        private final String detail;
        private final int valeur;

        public Barre(String libelle, String detail, int valeur) {
            this.libelle = libelle;
            this.detail = detail;
            this.valeur = valeur;
        }

        /** Libellé court, sous la barre. */
        public String getLibelle() {
            return libelle;
        }

        /** Libellé complet, dans l'info-bulle et l'export. */
        public String getDetail() {
            return detail;
        }

        public int getValeur() {
            return valeur;
        }
    }

    private final StatistiquesDao dao;
    private final DAOFactory daos;
    private final ControleAcces acces;

    public StatistiquesService(DAOFactory daos, ControleAcces acces) {
        this.daos = daos;
        this.dao = daos.getStatistiquesDao();
        this.acces = acces;
    }

    /** Tous les médecins, même désactivés (leur activité passée compte). */
    public List<medecin> medecins() {
        acces.exiger(Role.ADMIN);
        return daos.getMedecinDao().findAll();
    }

    /**
     * Calcule les indicateurs entre deux dates incluses.
     *
     * @param idMedecin null pour tout le cabinet
     */
    public Statistiques calculer(LocalDate debut, LocalDate fin, Integer idMedecin) {
        acces.exiger(Role.ADMIN);
        if (debut == null || fin == null) {
            throw new MetierException("Choisissez le début et la fin de la période.");
        }
        if (fin.isBefore(debut)) {
            throw new MetierException("La fin de la période est avant son début.");
        }
        if (ChronoUnit.DAYS.between(debut, fin) >= JOURS_MAX) {
            throw new MetierException("Choisissez une période de trois ans au plus.");
        }
        Statistiques s = new Statistiques(debut, fin, idMedecin);
        s.setConsultationsParJour(dao.consultationsParJour(debut, fin, idMedecin));
        Map<Integer, Integer> parMedecin = dao.consultationsParMedecin(debut, fin);
        if (idMedecin != null) {
            parMedecin.keySet().retainAll(java.util.Collections.singleton(idMedecin));
        }
        s.setConsultationsParMedecin(parMedecin);
        s.setPatientsDistincts(dao.patientsDistincts(debut, fin, idMedecin));
        s.setNouveauxPatients(dao.nouveauxPatients(debut, fin, idMedecin));
        s.setRendezVousParEtat(dao.rendezVousParEtat(debut, fin, idMedecin));
        s.setSalle(dao.salleAttente(debut, fin, idMedecin));
        return s;
    }

    /**
     * Consultations regroupées pour le graphique, jours sans consultation
     * compris : par jour jusqu'à 31 jours, par semaine jusqu'à 26 semaines,
     * par mois au-delà.
     */
    public static List<Barre> serie(Statistiques s) {
        LocalDate debut = s.getDebut();
        LocalDate fin = s.getFin();
        long jours = jours(s);
        Map<LocalDate, Integer> parGroupe = new LinkedHashMap<>();
        Map<LocalDate, String[]> libelles = new LinkedHashMap<>();
        for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
            LocalDate groupe;
            String court;
            String detail;
            if (jours <= JOURS_PAR_JOUR) {
                groupe = d;
                court = JOUR.format(d);
                detail = JOUR_LONG.format(d);
            } else if (jours <= JOURS_PAR_SEMAINE) {
                groupe = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                court = JOUR.format(groupe);
                detail = "Semaine du " + JOUR_LONG.format(groupe);
            } else {
                groupe = d.withDayOfMonth(1);
                court = MOIS.format(groupe);
                detail = MOIS_LONG.format(groupe);
            }
            parGroupe.merge(groupe, s.getConsultationsParJour().getOrDefault(d, 0), Integer::sum);
            libelles.putIfAbsent(groupe, new String[] {court, detail});
        }
        List<Barre> r = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> e : parGroupe.entrySet()) {
            String[] l = libelles.get(e.getKey());
            r.add(new Barre(l[0], majuscule(l[1]), e.getValue()));
        }
        return r;
    }

    private static final int JOURS_PAR_JOUR = 31;
    private static final int JOURS_PAR_SEMAINE = 26 * 7;

    private static long jours(Statistiques s) {
        return ChronoUnit.DAYS.between(s.getDebut(), s.getFin()) + 1;
    }

    /** Regroupement du graphique : « jour », « semaine » ou « mois ». */
    public static String grain(Statistiques s) {
        long jours = jours(s);
        return jours <= JOURS_PAR_JOUR ? "jour" : jours <= JOURS_PAR_SEMAINE ? "semaine" : "mois";
    }

    /** « du 01/10/2026 au 31/10/2026 », ou « le 22/10/2026 ». */
    public static String libellePeriode(Statistiques s) {
        return s.getDebut().equals(s.getFin()) ? "le " + DATE.format(s.getDebut())
                : "du " + DATE.format(s.getDebut()) + " au " + DATE.format(s.getFin());
    }

    /** Indicateurs principaux, dans l'ordre de l'écran, du PDF et du CSV. */
    public static Map<String, String> indicateurs(Statistiques s) {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("Consultations", String.valueOf(s.getConsultations()));
        r.put("Patients vus", String.valueOf(s.getPatientsDistincts()));
        r.put("Nouveaux patients", String.valueOf(s.getNouveauxPatients()));
        r.put("Rendez-vous pris", String.valueOf(s.getRendezVous()));
        r.put("Rendez-vous honorés", String.valueOf(s.getRendezVousHonores()));
        r.put("Absences", String.valueOf(s.rendezVous(Rendez_Vous.ETAT_ABSENT)));
        r.put("Taux d'absence", pourcentage(s.getTauxAbsence()));
        r.put("Annulations", String.valueOf(s.rendezVous(Rendez_Vous.ETAT_ANNULE)));
        r.put("Passages en salle d'attente", String.valueOf(s.getSalle().getPassages()));
        r.put("dont sans rendez-vous", String.valueOf(s.getSalle().getSansRendezVous()));
        r.put("dont urgences", String.valueOf(s.getSalle().getUrgences()));
        r.put("Partis sans consultation", String.valueOf(s.getSalle().getPartis()));
        r.put("Attente moyenne", minutes(s.getSalle().getAttenteMoyenne()));
        return r;
    }

    public static String pourcentage(Double taux) {
        return taux == null ? "–" : String.format(Locale.FRENCH, "%.1f %%", taux);
    }

    public static String minutes(Double m) {
        return m == null ? "–" : Math.round(m) + " min";
    }

    /**
     * Enregistre les statistiques en CSV pour Excel (point-virgule, UTF-8
     * avec BOM) : indicateurs, consultations par médecin, puis par période.
     */
    public void exporterCsv(Statistiques s, Map<Integer, medecin> medecins, File fichier) {
        acces.exiger(Role.ADMIN);
        StringBuilder csv = new StringBuilder();
        medecin m = s.getIdMedecin() == null ? null : medecins.get(s.getIdMedecin());
        ligne(csv, "Activité du cabinet", libellePeriode(s));
        ligne(csv, "Médecin", m == null ? "Tous les médecins" : m.toString());
        csv.append("\r\n");
        ligne(csv, "Indicateur", "Valeur");
        for (Map.Entry<String, String> e : indicateurs(s).entrySet()) {
            ligne(csv, e.getKey(), e.getValue());
        }
        csv.append("\r\n");
        ligne(csv, "Médecin", "Consultations");
        for (Map.Entry<Integer, Integer> e : s.getConsultationsParMedecin().entrySet()) {
            medecin x = medecins.get(e.getKey());
            ligne(csv, x == null ? "Médecin n° " + e.getKey() : x.toString(), String.valueOf(e.getValue()));
        }
        csv.append("\r\n");
        ligne(csv, "Période", "Consultations");
        for (Barre b : serie(s)) {
            ligne(csv, b.getDetail(), String.valueOf(b.getValeur()));
        }
        try (OutputStream out = new FileOutputStream(fichier)) {
            out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MetierException("Impossible d'enregistrer " + fichier.getAbsolutePath()
                    + " (est-il ouvert dans Excel ?).");
        }
    }

    private static void ligne(StringBuilder csv, String a, String b) {
        csv.append(cellule(a)).append(';').append(cellule(b)).append("\r\n");
    }

    private static String cellule(String v) {
        return v.matches(".*[;\"\\r\\n].*") ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }

    private static String majuscule(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
