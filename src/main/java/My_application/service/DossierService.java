package My_application.service;

import My_application.dao.ConsultationDao;
import My_application.dao.DAOFactory;
import My_application.dao.DemandeAnalyseDao;
import My_application.dao.MedecinDao;
import My_application.dao.PrescriptionDao;
import My_application.dao.UniteDeTravail;
import My_application.entities.Antecedent;
import My_application.entities.DossierPatient;
import My_application.entities.NoteDossier;
import My_application.entities.Role;
import My_application.entities.analyse;
import My_application.entities.analyseBio;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.medicament;
import My_application.entities.ordonnance;
import My_application.entities.pathologie;
import My_application.entities.patient;
import My_application.entities.symptome;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dossier médical : chargement complet d'un dossier et enregistrement
 * transactionnel des consultations.
 * <p>
 * Remplace {@code DossierPatient.ovrirDossier()}, {@code consultation.InsererToBD()},
 * {@code ordonnance.InsererToDB()}, {@code analyseBio.InsererToBD()/update()}.
 */
public class DossierService {

    private final DAOFactory daos;
    private final ConsultationDao consultationDao;
    private final MedecinDao medecinDao;
    private final PrescriptionDao prescriptionDao;
    private final DemandeAnalyseDao demandeAnalyseDao;
    private final JournalAcces journal;
    private final ControleAcces acces;

    public DossierService(DAOFactory daos) {
        this(daos, JournalAcces.AUCUN);
    }

    public DossierService(DAOFactory daos, JournalAcces journal) {
        this(daos, journal, ControleAcces.AUCUN);
    }

    /** @param acces vérifie que l'utilisateur connecté est médecin (secret médical) */
    public DossierService(DAOFactory daos, JournalAcces journal, ControleAcces acces) {
        this.daos = daos;
        this.journal = journal;
        this.acces = acces;
        this.consultationDao = daos.getConsultationDao();
        this.medecinDao = daos.getMedecinDao();
        this.prescriptionDao = daos.getPrescriptionDao();
        this.demandeAnalyseDao = daos.getDemandeAnalyseDao();
    }

    /** Charge le dossier du patient avec toutes ses consultations détaillées. */
    public DossierPatient ouvrirDossier(patient malade, medecin medecinTraitant) {
        acces.exiger(Role.MEDECIN);
        if (malade == null) {
            throw new MetierException("Aucun patient sélectionné.");
        }
        DossierPatient dossier = new DossierPatient(malade, medecinTraitant);
        Map<Integer, medecin> medecins = new HashMap<>();
        if (medecinTraitant != null) {
            medecins.put(medecinTraitant.getIdmedecin(), medecinTraitant);
        }
        // Cinq requêtes pour tout le dossier, quel que soit le nombre de
        // consultations (au lieu de quatre requêtes par consultation).
        int idPatient = malade.getIdentPat();
        Map<Integer, List<symptome>> symptomes = consultationDao.findSymptomesParPatient(idPatient);
        Map<Integer, List<pathologie>> pathologies = consultationDao.findPathologiesParPatient(idPatient);
        Map<Integer, List<medicament>> prescriptions = prescriptionDao.findByPatient(idPatient);
        Map<Integer, List<analyse>> analyses = demandeAnalyseDao.findByPatient(idPatient);
        List<consultation> consultations = new ArrayList<>();
        for (consultation c : consultationDao.findByPatient(idPatient)) {
            c.setMalade(malade);
            c.setMedTraitant(medecin(medecins, c.getMedecin().getIdmedecin()));
            int id = c.getIdConsul();
            for (symptome s : lignes(symptomes, id)) {
                c.addSymptome(s);
            }
            for (pathologie p : lignes(pathologies, id)) {
                c.addPathologie(p);
            }
            ordonnance ordo = new ordonnance();
            ordo.setMedicaments(lignes(prescriptions, id));
            c.setOrdoPat(ordo);
            analyseBio ana = new analyseBio();
            ana.setListeAnaly(lignes(analyses, id));
            c.setAnaPat(ana);
            consultations.add(c);
        }
        dossier.setConsultations(consultations);
        journal.noter(JournalAcces.Action.OUVERTURE_DOSSIER, idPatient, null);
        return dossier;
    }

    private static <T> List<T> lignes(Map<Integer, List<T>> parConsultation, int idConsultation) {
        List<T> l = parConsultation.get(idConsultation);
        return l != null ? l : new ArrayList<T>();
    }

    /** Un seul accès base par médecin, même s'il a signé 50 consultations. */
    private medecin medecin(Map<Integer, medecin> cache, int id) {
        medecin m = cache.get(id);
        if (m == null) {
            m = medecinDao.findById(id);
            if (m == null) {
                m = new medecin();
                m.setIdmedecin(id);
                m.setNom("(médecin inconnu)");
            }
            cache.put(id, m);
        }
        return m;
    }

    /** Crée une nouvelle consultation (non enregistrée) datée de maintenant. */
    public consultation nouvelleConsultation(DossierPatient dossier) {
        acces.exiger(Role.MEDECIN);
        if (dossier.getConsultationNonEnregistree() != null) {
            throw new MetierException("Une nouvelle consultation est déjà en cours de saisie.");
        }
        consultation c = new consultation(dossier.getMalade(), dossier.getMedTraitant());
        LocalDateTime maintenant = LocalDateTime.now().withNano(0);
        c.setDate_cons(maintenant.toLocalDate());
        c.setHeure_cons(maintenant.toLocalTime());
        dossier.ajouterConsultation(c);
        return c;
    }

    /** Vrai s'il y a quelque chose à enregistrer. */
    public boolean aDesModifications(DossierPatient dossier) {
        for (consultation c : dossier.getConsultations()) {
            if (c.estNouvelle() || c.isChange()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vrai si l'enregistrement terminera une consultation d'un patient qui
     * est dans la file d'un médecin : le médecin indique alors les frais que
     * la secrétaire encaissera.
     */
    public boolean fraisADemander(DossierPatient dossier) {
        acces.exiger(Role.MEDECIN);
        consultation c = dossier.getConsultationNonEnregistree();
        return c != null && SalleAttenteService.estDansLaFile(daos, dossier.getMalade().getIdentPat(),
                LocalDate.now());
    }

    /** Frais proposés au médecin : les derniers qu'il a fixés, ou null. */
    public Integer fraisProposes(DossierPatient dossier) {
        acces.exiger(Role.MEDECIN);
        medecin m = dossier.getMedTraitant();
        return m == null ? null : SalleAttenteService.dernierFrais(daos, m.getIdmedecin());
    }

    /** Même chose sans frais précisés. */
    public int enregistrer(final DossierPatient dossier) {
        return enregistrer(dossier, null);
    }

    /**
     * Enregistre en une seule transaction la nouvelle consultation (en-tête,
     * symptômes, pathologies, ordonnance, analyses) et les résultats
     * d'analyses modifiés. En cas d'erreur, rien n'est enregistré.
     *
     * @param frais frais de la nouvelle consultation, en dinars, que la
     *              secrétaire encaissera (null : non précisés)
     * @return le nombre de consultations écrites
     */
    public int enregistrer(final DossierPatient dossier, final Integer frais) {
        acces.exiger(Role.MEDECIN);
        SalleAttenteService.verifierMontant(frais);
        final List<consultation> aEcrire = new ArrayList<>();
        for (consultation c : dossier.getConsultations()) {
            if (c.estNouvelle()) {
                validerAvantEnregistrement(c);
                aEcrire.add(c);
            } else if (c.isChange()) {
                aEcrire.add(c);
            }
        }
        if (aEcrire.isEmpty()) {
            return 0;
        }
        final List<consultation> nouvelles = new ArrayList<>();
        for (consultation c : aEcrire) {
            if (c.estNouvelle()) {
                nouvelles.add(c);
            }
        }
        try {
            daos.enTransaction(new UniteDeTravail<Void>() {
                @Override
                public Void executer() {
                    for (consultation c : aEcrire) {
                        if (nouvelles.contains(c)) {
                            inserer(c);
                            journal.noter(JournalAcces.Action.ENREGISTREMENT_CONSULTATION,
                                    c.getPatient().getIdentPat(), "consultation n° " + c.getIdConsul());
                            // Le patient quitte la file du médecin, à régler (son rendez-vous est terminé).
                            SalleAttenteService.cloreConsultation(daos, c.getPatient().getIdentPat(),
                                    LocalDateTime.now(), frais);
                        } else {
                            demandeAnalyseDao.updateResultats(c.getIdConsul(), c.getAnaPat().getListeAnaly());
                            journal.noter(JournalAcces.Action.SAISIE_RESULTATS,
                                    c.getPatient().getIdentPat(), "consultation n° " + c.getIdConsul());
                        }
                    }
                    return null;
                }
            });
        } catch (RuntimeException e) {
            // Transaction annulée : les consultations insérées redeviennent « nouvelles ».
            for (consultation c : nouvelles) {
                c.setIdConsul(0);
            }
            throw e;
        }
        // Après le commit seulement : en cas d'échec, l'état reste « à enregistrer ».
        for (consultation c : aEcrire) {
            if (c.getAnaPat() != null) {
                c.getAnaPat().setChange(false);
            }
        }
        return aEcrire.size();
    }

    private void inserer(consultation c) {
        consultationDao.insert(c);
        int id = c.getIdConsul();
        consultationDao.ajouterSymptomes(id, c.getSymptomes());
        consultationDao.ajouterPathologies(id, c.getPathologies());
        if (c.hasOrdonnance()) {
            prescriptionDao.insertAll(id, c.getOrdoPat().getMedicaments());
        }
        if (c.hasAnalyse()) {
            demandeAnalyseDao.insertAll(id, c.getAnaPat().getListeAnaly());
        }
    }

    static void validerAvantEnregistrement(consultation c) {
        if (c.getPatient() == null || c.getPatient().getIdentPat() <= 0) {
            throw new MetierException("La consultation n'est rattachée à aucun patient.");
        }
        if (c.getMedecin() == null || c.getMedecin().getIdmedecin() <= 0) {
            throw new MetierException("La consultation n'est rattachée à aucun médecin.");
        }
        if (c.getDate_cons() == null || c.getHeure_cons() == null) {
            throw new MetierException("La date de la consultation est manquante.");
        }
    }

    /**
     * Rendez-vous et passages au cabinet du patient (avec les frais et les
     * règlements), pour les onglets de son dossier.
     */
    public HistoriquePatient historique(patient malade) {
        acces.exiger(Role.MEDECIN);
        if (malade == null) {
            throw new MetierException("Aucun patient sélectionné.");
        }
        int id = malade.getIdentPat();
        return new HistoriquePatient(daos.getRendezVousDao().findDesPatients(Collections.singletonList(id)),
                daos.getSalleAttenteDao().findDuPatient(id), medecinDao.findAll());
    }

    // ----- antécédents et notes (V18) -----

    public static final int LONGUEUR_LIBELLE = 200;
    public static final int LONGUEUR_COMPLEMENT = 255;
    public static final int LONGUEUR_NOTE = 4000;

    /** Antécédents notés dans le dossier, par rubrique. */
    public List<Antecedent> antecedents(patient malade) {
        acces.exiger(Role.MEDECIN);
        return daos.getDossierCliniqueDao().findAntecedents(idPatient(malade));
    }

    /** Notes du médecin, de la plus récente à la plus ancienne. */
    public List<NoteDossier> notes(patient malade) {
        acces.exiger(Role.MEDECIN);
        return daos.getDossierCliniqueDao().findNotes(idPatient(malade));
    }

    /**
     * Note un antécédent dans le dossier, tout de suite (sans attendre la
     * fermeture du dossier), au nom du médecin du dossier.
     *
     * @throws MetierException libellé vide ou trop long, ou déjà noté dans cette rubrique
     */
    public Antecedent ajouterAntecedent(DossierPatient dossier, Antecedent.Categorie categorie, String libelle,
            String complement) {
        acces.exiger(Role.MEDECIN);
        if (categorie == null) {
            throw new MetierException("Choisissez la rubrique de l'antécédent.");
        }
        String texte = obligatoire(libelle, "Indiquez l'antécédent.", LONGUEUR_LIBELLE);
        String precision = facultatif(complement, LONGUEUR_COMPLEMENT);
        int id = idPatient(dossier.getMalade());
        for (Antecedent a : daos.getDossierCliniqueDao().findAntecedents(id)) {
            if (a.getCategorie() == categorie && a.getLibelle().equalsIgnoreCase(texte)) {
                throw new MetierException("« " + texte + " » est déjà noté dans « " + categorie.getLibelle() + " ».");
            }
        }
        final Antecedent a = new Antecedent();
        a.setIdPatient(id);
        a.setCategorie(categorie);
        a.setLibelle(texte);
        a.setComplement(precision);
        a.setIdMedecin(idMedecin(dossier));
        a.setSaisiLe(LocalDateTime.now().withNano(0));
        daos.enTransaction(() -> {
            daos.getDossierCliniqueDao().insert(a);
            journal.noter(JournalAcces.Action.ANTECEDENT_AJOUTE, id, categorie.name() + " : " + texte);
            return null;
        });
        return a;
    }

    public void supprimerAntecedent(final Antecedent a) {
        acces.exiger(Role.MEDECIN);
        daos.enTransaction(() -> {
            if (!daos.getDossierCliniqueDao().supprimerAntecedent(a.getIdAntecedent())) {
                throw new MetierException("Cet antécédent a déjà été retiré du dossier.");
            }
            journal.noter(JournalAcces.Action.ANTECEDENT_SUPPRIME, a.getIdPatient(),
                    a.getCategorie().name() + " : " + a.getLibelle());
            return null;
        });
    }

    /** Ajoute une note datée au dossier, au nom du médecin du dossier. */
    public NoteDossier ajouterNote(DossierPatient dossier, String texte) {
        acces.exiger(Role.MEDECIN);
        final NoteDossier n = new NoteDossier();
        n.setIdPatient(idPatient(dossier.getMalade()));
        n.setTexte(obligatoire(texte, "La note est vide.", LONGUEUR_NOTE));
        n.setIdMedecin(idMedecin(dossier));
        n.setEcriteLe(LocalDateTime.now().withNano(0));
        daos.enTransaction(() -> {
            daos.getDossierCliniqueDao().insert(n);
            journal.noter(JournalAcces.Action.NOTE_AJOUTEE, n.getIdPatient(), "note n° " + n.getIdNote());
            return null;
        });
        return n;
    }

    public void supprimerNote(final NoteDossier n) {
        acces.exiger(Role.MEDECIN);
        daos.enTransaction(() -> {
            if (!daos.getDossierCliniqueDao().supprimerNote(n.getIdNote())) {
                throw new MetierException("Cette note a déjà été supprimée.");
            }
            journal.noter(JournalAcces.Action.NOTE_SUPPRIMEE, n.getIdPatient(), "note n° " + n.getIdNote());
            return null;
        });
    }

    private static int idPatient(patient malade) {
        if (malade == null) {
            throw new MetierException("Aucun patient sélectionné.");
        }
        return malade.getIdentPat();
    }

    private static Integer idMedecin(DossierPatient dossier) {
        medecin m = dossier.getMedTraitant();
        return m == null || m.getIdmedecin() <= 0 ? null : m.getIdmedecin();
    }

    private static String obligatoire(String texte, String siVide, int longueur) {
        String t = texte == null ? "" : texte.trim();
        if (t.isEmpty()) {
            throw new MetierException(siVide);
        }
        if (t.length() > longueur) {
            throw new MetierException("Texte trop long (" + longueur + " caractères au plus).");
        }
        return t;
    }

    private static String facultatif(String texte, int longueur) {
        String t = texte == null ? "" : texte.trim();
        if (t.length() > longueur) {
            throw new MetierException("Précision trop longue (" + longueur + " caractères au plus).");
        }
        return t.isEmpty() ? null : t;
    }

    /** Consultations du jour (motifs et diagnostics compris) : réservé aux médecins. */
    public List<consultation> consultationsDuJour(LocalDate jour) {
        acces.exiger(Role.MEDECIN);
        return consultationDao.findByDate(jour);
    }
}
