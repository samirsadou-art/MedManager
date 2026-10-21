package My_application.fake;

import My_application.dao.AnalyseDao;
import My_application.dao.ConsultationDao;
import My_application.dao.DAOFactory;
import My_application.dao.DataAccessException;
import My_application.dao.DemandeAnalyseDao;
import My_application.dao.JournalDao;
import My_application.dao.MedecinDao;
import My_application.dao.MedicamentDao;
import My_application.dao.PathologieDao;
import My_application.dao.PatientDao;
import My_application.dao.PrescriptionDao;
import My_application.dao.RendezVousDao;
import My_application.dao.SymptomeDao;
import My_application.dao.UniteDeTravail;
import My_application.dao.UtilisateurDao;
import My_application.entities.CategAnalyse;
import My_application.entities.Compte;
import My_application.entities.EntreeJournal;
import My_application.entities.Rendez_Vous;
import My_application.entities.User;
import My_application.entities.analyse;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.entities.medicament;
import My_application.entities.pathologie;
import My_application.entities.patient;
import My_application.entities.symptome;
import java.sql.Date;
import java.util.ArrayList;
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
            public List<consultation> findByDate(Date date) {
                appel("consultation.findByDate");
                List<consultation> r = new ArrayList<>();
                for (consultation c : consultations) {
                    if (c.getDate_cons().toLocalDate().equals(date.toLocalDate())) {
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
                rendezVous.add(rdv);
            }

            @Override
            public boolean delete(Rendez_Vous rdv) {
                appel("rdv.delete");
                for (Rendez_Vous r : new ArrayList<>(rendezVous)) {
                    if (r.memeCreneauEtPatient(rdv)) {
                        rendezVous.remove(r);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public List<Rendez_Vous> findEntre(Date debut, Date fin) {
                appel("rdv.findEntre");
                List<Rendez_Vous> r = new ArrayList<>();
                for (Rendez_Vous x : rendezVous) {
                    java.time.LocalDate d = x.getDate().toLocalDate();
                    if (!d.isBefore(debut.toLocalDate()) && !d.isAfter(fin.toLocalDate())) {
                        r.add(x);
                    }
                }
                return r;
            }
        };
    }
}
