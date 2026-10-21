package My_application.service;

import My_application.dao.DAOFactory;
import My_application.dao.PatientDao;
import My_application.dao.UniteDeTravail;
import My_application.entities.patient;
import java.time.LocalDate;
import java.util.List;

/** Règles métier liées aux patients. */
public class PatientService {

    private final DAOFactory daos;
    private final PatientDao patientDao;
    private final JournalAcces journal;

    public PatientService(DAOFactory daos) {
        this(daos, JournalAcces.AUCUN);
    }

    public PatientService(DAOFactory daos, JournalAcces journal) {
        this.daos = daos;
        this.patientDao = daos.getPatientDao();
        this.journal = journal;
    }

    public List<patient> rechercher(String debutDuNom) {
        return patientDao.findByNomCommencantPar(debutDuNom == null ? "" : debutDuNom.trim());
    }

    public patient trouver(int id) {
        return patientDao.findById(id);
    }

    /** Crée le patient après validation ; renvoie le patient avec son identifiant. */
    public patient creer(final patient p) {
        valider(p);
        p.setNom(p.getNom().trim().toUpperCase());
        p.setPrenom(capitaliser(p.getPrenom().trim()));
        if (p.getIdentPat() > 0 && patientDao.findById(p.getIdentPat()) != null) {
            throw new MetierException("Le numéro de dossier " + p.getIdentPat() + " est déjà utilisé.");
        }
        return daos.enTransaction(new UniteDeTravail<patient>() {
            @Override
            public patient executer() {
                patient cree = patientDao.insert(p);
                journal.noter(JournalAcces.Action.CREATION_PATIENT, cree.getIdentPat(), null);
                return cree;
            }
        });
    }

    static void valider(patient p) {
        if (p.getNom() == null || p.getNom().trim().isEmpty()) {
            throw new MetierException("Le nom du patient est obligatoire.");
        }
        if (p.getPrenom() == null || p.getPrenom().trim().isEmpty()) {
            throw new MetierException("Le prénom du patient est obligatoire.");
        }
        if (p.getDat_nais() == null) {
            throw new MetierException("La date de naissance est obligatoire.");
        }
        LocalDate naissance = p.getDat_nais();
        if (naissance.isAfter(LocalDate.now())) {
            throw new MetierException("La date de naissance ne peut pas être dans le futur.");
        }
        if (naissance.isBefore(LocalDate.now().minusYears(130))) {
            throw new MetierException("La date de naissance est invraisemblable.");
        }
        if (p.getSexe() == null) {
            throw new MetierException("Le sexe du patient est obligatoire.");
        }
    }

    static String capitaliser(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
