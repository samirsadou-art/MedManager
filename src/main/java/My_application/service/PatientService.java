package My_application.service;

import java.util.Locale;
import My_application.dao.DAOFactory;
import My_application.dao.PatientDao;
import My_application.dao.UniteDeTravail;
import My_application.entities.patient;
import java.time.LocalDate;
import java.util.ArrayList;
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

    /**
     * Patients dont le nom commence par le texte saisi, ou, si le texte est un
     * début de numéro (au moins 3 chiffres), dont le téléphone commence ainsi.
     */
    public List<patient> rechercher(String texte) {
        String t = texte == null ? "" : texte.trim();
        if (t.matches("[+0-9 .\\-]+") && chiffres(t).length() >= 3) {
            return patientDao.findByTelephoneCommencantPar(versNational(chiffres(t), t.startsWith("+")));
        }
        return patientDao.findByNomCommencantPar(t);
    }

    public patient trouver(int id) {
        return patientDao.findById(id);
    }

    /**
     * Crée le patient après validation ; renvoie le patient avec son identifiant.
     * Le téléphone est obligatoire à l'inscription (rappels de rendez-vous).
     * Appeler d'abord {@link #doublonsPossibles(patient)} pour proposer une
     * fiche existante.
     */
    public patient creer(final patient p) {
        valider(p);
        normaliser(p);
        if (p.getTelephone() == null) {
            throw new MetierException("Le numéro de téléphone du patient est obligatoire.");
        }
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

    /**
     * Fiches qui ressemblent à ce patient : même nom et même date de
     * naissance, ou même téléphone. La fiche elle-même (même numéro) est
     * exclue, pour pouvoir s'en servir aussi en modification.
     */
    public List<patient> doublonsPossibles(patient p) {
        valider(p);
        patient n = copieNormalisee(p);
        List<patient> r = new ArrayList<>();
        for (patient trouve : patientDao.findDoublons(n.getNom(), n.getDat_nais(), n.getTelephone())) {
            if (trouve.getIdentPat() != p.getIdentPat()) {
                r.add(trouve);
            }
        }
        return r;
    }

    /** Enregistre les modifications de l'état civil et des coordonnées (pas du dossier médical). */
    public patient modifier(final patient p) {
        valider(p);
        if (p.getIdentPat() <= 0 || patientDao.findById(p.getIdentPat()) == null) {
            throw new MetierException("Ce patient n'existe pas (ou plus) dans la base.");
        }
        normaliser(p);
        return daos.enTransaction(new UniteDeTravail<patient>() {
            @Override
            public patient executer() {
                patientDao.update(p);
                journal.noter(JournalAcces.Action.MODIFICATION_PATIENT, p.getIdentPat(), null);
                return p;
            }
        });
    }

    /** Nom en majuscules, prénom capitalisé, téléphone au format national, champs vides à null. */
    static void normaliser(patient p) {
        p.setNom(p.getNom().trim().toUpperCase(Locale.FRENCH));
        p.setPrenom(capitaliser(p.getPrenom().trim()));
        p.setTelephone(normaliserTelephone(p.getTelephone()));
        p.setEmail(normaliserEmail(p.getEmail()));
        p.setNum_soc(vide(p.getNum_soc()));
        p.setLocalite(vide(p.getLocalite()));
        p.setVille(vide(p.getVille()));
        p.setWilaya(vide(p.getWilaya()));
        p.setPays(vide(p.getPays()));
    }

    private static patient copieNormalisee(patient p) {
        patient c = new patient();
        c.setNom(p.getNom());
        c.setPrenom(p.getPrenom());
        c.setDat_nais(p.getDat_nais());
        c.setTelephone(p.getTelephone());
        normaliser(c);
        return c;
    }

    /**
     * Numéro algérien au format national, chiffres seuls : 10 chiffres pour un
     * mobile (05, 06, 07), 9 pour un fixe (0XX). Accepte espaces, points,
     * tirets et l'indicatif +213 ou 00213. Null si rien n'est saisi.
     *
     * @throws MetierException si le numéro n'a pas cette forme
     */
    public static String normaliserTelephone(String saisie) {
        String t = vide(saisie);
        if (t == null) {
            return null;
        }
        if (!t.matches("[+0-9 .\\-()]+")) {
            throw new MetierException("Le numéro de téléphone ne doit contenir que des chiffres.");
        }
        String n = versNational(chiffres(t), t.startsWith("+"));
        if (!n.matches("0[5-7][0-9]{8}|0[2-4][0-9]{7}")) {
            throw new MetierException("Numéro de téléphone invalide : 10 chiffres pour un mobile (05, 06, 07), "
                    + "9 pour un fixe, par exemple 0555 12 34 56.");
        }
        return n;
    }

    private static String versNational(String chiffres, boolean international) {
        if (chiffres.startsWith("00213")) {
            return "0" + chiffres.substring(5);
        }
        if (international && chiffres.startsWith("213")) {
            return "0" + chiffres.substring(3);
        }
        return chiffres;
    }

    private static String chiffres(String s) {
        return s.replaceAll("[^0-9]", "");
    }

    static String normaliserEmail(String saisie) {
        String e = vide(saisie);
        if (e == null) {
            return null;
        }
        e = e.toLowerCase(Locale.ROOT);
        if (!e.matches("[^@\\s]+@[^@\\s]+\\.[a-z]{2,}")) {
            throw new MetierException("Adresse e-mail invalide.");
        }
        return e;
    }

    private static String vide(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
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
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.FRENCH);
    }
}
