package My_application.fake;

import My_application.dao.AnalyseDao;
import My_application.dao.ConsultationDao;
import My_application.dao.DAOFactory;
import My_application.dao.DataAccessException;
import My_application.dao.DemandeAnalyseDao;
import My_application.dao.DoublonException;
import My_application.dao.EvenementDao;
import My_application.dao.JournalDao;
import My_application.dao.MedecinDao;
import My_application.dao.MedicamentDao;
import My_application.dao.MessageDao;
import My_application.dao.PathologieDao;
import My_application.dao.PatientDao;
import My_application.dao.PrescriptionDao;
import My_application.dao.PresenceDao;
import My_application.dao.RendezVousDao;
import My_application.dao.SalleAttenteDao;
import My_application.dao.SymptomeDao;
import My_application.dao.UniteDeTravail;
import My_application.dao.UtilisateurDao;
import My_application.entities.CategAnalyse;
import My_application.entities.Compte;
import My_application.entities.EntreeJournal;
import My_application.entities.MessageInterne;
import My_application.entities.Passage;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.analyse;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.medicament;
import My_application.entities.pathologie;
import My_application.entities.patient;
import My_application.entities.symptome;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implémentation EN MÉMOIRE de toute la couche DAO, pour les tests.
 * <p>
 * C'est l'intérêt du patron DAO + fabrique abstraite : les services et les
 * contrôleurs sont testés sans MySQL, simplement en leur donnant cette
 * fabrique. Elle simule aussi :
 * <ul>
 * <li>les transactions (instantané de l'état, restauré en cas d'erreur) ;</li>
 * <li>une panne de base sur une opération donnée ({@link #echouerSur(String)}).</li>
 * </ul>
 */
public class MemoireDAOFactory implements DAOFactory {

    // ----- « tables » -----
    public final Map<Integer, patient> patients = new LinkedHashMap<>();
    public final Map<Integer, medecin> medecins = new HashMap<>();
    public final Map<Integer, User> utilisateurs = new HashMap<>();
    /** Comptes : identifiant et empreinte par idUtilisateur, et comptes désactivés. */
    public final Map<Integer, String> logins = new HashMap<>();
    public final Map<Integer, String> empreintes = new HashMap<>();
    public final Set<Integer> inactifs = new HashSet<>();
    /** Dernière connexion par idUtilisateur. */
    public final Map<Integer, java.time.LocalDateTime> connexions = new HashMap<>();
    public final List<EntreeJournal> journal = new ArrayList<>();
    public final List<consultation> consultations = new ArrayList<>();
    public final List<int[]> consultSymptome = new ArrayList<>();
    public final List<int[]> consultPathologie = new ArrayList<>();
    public final List<symptome> symptomes = new ArrayList<>();
    public final List<pathologie> pathologies = new ArrayList<>();
    public final List<medicament> medicaments = new ArrayList<>();
    public final Map<Integer, String> formes = new HashMap<>();
    public final Map<Integer, List<medicament>> prescriptions = new HashMap<>();
    public final List<analyse> catalogueAnalyses = new ArrayList<>();
    public final Map<Integer, Integer> categorieDeLAnalyse = new HashMap<>();
    public final List<CategAnalyse> categories = new ArrayList<>();
    public final Map<Integer, List<analyse>> demandes = new HashMap<>();
    public final List<Rendez_Vous> rendezVous = new ArrayList<>();
    public final List<Passage> passages = new ArrayList<>();
    /** Événements publiés : type et identifiant de l'objet. */
    public final List<String> evenements = new ArrayList<>();
    public final List<MessageInterne> messages = new ArrayList<>();
    /** Accusés de lecture « idMessage:idUtilisateur ». */
    public final Set<String> lus = new HashSet<>();
    /** Membres en ligne, et le poste de chacun. */
    public final Map<Integer, String> presents = new HashMap<>();

    // ----- instrumentation -----
    public int commits;
    public int rollbacks;
    public int requetes;
    private String operationEnEchec;
    private RuntimeException erreurSimulee;
    private int sequence = 100;

    /** La prochaine exécution de cette opération lèvera une DataAccessException. */
    public void echouerSur(String operation) {
        echouerSur(operation, new DataAccessException("Panne simulée sur " + operation));
    }

    /** Idem, avec l'exception à lever (ex. DoublonException pour une clé déjà prise). */
    public void echouerSur(String operation, RuntimeException erreur) {
        this.operationEnEchec = operation;
        this.erreurSimulee = erreur;
    }

    private void appel(String operation) {
        requetes++;
        if (operation.equals(operationEnEchec)) {
            operationEnEchec = null;
            throw erreurSimulee;
        }
    }

    private List<Integer> idsConsultations(int idPatient) {
        List<Integer> ids = new ArrayList<>();
        for (consultation c : consultations) {
            if (c.getPatient().getIdentPat() == idPatient) {
                ids.add(c.getIdConsul());
            }
        }
        return ids;
    }

    private int nouvelId() {
        return ++sequence;
    }

    // ----- transactions simulées -----

    @Override
    public <T> T enTransaction(UniteDeTravail<T> travail) {
        Etat avant = new Etat();
        try {
            T r = travail.executer();
            commits++;
            return r;
        } catch (RuntimeException e) {
            rollbacks++;
            avant.restaurer();
            throw e;
        }
    }

    /** Instantané (copie superficielle) des collections. */
    private final class Etat {
        final List<consultation> c = new ArrayList<>(consultations);
        final List<int[]> cs = new ArrayList<>(consultSymptome);
        final List<int[]> cp = new ArrayList<>(consultPathologie);
        final Map<Integer, List<medicament>> pr = copie(prescriptions);
        final Map<Integer, List<analyse>> de = copie(demandes);
        final List<Rendez_Vous> rv = new ArrayList<>(rendezVous);
        final List<Passage> sa = new ArrayList<>(passages);
        final List<String> ev = new ArrayList<>(evenements);
        final List<MessageInterne> me = new ArrayList<>(messages);
        final Set<String> lu = new HashSet<>(lus);
        final List<symptome> sy = new ArrayList<>(symptomes);
        final List<pathologie> pa = new ArrayList<>(pathologies);
        final Map<Integer, String> lo = new HashMap<>(logins);
        final Map<Integer, String> em = new HashMap<>(empreintes);
        final Set<Integer> in = new HashSet<>(inactifs);
        final List<EntreeJournal> jo = new ArrayList<>(journal);

        void restaurer() {
            remplacer(consultations, c);
            remplacer(consultSymptome, cs);
            remplacer(consultPathologie, cp);
            prescriptions.clear();
            prescriptions.putAll(pr);
            demandes.clear();
            demandes.putAll(de);
            remplacer(rendezVous, rv);
            remplacer(passages, sa);
            remplacer(evenements, ev);
            remplacer(messages, me);
            lus.clear();
            lus.addAll(lu);
            remplacer(symptomes, sy);
            remplacer(pathologies, pa);
            logins.clear();
            logins.putAll(lo);
            empreintes.clear();
            empreintes.putAll(em);
            inactifs.clear();
            inactifs.addAll(in);
            remplacer(journal, jo);
        }
    }

    private static <T> void remplacer(List<T> cible, List<T> source) {
        cible.clear();
        cible.addAll(source);
    }

    private static <T> Map<Integer, List<T>> copie(Map<Integer, List<T>> m) {
        Map<Integer, List<T>> r = new HashMap<>();
        for (Map.Entry<Integer, List<T>> e : m.entrySet()) {
            r.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return r;
    }

    // ----- DAO -----

    @Override
    public PatientDao getPatientDao() {
        return new PatientDao() {
            @Override
            public patient insert(patient p) {
                appel("patient.insert");
                if (p.getIdentPat() <= 0) {
                    p.setIdentPat(nouvelId());
                }
                patients.put(p.getIdentPat(), p);
                return p;
            }

            @Override
            public void update(patient p) {
                appel("patient.update");
                patients.put(p.getIdentPat(), p);
            }

            @Override
            public void delete(Integer id) {
                appel("patient.delete");
                patients.remove(id);
            }

            @Override
            public patient findById(Integer id) {
                appel("patient.findById");
                return patients.get(id);
            }

            @Override
            public List<patient> findAll() {
                return findByNomCommencantPar("");
            }

            @Override
            public List<patient> findByNomCommencantPar(String prefixe) {
                appel("patient.findByNom");
                List<patient> r = new ArrayList<>();
                for (patient p : patients.values()) {
                    if (p.getNom().toUpperCase().startsWith(prefixe.toUpperCase())) {
                        r.add(p);
                    }
                }
                r.sort(new Comparator<patient>() {
                    @Override
                    public int compare(patient a, patient b) {
                        return a.getNom().compareTo(b.getNom());
                    }
                });
                return r;
            }

            @Override
            public List<patient> findByTelephoneCommencantPar(String prefixe) {
                appel("patient.findByTelephone");
                List<patient> r = new ArrayList<>();
                for (patient p : patients.values()) {
                    if (p.getTelephone() != null && p.getTelephone().startsWith(prefixe)) {
                        r.add(p);
                    }
                }
                return r;
            }

            @Override
            public List<patient> findDoublons(String nom, java.time.LocalDate naissance, String telephone) {
                appel("patient.findDoublons");
                List<patient> r = new ArrayList<>();
                for (patient p : patients.values()) {
                    if ((p.getNom().equals(nom) && naissance.equals(p.getDat_nais()))
                            || (telephone != null && telephone.equals(p.getTelephone()))) {
                        r.add(p);
                    }
                }
                return r;
            }
        };
    }

    @Override
    public MedecinDao getMedecinDao() {
        return new MedecinDao() {
            @Override
            public medecin findById(int id) {
                appel("medecin.findById");
                return medecins.get(id);
            }

            @Override
            public List<medecin> findAll() {
                appel("medecin.findAll");
                List<medecin> r = new ArrayList<>(medecins.values());
                r.sort(new Comparator<medecin>() {
                    @Override
                    public int compare(medecin a, medecin b) {
                        int c = a.getNom().compareTo(b.getNom());
                        return c != 0 ? c : a.getPrenom().compareTo(b.getPrenom());
                    }
                });
                return r;
            }
        };
    }

    @Override
    public UtilisateurDao getUtilisateurDao() {
        return new UtilisateurDao() {
            @Override
            public User findById(int id) {
                appel("utilisateur.findById");
                return avecCompte(utilisateurs.get(id));
            }

            @Override
            public List<User> findAll() {
                appel("utilisateur.findAll");
                List<User> r = new ArrayList<>();
                for (User u : utilisateurs.values()) {
                    r.add(avecCompte(u));
                }
                return r;
            }

            @Override
            public Compte findCompte(String login) {
                appel("utilisateur.findCompte");
                for (Map.Entry<Integer, String> e : logins.entrySet()) {
                    if (e.getValue().equals(login)) {
                        return new Compte(avecCompte(utilisateurs.get(e.getKey())), empreintes.get(e.getKey()));
                    }
                }
                return null;
            }

            @Override
            public boolean existeUnCompte() {
                appel("utilisateur.existeUnCompte");
                for (Integer id : logins.keySet()) {
                    if (empreintes.get(id) != null) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void definirIdentifiants(int idUtilisateur, String login, String empreinte) {
                appel("utilisateur.definirIdentifiants");
                for (Map.Entry<Integer, String> e : logins.entrySet()) {
                    if (e.getValue().equals(login) && e.getKey() != idUtilisateur) {
                        throw new My_application.dao.DoublonException("login " + login + " déjà pris", null);
                    }
                }
                logins.put(idUtilisateur, login);
                empreintes.put(idUtilisateur, empreinte);
            }

            @Override
            public void definirActif(int idUtilisateur, boolean actif) {
                appel("utilisateur.definirActif");
                if (actif) {
                    inactifs.remove(idUtilisateur);
                } else {
                    inactifs.add(idUtilisateur);
                }
            }

            @Override
            public User insert(User u) {
                appel("utilisateur.insert");
                int id = 1;
                for (Integer k : utilisateurs.keySet()) {
                    id = Math.max(id, k + 1);
                }
                u.setIdent(id);
                utilisateurs.put(id, u);
                if (!u.isActif()) {
                    inactifs.add(id);
                }
                return u;
            }

            @Override
            public void definirRole(int idUtilisateur, My_application.entities.Role role, Integer idMedecin) {
                appel("utilisateur.definirRole");
                User u = utilisateurs.get(idUtilisateur);
                u.setRole(role);
                u.setIdMedecin(idMedecin);
            }

            @Override
            public void noterConnexion(int idUtilisateur, java.time.LocalDateTime quand) {
                appel("utilisateur.noterConnexion");
                connexions.put(idUtilisateur, quand);
            }
        };
    }

    private User avecCompte(User u) {
        if (u != null) {
            u.setLogin(logins.get(u.getIdent()));
            u.setActif(!inactifs.contains(u.getIdent()));
        }
        return u;
    }

    @Override
    public JournalDao getJournalDao() {
        return new JournalDao() {
            @Override
            public void ajouter(EntreeJournal entree) {
                appel("journal.ajouter");
                journal.add(entree);
            }

            @Override
            public List<EntreeJournal> findByPatient(int idPatient) {
                appel("journal.findByPatient");
                List<EntreeJournal> r = new ArrayList<>();
                for (int i = journal.size() - 1; i >= 0; i--) {
                    EntreeJournal e = journal.get(i);
                    if (e.getIdPatient() != null && e.getIdPatient() == idPatient) {
                        r.add(e);
                    }
                }
                return r;
            }
        };
    }

    @Override
    public ConsultationDao getConsultationDao() {
        return new ConsultationDao() {
            @Override
            public consultation insert(consultation c) {
                appel("consultation.insert");
                c.setIdConsul(nouvelId());
                consultations.add(c);
                return c;
            }

            @Override
            public List<consultation> findByPatient(int idPatient) {
                appel("consultation.findByPatient");
                List<consultation> r = new ArrayList<>();
                for (consultation c : consultations) {
                    if (c.getPatient().getIdentPat() == idPatient) {
                        // Comme le DAO JDBC : un en-tête neuf, médecin réduit à son id.
                        consultation copie = new consultation(null, null);
                        copie.setIdConsul(c.getIdConsul());
                        copie.setDate_cons(c.getDate_cons());
                        copie.setHeure_cons(c.getHeure_cons());
                        copie.setTemperature(c.getTemperature());
                        copie.setPul_Cardiaque(c.getPul_Cardiaque());
                        copie.setPressionA(c.getPressionA());
                        copie.setTauxRespir(c.getTauxRespir());
                        copie.setPoids(c.getPoids());
                        medecin m = new medecin();
                        m.setIdmedecin(c.getMedecin().getIdmedecin());
                        copie.setMedTraitant(m);
                        r.add(copie);
                    }
                }
                return r;
            }

            @Override
            public List<consultation> findByDate(LocalDate date) {
                appel("consultation.findByDate");
                List<consultation> r = new ArrayList<>();
                for (consultation c : consultations) {
                    if (c.getDate_cons().equals(date)) {
                        r.add(c);
                    }
                }
                return r;
            }

            @Override
            public void ajouterSymptomes(int idConsultation, List<symptome> liste) {
                appel("consultation.ajouterSymptomes");
                for (symptome s : liste) {
                    consultSymptome.add(new int[]{idConsultation, s.getId()});
                }
            }

            @Override
            public void ajouterPathologies(int idConsultation, List<pathologie> liste) {
                appel("consultation.ajouterPathologies");
                for (pathologie p : liste) {
                    consultPathologie.add(new int[]{idConsultation, p.getId()});
                }
            }

            @Override
            public List<symptome> findSymptomes(int idConsultation) {
                List<symptome> r = new ArrayList<>();
                for (int[] l : consultSymptome) {
                    if (l[0] == idConsultation) {
                        for (symptome s : symptomes) {
                            if (s.getId() == l[1]) {
                                r.add(s);
                            }
                        }
                    }
                }
                return r;
            }

            @Override
            public List<pathologie> findPathologies(int idConsultation) {
                List<pathologie> r = new ArrayList<>();
                for (int[] l : consultPathologie) {
                    if (l[0] == idConsultation) {
                        for (pathologie p : pathologies) {
                            if (p.getId() == l[1]) {
                                r.add(p);
                            }
                        }
                    }
                }
                return r;
            }

            @Override
            public Map<Integer, List<symptome>> findSymptomesParPatient(int idPatient) {
                appel("consultation.findSymptomesParPatient");
                Map<Integer, List<symptome>> r = new HashMap<>();
                for (int id : idsConsultations(idPatient)) {
                    r.put(id, findSymptomes(id));
                }
                return r;
            }

            @Override
            public Map<Integer, List<pathologie>> findPathologiesParPatient(int idPatient) {
                appel("consultation.findPathologiesParPatient");
                Map<Integer, List<pathologie>> r = new HashMap<>();
                for (int id : idsConsultations(idPatient)) {
                    r.put(id, findPathologies(id));
                }
                return r;
            }
        };
    }

    @Override
    public SymptomeDao getSymptomeDao() {
        return new SymptomeDao() {
            @Override
            public List<symptome> findByDesignationCommencantPar(String prefixe) {
                appel("symptome.find");
                List<symptome> r = new ArrayList<>();
                for (symptome s : symptomes) {
                    if (s.getDesignation().toUpperCase().startsWith(prefixe.toUpperCase())) {
                        r.add(s);
                    }
                }
                return r;
            }

            @Override
            public symptome findByDesignation(String designation) {
                for (symptome s : symptomes) {
                    if (s.getDesignation().equalsIgnoreCase(designation)) {
                        return s;
                    }
                }
                return null;
            }

            @Override
            public symptome insert(String designation) {
                appel("symptome.insert");
                symptome s = new symptome(nouvelId(), designation);
                symptomes.add(s);
                return s;
            }
        };
    }

    @Override
    public PathologieDao getPathologieDao() {
        return new PathologieDao() {
            @Override
            public List<pathologie> findByDesignationCommencantPar(String prefixe) {
                List<pathologie> r = new ArrayList<>();
                for (pathologie p : pathologies) {
                    if (p.getDesignation().toUpperCase().startsWith(prefixe.toUpperCase())) {
                        r.add(p);
                    }
                }
                return r;
            }

            @Override
            public pathologie findByDesignation(String designation) {
                for (pathologie p : pathologies) {
                    if (p.getDesignation().equalsIgnoreCase(designation)) {
                        return p;
                    }
                }
                return null;
            }

            @Override
            public pathologie insert(String designation) {
                appel("pathologie.insert");
                pathologie p = new pathologie(nouvelId(), designation);
                pathologies.add(p);
                return p;
            }
        };
    }

    @Override
    public MedicamentDao getMedicamentDao() {
        return new MedicamentDao() {
            @Override
            public List<medicament> findByNomCommencantPar(String prefixe) {
                List<medicament> r = new ArrayList<>();
                for (medicament m : medicaments) {
                    if (m.getDesignmed().toUpperCase().startsWith(prefixe.toUpperCase())) {
                        r.add(m);
                    }
                }
                return r;
            }

            @Override
            public medicament findByNom(String nom) {
                for (medicament m : medicaments) {
                    if (m.getDesignmed().equalsIgnoreCase(nom)) {
                        return m;
                    }
                }
                return null;
            }

            @Override
            public List<String> findFormes() {
                List<String> r = new ArrayList<>();
                for (String f : formes.values()) {
                    if (f != null && !r.contains(f)) {
                        r.add(f);
                    }
                }
                return r;
            }

            @Override
            public medicament insert(String nom, String forme) {
                appel("medicament.insert");
                medicament m = new medicament();
                m.setIdmed(nouvelId());
                m.setDesignmed(nom);
                medicaments.add(m);
                formes.put(m.getIdmed(), forme);
                return m;
            }
        };
    }

    @Override
    public PrescriptionDao getPrescriptionDao() {
        return new PrescriptionDao() {
            @Override
            public void insertAll(int idConsultation, List<medicament> liste) {
                appel("prescription.insertAll");
                List<medicament> copie = new ArrayList<>();
                for (medicament m : liste) {
                    copie.add(new medicament(m));
                }
                prescriptions.put(idConsultation, copie);
            }

            @Override
            public List<medicament> findByConsultation(int idConsultation) {
                List<medicament> l = prescriptions.get(idConsultation);
                return l == null ? new ArrayList<medicament>() : new ArrayList<>(l);
            }

            @Override
            public Map<Integer, List<medicament>> findByPatient(int idPatient) {
                appel("prescription.findByPatient");
                Map<Integer, List<medicament>> r = new HashMap<>();
                for (int id : idsConsultations(idPatient)) {
                    r.put(id, findByConsultation(id));
                }
                return r;
            }
        };
    }

    @Override
    public AnalyseDao getAnalyseDao() {
        return new AnalyseDao() {
            @Override
            public List<analyse> findByDesignationCommencantPar(String prefixe) {
                List<analyse> r = new ArrayList<>();
                for (analyse a : catalogueAnalyses) {
                    if (a.getDesign().toUpperCase().startsWith(prefixe.toUpperCase())) {
                        r.add(a);
                    }
                }
                return r;
            }

            @Override
            public List<analyse> findByCategorie(int idCategorie) {
                List<analyse> r = new ArrayList<>();
                for (analyse a : catalogueAnalyses) {
                    if (categorieDeLAnalyse.get(a.getIdAnal()) == idCategorie) {
                        r.add(a);
                    }
                }
                return r;
            }

            @Override
            public List<CategAnalyse> findCategories() {
                return new ArrayList<>(categories);
            }

            @Override
            public analyse insert(String designation, int idCategorie) {
                appel("analyse.insert");
                analyse a = new analyse();
                a.setIdAnal(nouvelId());
                a.setDesign(designation);
                catalogueAnalyses.add(a);
                categorieDeLAnalyse.put(a.getIdAnal(), idCategorie);
                return a;
            }
        };
    }

    @Override
    public DemandeAnalyseDao getDemandeAnalyseDao() {
        return new DemandeAnalyseDao() {
            @Override
            public void insertAll(int idConsultation, List<analyse> liste) {
                appel("demande.insertAll");
                demandes.put(idConsultation, copieAnalyses(liste));
            }

            @Override
            public void updateResultats(int idConsultation, List<analyse> liste) {
                appel("demande.updateResultats");
                demandes.put(idConsultation, copieAnalyses(liste));
            }

            @Override
            public List<analyse> findByConsultation(int idConsultation) {
                List<analyse> l = demandes.get(idConsultation);
                return l == null ? new ArrayList<analyse>() : copieAnalyses(l);
            }

            @Override
            public Map<Integer, List<analyse>> findByPatient(int idPatient) {
                appel("demande.findByPatient");
                Map<Integer, List<analyse>> r = new HashMap<>();
                for (int id : idsConsultations(idPatient)) {
                    r.put(id, findByConsultation(id));
                }
                return r;
            }
        };
    }

    private static List<analyse> copieAnalyses(List<analyse> liste) {
        List<analyse> r = new ArrayList<>();
        for (analyse a : liste) {
            analyse c = new analyse();
            c.setIdAnal(a.getIdAnal());
            c.setDesign(a.getDesign());
            c.setObserv(a.getObserv());
            c.setResultat(a.getResultat());
            c.setNote(a.getNote());
            r.add(c);
        }
        return r;
    }

    @Override
    public RendezVousDao getRendezVousDao() {
        return new RendezVousDao() {
            @Override
            public void insert(Rendez_Vous rdv) {
                appel("rdv.insert");
                verifierCreneauLibre(rdv.getIdMedecin(), rdv.getDate(), rdv.getHeure(), null);
                rdv.setIdRendezVous(nouvelId());
                rendezVous.add(rdv);
            }

            @Override
            public boolean changerEtat(int idRendezVous, int etatAttendu, int nouvelEtat) {
                appel("rdv.changerEtat");
                Rendez_Vous r = rendezVousNumero(idRendezVous);
                if (r == null || r.getEtat() != etatAttendu) {
                    return false;
                }
                r.setEtat(nouvelEtat);
                return true;
            }

            @Override
            public boolean deplacer(int idRendezVous, LocalDate jour, LocalTime heure, int idMedecin) {
                appel("rdv.deplacer");
                Rendez_Vous r = rendezVousNumero(idRendezVous);
                if (r == null || r.getEtat() != Rendez_Vous.ETAT_PREVU) {
                    return false;
                }
                verifierCreneauLibre(idMedecin, jour, heure, idRendezVous);
                r.setDate(jour);
                r.setHeure(heure);
                r.setIdMedecin(idMedecin);
                return true;
            }

            @Override
            public List<Rendez_Vous> findEntre(LocalDate debut, LocalDate fin) {
                appel("rdv.findEntre");
                List<Rendez_Vous> r = new ArrayList<>();
                for (Rendez_Vous x : rendezVous) {
                    LocalDate d = x.getDate();
                    if (!d.isBefore(debut) && !d.isAfter(fin)) {
                        r.add(x);
                    }
                }
                r.sort(Comparator.comparing(Rendez_Vous::getDebut));
                return r;
            }

            @Override
            public List<Rendez_Vous> findDesPatients(Collection<Integer> idPatients) {
                appel("rdv.findDesPatients");
                List<Rendez_Vous> r = new ArrayList<>();
                for (Rendez_Vous x : rendezVous) {
                    if (idPatients.contains(x.getPatient().getIdentPat())) {
                        r.add(x);
                    }
                }
                r.sort(Comparator.comparing(Rendez_Vous::getDebut).reversed());
                return r;
            }
        };
    }

    private Rendez_Vous rendezVousNumero(int id) {
        for (Rendez_Vous r : rendezVous) {
            if (r.getIdRendezVous() != null && r.getIdRendezVous() == id) {
                return r;
            }
        }
        return null;
    }

    /** Comme la clé unique (idMedecin, date, heure, occupe) de V12. */
    private void verifierCreneauLibre(Integer idMedecin, LocalDate jour, LocalTime heure, Integer sauf) {
        for (Rendez_Vous r : rendezVous) {
            if (r.isOccupant() && r.getDate().equals(jour) && r.getHeure().equals(heure)
                    && java.util.Objects.equals(r.getIdMedecin(), idMedecin)
                    && (sauf == null || !sauf.equals(r.getIdRendezVous()))) {
                throw new DoublonException("Duplicate entry (uk_rendezvous_creneau)", null);
            }
        }
    }

    @Override
    public SalleAttenteDao getSalleAttenteDao() {
        return new SalleAttenteDao() {
            @Override
            public void insert(Passage p) {
                appel("salle.insert");
                for (Passage x : passages) {
                    boolean memePresent = x.isPresent() && x.getPatient().getIdentPat() == p.getPatient().getIdentPat()
                            && x.getJour().equals(p.getJour());
                    boolean memeRdv = p.getIdRendezVous() != null && p.getIdRendezVous().equals(x.getIdRendezVous());
                    if (memePresent || memeRdv) {
                        throw new DoublonException("Duplicate entry (uk_salle_attente)", null);
                    }
                }
                p.setIdPassage(nouvelId());
                passages.add(copie(p)); // la base garde sa propre ligne
            }

            @Override
            public List<Passage> findDuJour(LocalDate jour) {
                appel("salle.findDuJour");
                List<Passage> l = new ArrayList<>();
                for (Passage x : passages) {
                    if (x.getJour().equals(jour)) {
                        l.add(copie(x));
                    }
                }
                return l;
            }

            @Override
            public boolean mettreAJour(Passage p, int etatAttendu) {
                appel("salle.mettreAJour");
                for (Passage x : passages) {
                    if (x.getIdPassage().equals(p.getIdPassage())) {
                        if (x.getEtat() != etatAttendu) {
                            return false;
                        }
                        x.setEtat(p.getEtat());
                        x.setAppeleLe(p.getAppeleLe());
                        x.setSortieLe(p.getSortieLe());
                        x.setUrgence(p.isUrgence());
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /** Comme une relecture en base : une copie, avec l'heure du rendez-vous. */
    private Passage copie(Passage x) {
        Passage c = new Passage();
        c.setIdPassage(x.getIdPassage());
        c.setPatient(x.getPatient());
        c.setIdMedecin(x.getIdMedecin());
        c.setIdRendezVous(x.getIdRendezVous());
        Rendez_Vous r = x.getIdRendezVous() == null ? null : rendezVousNumero(x.getIdRendezVous());
        c.setHeurePrevue(r == null ? null : r.getHeure());
        c.setJour(x.getJour());
        c.setArriveeLe(x.getArriveeLe());
        c.setAppeleLe(x.getAppeleLe());
        c.setSortieLe(x.getSortieLe());
        c.setUrgence(x.isUrgence());
        c.setEtat(x.getEtat());
        return c;
    }

    @Override
    public EvenementDao getEvenementDao() {
        return new EvenementDao() {
            @Override
            public void publier(String type, Integer idObjet) {
                appel("evenement.publier");
                evenements.add(type + " " + idObjet);
            }

            @Override
            public long dernier() {
                appel("evenement.dernier");
                return evenements.size();
            }

            @Override
            public int purgerAvant(LocalDateTime limite) {
                appel("evenement.purger");
                return 0;
            }
        };
    }

    @Override
    public MessageDao getMessageDao() {
        return new MessageDao() {
            @Override
            public void insert(MessageInterne m) {
                appel("message.insert");
                m.setIdMessage(nouvelId());
                messages.add(m);
            }

            private boolean dansLeFil(MessageInterne m, int moi, Integer autre) {
                if (autre == null) {
                    return m.getIdDestinataire() == null;
                }
                Integer de = m.getIdExpediteur();
                Integer a = m.getIdDestinataire();
                return (Integer.valueOf(moi).equals(de) && autre.equals(a))
                        || (autre.equals(de) && Integer.valueOf(moi).equals(a));
            }

            private boolean recuNonLu(MessageInterne m, int moi) {
                boolean recu = m.getIdDestinataire() == null || m.getIdDestinataire() == moi;
                boolean pasDeMoi = m.getIdExpediteur() == null || m.getIdExpediteur() != moi;
                return recu && pasDeMoi && !lus.contains(m.getIdMessage() + ":" + moi);
            }

            @Override
            public List<MessageInterne> conversation(int moi, Integer autre, int limite) {
                appel("message.conversation");
                List<MessageInterne> l = new ArrayList<>();
                for (MessageInterne m : messages) {
                    if (dansLeFil(m, moi, autre)) {
                        MessageInterne c = new MessageInterne();
                        c.setIdMessage(m.getIdMessage());
                        c.setIdExpediteur(m.getIdExpediteur());
                        c.setIdDestinataire(m.getIdDestinataire());
                        c.setTexte(m.getTexte());
                        c.setPatient(m.getPatient());
                        c.setEnvoyeLe(m.getEnvoyeLe());
                        User u = m.getIdExpediteur() == null ? null : utilisateurs.get(m.getIdExpediteur());
                        c.setNomExpediteur(u == null ? null : u.getNom() + " " + u.getPrenom());
                        c.setLuParDestinataire(m.getIdDestinataire() != null
                                && lus.contains(m.getIdMessage() + ":" + m.getIdDestinataire()));
                        l.add(c);
                    }
                }
                return l.size() > limite ? new ArrayList<>(l.subList(l.size() - limite, l.size())) : l;
            }

            @Override
            public Map<Integer, Integer> nonLus(int moi) {
                appel("message.nonLus");
                Map<Integer, Integer> n = new HashMap<>();
                for (MessageInterne m : messages) {
                    if (recuNonLu(m, moi)) {
                        int fil = m.getIdDestinataire() == null ? TOUT_LE_CABINET : m.getIdExpediteur();
                        n.merge(fil, 1, Integer::sum);
                    }
                }
                return n;
            }

            @Override
            public int marquerLus(int moi, Integer autre, LocalDateTime quand) {
                appel("message.marquerLus");
                int n = 0;
                for (MessageInterne m : messages) {
                    if (dansLeFil(m, moi, autre) && recuNonLu(m, moi)) {
                        lus.add(m.getIdMessage() + ":" + moi);
                        n++;
                    }
                }
                return n;
            }

            @Override
            public long dernier() {
                appel("message.dernier");
                return messages.isEmpty() ? 0 : messages.get(messages.size() - 1).getIdMessage();
            }

            @Override
            public int purgerAvant(LocalDateTime limite) {
                appel("message.purger");
                int avant = messages.size();
                messages.removeIf(m -> m.getEnvoyeLe().isBefore(limite));
                return avant - messages.size();
            }
        };
    }

    @Override
    public PresenceDao getPresenceDao() {
        return new PresenceDao() {
            @Override
            public void signaler(int idUtilisateur, String poste) {
                appel("presence.signaler");
                presents.put(idUtilisateur, poste);
            }

            @Override
            public void quitter(int idUtilisateur) {
                appel("presence.quitter");
                presents.remove(idUtilisateur);
            }

            @Override
            public Set<Integer> enLigne(int secondes) {
                appel("presence.enLigne");
                return new HashSet<>(presents.keySet());
            }
        };
    }
}
