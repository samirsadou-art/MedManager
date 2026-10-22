package My_application.report;

import My_application.entities.Rendez_Vous;
import My_application.entities.Statistiques;
import My_application.entities.analyse;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.medicament;
import My_application.entities.patient;
import My_application.service.MetierException;
import My_application.service.StatistiquesService;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Ordonnances et demandes d'analyses au format PDF, générées avec OpenPDF
 * (remplace JasperReports).
 * <p>
 * <ul>
 * <li>Plus de modèles .jrxml à compiler à l'exécution : la mise en page est
 * écrite en Java ({@link #pdfOrdonnance}, {@link #pdfAnalyses}), et le PDF est
 * produit en mémoire, ce qui permet de le tester.</li>
 * <li>Chaque document est enregistré dans un dossier d'archives (par défaut
 * {@code ~/GestiMed/Documents}, modifiable par {@code -Dgestimed.documents=...})
 * puis ouvert dans la visionneuse PDF du système, ou envoyé à l'imprimante.</li>
 * <li>Ces PDF nominatifs ne s'accumulent pas : ceux produits par GestiMed il y a
 * plus de {@code -Dgestimed.documents.conservation} jours (30 par défaut, 0 pour
 * tout garder) sont supprimés à chaque nouveau document. Rien n'est perdu : un
 * document se régénère à tout moment depuis le dossier du patient.</li>
 * </ul>
 * L'ouverture et l'impression passent par l'interface {@link Afficheur}, que
 * les tests remplacent pour ne rien ouvrir à l'écran.
 */
public class RapportService {

    /** Ouvre ou imprime un fichier PDF (par défaut via {@link java.awt.Desktop}). */
    public interface Afficheur {

        void ouvrir(File pdf) throws IOException;

        /**
         * Imprime le fichier ; renvoie false si l'impression directe n'est pas
         * disponible (le document est alors ouvert pour être imprimé depuis la visionneuse).
         */
        boolean imprimer(File pdf) throws IOException;
    }

    private static final Logger LOG = Logger.getLogger("GestiMed");

    /** Jours de conservation des PDF par défaut. */
    static final int CONSERVATION_PAR_DEFAUT = 30;

    /** Noms des fichiers produits par {@link #nomFichier} : seuls ceux-là sont purgés. */
    private static final Pattern NOM_DOCUMENT = Pattern.compile("(Ordonnance|Analyses|RendezVous|Statistiques)_[A-Za-z0-9_\\-]*\\.pdf");

    static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private static final Font TITRE = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(0, 51, 102));
    private static final Font CABINET = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font GRAS = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font ENTETE_TABLEAU = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font PETIT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
    private static final Color BLEU = new Color(0, 51, 102);
    private static final Color GRIS_CLAIR = new Color(240, 240, 240);

    private final File dossier;
    private final Afficheur afficheur;
    private final int joursConservation;

    public RapportService() {
        this(new File(System.getProperty("gestimed.documents",
                System.getProperty("user.home") + File.separator + "GestiMed" + File.separator + "Documents")),
                new AfficheurSysteme());
    }

    public RapportService(File dossier, Afficheur afficheur) {
        this(dossier, afficheur, Integer.getInteger("gestimed.documents.conservation", CONSERVATION_PAR_DEFAUT));
    }

    /** @param joursConservation 0 (ou moins) : aucun document n'est supprimé */
    public RapportService(File dossier, Afficheur afficheur, int joursConservation) {
        this.dossier = dossier;
        this.afficheur = afficheur;
        this.joursConservation = joursConservation;
    }

    // ----- actions appelées par le contrôleur -----

    /**
     * Enregistre l'ordonnance en PDF puis l'ouvre ou l'imprime.
     *
     * @return false si l'impression a été demandée mais que le document a
     *         seulement pu être ouvert (impression directe indisponible)
     */
    public boolean ordonnance(consultation c, boolean imprimer) {
        if (!c.hasOrdonnance()) {
            throw new MetierException("Cette consultation ne comporte pas d'ordonnance.");
        }
        return publier(nomFichier("Ordonnance", c), pdfOrdonnance(c), imprimer);
    }

    public boolean analyses(consultation c, boolean imprimer) {
        if (!c.hasAnalyse()) {
            throw new MetierException("Cette consultation ne comporte pas de demande d'analyses.");
        }
        return publier(nomFichier("Analyses", c), pdfAnalyses(c), imprimer);
    }

    /**
     * Liste des rendez-vous d'un jour, une page par médecin, pour appeler
     * les patients la veille ou pointer les arrivées.
     *
     * @param rdvs     rendez-vous du jour, triés par médecin puis par heure
     * @param medecins médecins du cabinet par identifiant (nom en tête de page)
     */
    public boolean listeRendezVous(LocalDate jour, List<Rendez_Vous> rdvs, Map<Integer, medecin> medecins,
            boolean imprimer) {
        if (rdvs.isEmpty()) {
            throw new MetierException("Aucun rendez-vous le " + FORMAT_DATE.format(jour) + ".");
        }
        return publier("RendezVous_" + jour + ".pdf", pdfListeRendezVous(jour, rdvs, medecins), imprimer);
    }

    /** Activité du cabinet sur une période (compte administrateur) : nombres seulement. */
    public boolean statistiques(Statistiques s, Map<Integer, medecin> medecins) {
        return publier("Statistiques_" + s.getDebut() + "_" + s.getFin() + ".pdf", pdfStatistiques(s, medecins), false);
    }

    private boolean publier(String nom, byte[] pdf, boolean imprimer) {
        File fichier = enregistrer(nom, pdf);
        purger(fichier, System.currentTimeMillis());
        try {
            if (imprimer) {
                return afficheur.imprimer(fichier);
            }
            afficheur.ouvrir(fichier);
            return true;
        } catch (IOException | UnsupportedOperationException e) {
            throw new RapportException("Le document a été enregistré dans :\n" + fichier.getAbsolutePath()
                    + "\nmais n'a pas pu être " + (imprimer ? "imprimé" : "ouvert")
                    + ". Vérifiez qu'une visionneuse PDF est installée.", e);
        }
    }

    File enregistrer(String nom, byte[] pdf) {
        if (!dossier.isDirectory() && !dossier.mkdirs()) {
            throw new RapportException("Impossible de créer le dossier des documents : " + dossier.getAbsolutePath(), null);
        }
        File f = new File(dossier, nom);
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(pdf);
        } catch (IOException e) {
            throw new RapportException("Impossible d'enregistrer le document " + f.getAbsolutePath()
                    + " (est-il ouvert dans une autre application ?)", e);
        }
        return f;
    }

    /**
     * Supprime les documents GestiMed plus anciens que la durée de conservation,
     * sauf {@code garde} (celui qu'on vient de produire). Les autres fichiers du
     * dossier ne sont jamais touchés. Un échec est journalisé sans bloquer.
     *
     * @return nombre de fichiers supprimés
     */
    int purger(File garde, long maintenant) {
        File[] fichiers = joursConservation > 0 ? dossier.listFiles() : null;
        if (fichiers == null) {
            return 0;
        }
        long limite = maintenant - TimeUnit.DAYS.toMillis(joursConservation);
        int supprimes = 0;
        for (File f : fichiers) {
            if (f.isFile() && !f.equals(garde) && NOM_DOCUMENT.matcher(f.getName()).matches()
                    && f.lastModified() < limite) {
                if (f.delete()) {
                    supprimes++;
                } else {
                    LOG.warning("Document ancien non supprimé (ouvert ailleurs ?) : " + f.getAbsolutePath());
                }
            }
        }
        if (supprimes > 0) {
            LOG.info(supprimes + " document(s) de plus de " + joursConservation + " jours supprimé(s) de " + dossier);
        }
        return supprimes;
    }

    /** Ex. « Ordonnance_AMRANI_Karim_2026-10-08_0930.pdf » (sans accents ni espaces). */
    static String nomFichier(String type, consultation c) {
        patient p = c.getPatient();
        String date = c.getDate_cons() == null ? LocalDate.now().toString() : c.getDate_cons().toString();
        String heure = c.getHeureString().replace(":", "");
        return nettoyer(type + "_" + p.getNom() + "_" + p.getPrenom() + "_" + date + (heure.isEmpty() ? "" : "_" + heure))
                + ".pdf";
    }

    private static String nettoyer(String s) {
        String sansAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    // ----- génération des PDF (testable, sans écran ni imprimante) -----

    public byte[] pdfOrdonnance(consultation c) {
        List<String[]> lignes = lignesOrdonnance(c.getOrdoPat().getMedicaments());
        return document(c, "ORDONNANCE",
                new String[]{"N°", "Médicament", "Unités", "Posologie", "Moment", "Durée"},
                new float[]{0.6f, 4.2f, 1.1f, 1.6f, 1.4f, 1.8f}, lignes);
    }

    public byte[] pdfAnalyses(consultation c) {
        List<String[]> lignes = lignesAnalyses(c.getAnaPat().getListeAnaly());
        return document(c, "ANALYSES BIOLOGIQUES",
                new String[]{"N°", "Analyse", "Observation"},
                new float[]{0.6f, 4.5f, 4.5f}, lignes);
    }

    public byte[] pdfListeRendezVous(LocalDate jour, List<Rendez_Vous> rdvs, Map<Integer, medecin> medecins) {
        Map<Integer, List<String[]>> parMedecin = lignesRendezVous(rdvs);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 70);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            medecin cabinet = medecins.isEmpty() ? new medecin() : medecins.values().iterator().next();
            writer.setPageEvent(new PiedDePage(cabinet));
            doc.addTitle("Rendez-vous du " + FORMAT_DATE.format(jour));
            doc.addCreator("MedManager");
            doc.open();
            boolean premiere = true;
            for (Map.Entry<Integer, List<String[]>> e : parMedecin.entrySet()) {
                if (!premiere) {
                    doc.newPage();
                }
                premiere = false;
                medecin m = e.getKey() == null ? null : medecins.get(e.getKey());
                PdfPTable entete = new PdfPTable(new float[]{3, 1.4f});
                entete.setWidthPercentage(100);
                PdfPCell gauche = sansBordure();
                gauche.addElement(new Paragraph(m == null ? texte(cabinet.getNom_cabinet())
                        : texte(m.getNom_cabinet()), CABINET));
                gauche.addElement(new Paragraph(m == null ? "Médecin non renseigné" : coordonneesMedecin(m).get(0),
                        NORMAL));
                entete.addCell(gauche);
                PdfPCell droite = sansBordure();
                Paragraph nombre = new Paragraph(e.getValue().size() + " rendez-vous", GRAS);
                nombre.setAlignment(Element.ALIGN_RIGHT);
                droite.addElement(nombre);
                entete.addCell(droite);
                doc.add(entete);
                doc.add(new Chunk(new LineSeparator(1f, 100f, BLEU, Element.ALIGN_CENTER, -4)));
                Paragraph t = new Paragraph("RENDEZ-VOUS DU " + FORMAT_JOUR.format(jour).toUpperCase(Locale.FRENCH),
                        TITRE);
                t.setAlignment(Element.ALIGN_CENTER);
                t.setSpacingBefore(18);
                t.setSpacingAfter(14);
                doc.add(t);
                doc.add(tableau(new String[]{"Heure", "Patient", "Téléphone", "Motif", "Observation"},
                        new float[]{0.9f, 3.2f, 1.8f, 2.4f, 2.2f}, e.getValue()));
            }
            doc.close();
        } catch (DocumentException e) {
            throw new RapportException("Impossible de générer le document.", e);
        }
        return out.toByteArray();
    }

    public byte[] pdfStatistiques(Statistiques s, Map<Integer, medecin> medecins) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 70);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            medecin cabinet = medecins.isEmpty() ? new medecin() : medecins.values().iterator().next();
            writer.setPageEvent(new PiedDePage(cabinet));
            doc.addTitle("Activité du cabinet " + StatistiquesService.libellePeriode(s));
            doc.addCreator("MedManager");
            doc.open();
            doc.add(new Paragraph(texte(cabinet.getNom_cabinet()), CABINET));
            medecin choisi = s.getIdMedecin() == null ? null : medecins.get(s.getIdMedecin());
            doc.add(new Paragraph(choisi == null ? "Tous les médecins" : coordonneesMedecin(choisi).get(0), NORMAL));
            doc.add(new Chunk(new LineSeparator(1f, 100f, BLEU, Element.ALIGN_CENTER, -4)));
            Paragraph t = new Paragraph("ACTIVITÉ DU CABINET", TITRE);
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingBefore(18);
            doc.add(t);
            Paragraph periode = new Paragraph(StatistiquesService.libellePeriode(s), GRAS);
            periode.setAlignment(Element.ALIGN_CENTER);
            periode.setSpacingAfter(14);
            doc.add(periode);

            List<String[]> lignes = new ArrayList<>();
            for (Map.Entry<String, String> e : StatistiquesService.indicateurs(s).entrySet()) {
                lignes.add(new String[] {e.getKey(), e.getValue()});
            }
            doc.add(tableau(new String[] {"Indicateur", "Valeur"}, new float[] {4, 1.5f}, lignes));

            if (!s.getConsultationsParMedecin().isEmpty()) {
                lignes = new ArrayList<>();
                for (Map.Entry<Integer, Integer> e : s.getConsultationsParMedecin().entrySet()) {
                    medecin m = medecins.get(e.getKey());
                    lignes.add(new String[] {m == null ? "Médecin n° " + e.getKey() : coordonneesMedecin(m).get(0),
                        String.valueOf(e.getValue())});
                }
                doc.add(sousTitre("Consultations par médecin"));
                doc.add(tableau(new String[] {"Médecin", "Consultations"}, new float[] {4, 1.5f}, lignes));
            }

            lignes = new ArrayList<>();
            for (StatistiquesService.Barre b : StatistiquesService.serie(s)) {
                lignes.add(new String[] {b.getDetail(), String.valueOf(b.getValeur())});
            }
            doc.add(sousTitre("Consultations dans le temps"));
            doc.add(tableau(new String[] {"Période", "Consultations"}, new float[] {4, 1.5f}, lignes));
            Paragraph note = new Paragraph("Nombres agrégés, sans aucune donnée nominative.", PETIT);
            note.setSpacingBefore(12);
            doc.add(note);
            doc.close();
        } catch (DocumentException e) {
            throw new RapportException("Impossible de générer le document.", e);
        }
        return out.toByteArray();
    }

    private static Paragraph sousTitre(String texte) {
        Paragraph p = new Paragraph(texte, CABINET);
        p.setSpacingBefore(16);
        p.setSpacingAfter(8);
        return p;
    }

    private byte[] document(consultation c, String titre, String[] colonnes, float[] largeurs, List<String[]> lignes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 70);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PiedDePage(c.getMedecin()));
            doc.addTitle(titre + " - " + c.getPatient().getNom() + " " + c.getPatient().getPrenom());
            doc.addCreator("MedManager");
            doc.open();
            entete(doc, c);
            Paragraph t = new Paragraph(titre, TITRE);
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingBefore(18);
            t.setSpacingAfter(14);
            doc.add(t);
            doc.add(lignePatient(c.getPatient()));
            doc.add(tableau(colonnes, largeurs, lignes));
            Paragraph signature = new Paragraph("Signature et cachet du médecin", GRAS);
            signature.setAlignment(Element.ALIGN_RIGHT);
            signature.setSpacingBefore(40);
            doc.add(signature);
            doc.close();
        } catch (DocumentException e) {
            throw new RapportException("Impossible de générer le document.", e);
        }
        return out.toByteArray();
    }

    private static void entete(Document doc, consultation c) throws DocumentException {
        medecin m = c.getMedecin();
        PdfPTable entete = new PdfPTable(new float[]{3, 1.4f});
        entete.setWidthPercentage(100);

        PdfPCell gauche = sansBordure();
        gauche.addElement(new Paragraph(texte(m.getNom_cabinet()), CABINET));
        for (String ligne : coordonneesMedecin(m)) {
            gauche.addElement(new Paragraph(ligne, NORMAL));
        }
        entete.addCell(gauche);

        PdfPCell droite = sansBordure();
        Paragraph date = new Paragraph("Date : " + dateConsultation(c), GRAS);
        date.setAlignment(Element.ALIGN_RIGHT);
        droite.addElement(date);
        entete.addCell(droite);

        doc.add(entete);
        doc.add(new Chunk(new LineSeparator(1f, 100f, BLEU, Element.ALIGN_CENTER, -4)));
    }

    private static Paragraph lignePatient(patient p) {
        Paragraph para = new Paragraph();
        para.add(new Chunk("Patient : ", GRAS));
        para.add(new Chunk(texte(p.getNom()) + " " + texte(p.getPrenom()), NORMAL));
        para.add(new Chunk("        Âge : ", GRAS));
        para.add(new Chunk(p.getStringAge(), NORMAL));
        para.setSpacingAfter(12);
        return para;
    }

    private static PdfPTable tableau(String[] colonnes, float[] largeurs, List<String[]> lignes) {
        PdfPTable t = new PdfPTable(largeurs);
        t.setWidthPercentage(100);
        t.setHeaderRows(1); // en-tête répété si le tableau passe sur une autre page
        for (String col : colonnes) {
            PdfPCell cell = new PdfPCell(new Phrase(col, ENTETE_TABLEAU));
            cell.setBackgroundColor(BLEU);
            cell.setPadding(5);
            t.addCell(cell);
        }
        boolean pair = false;
        for (String[] ligne : lignes) {
            for (String valeur : ligne) {
                PdfPCell cell = new PdfPCell(new Phrase(valeur, NORMAL));
                cell.setPadding(5);
                if (pair) {
                    cell.setBackgroundColor(GRIS_CLAIR);
                }
                t.addCell(cell);
            }
            pair = !pair;
        }
        return t;
    }

    private static PdfPCell sansBordure() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    /** Adresse et téléphone du cabinet en bas de chaque page, avec le numéro de page. */
    private static final class PiedDePage extends PdfPageEventHelper {
        private final medecin m;

        PiedDePage(medecin m) {
            this.m = m;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float x = (document.left() + document.right()) / 2;
            float y = document.bottom() - 20;
            cb.setColorStroke(BLEU);
            cb.moveTo(document.left(), y + 12);
            cb.lineTo(document.right(), y + 12);
            cb.stroke();
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(adresse(m), PETIT), x, y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(telephone(m), PETIT), x, y - 11, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase("Page " + writer.getPageNumber(), PETIT),
                    document.right(), y - 22, 0);
        }
    }

    // ----- contenu (fonctions pures, testées) -----

    static List<String> coordonneesMedecin(medecin m) {
        List<String> l = new ArrayList<>();
        l.add((texte(m.getTitre()) + " " + texte(m.getNom()) + " " + texte(m.getPrenom())).trim());
        if (!texte(m.getSpecialite()).isEmpty()) {
            l.add(m.getSpecialite());
        }
        if (!texte(m.getOrdmedcin()).isEmpty()) {
            l.add("Inscrit à l'ordre N° : " + m.getOrdmedcin());
        }
        return l;
    }

    static String adresse(medecin m) {
        return "Adresse : " + texte(m.getAdresse());
    }

    /** Le second numéro est facultatif (l'ancien code levait une NullPointerException). */
    static String telephone(medecin m) {
        String tel = "Téléphone : " + texte(m.getTelephone1());
        if (!texte(m.getTelephone2()).isEmpty()) {
            tel += " - " + m.getTelephone2();
        }
        if (!texte(m.getEmail()).isEmpty()) {
            tel += "    Email : " + m.getEmail();
        }
        return tel;
    }

    static String dateConsultation(consultation c) {
        LocalDate d = c.getDate_cons() == null ? LocalDate.now() : c.getDate_cons();
        return FORMAT_DATE.format(d);
    }

    static List<String[]> lignesOrdonnance(List<medicament> medicaments) {
        List<String[]> lignes = new ArrayList<>();
        int i = 1;
        for (medicament med : medicaments) {
            String moment;
            switch (med.getMode()) {
                case medicament.MODE_SOIR:
                    moment = "le soir";
                    break;
                case medicament.MODE_MATIN:
                    moment = "le matin";
                    break;
                default:
                    moment = "";
            }
            lignes.add(new String[]{
                i++ + ")",
                texte(med.getDesignmed()),
                med.getUnite() + " U",
                med.getPosologie() + " fois/jour",
                moment,
                med.getDuree() == 0 ? "" : "pendant " + med.getDuree() + " jours"});
        }
        return lignes;
    }

    static List<String[]> lignesAnalyses(List<analyse> analyses) {
        List<String[]> lignes = new ArrayList<>();
        int i = 1;
        for (analyse a : analyses) {
            lignes.add(new String[]{i++ + ")", texte(a.getDesign()), texte(a.getObserv())});
        }
        return lignes;
    }

    /** Lignes du tableau par médecin (ordre de la liste) : heure, patient, téléphone, motif, case vide. */
    static Map<Integer, List<String[]>> lignesRendezVous(List<Rendez_Vous> rdvs) {
        Map<Integer, List<String[]>> l = new LinkedHashMap<>();
        for (Rendez_Vous r : rdvs) {
            List<String[]> lignes = l.get(r.getIdMedecin());
            if (lignes == null) {
                lignes = new ArrayList<>();
                l.put(r.getIdMedecin(), lignes);
            }
            patient p = r.getPatient();
            lignes.add(new String[]{
                r.getHeureString(),
                (texte(p.getNom()).toUpperCase(Locale.FRENCH) + " " + texte(p.getPrenom())).trim(),
                texte(p.getTelephone()),
                texte(r.getMotif()),
                ""});
        }
        return l;
    }

    private static String texte(String s) {
        return s == null ? "" : s;
    }
}
