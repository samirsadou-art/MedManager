package My_application.service;

import My_application.dao.ConsultationDao;
import My_application.dao.DAOFactory;
import My_application.dao.DemandeAnalyseDao;
import My_application.dao.MedecinDao;
import My_application.dao.PrescriptionDao;
import My_application.dao.UniteDeTravail;
import My_application.entities.DossierPatient;
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

    public DossierService(DAOFactory daos) {
        this(daos, JournalAcces.AUCUN);
    }

    public DossierService(DAOFactory daos, JournalAcces journal) {
        this.daos = daos;
        this.journal = journal;
        this.consultationDao = daos.getConsultationDao();
        this.medecinDao = daos.getMedecinDao();
        this.prescriptionDao = daos.getPrescriptionDao();
        this.demandeAnalyseDao = daos.getDemandeAnalyseDao();
    }

    /** Charge le dossier du patient avec toutes ses consultations détaillées. */
    public DossierPatient ouvrirDossier(patient malade, medecin medecinTraitant) {
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
     * Enregistre en une seule transaction la nouvelle consultation (en-tête,
     * symptômes, pathologies, ordonnance, analyses) et les résultats
     * d'analyses modifiés. En cas d'erreur, rien n'est enregistré.
     *
     * @return le nombre de consultations écrites
     */
    public int enregistrer(final DossierPatient dossier) {
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
        if (c.hasAanlyse()) {
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

    public List<consultation> consultationsDuJour(LocalDate jour) {
        return consultationDao.findByDate(jour);
    }
}
