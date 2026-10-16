package My_application.fake;

import My_application.entities.User;
import My_application.entities.analyse;
import My_application.entities.medecin;
import My_application.entities.medicament;
import My_application.entities.patient;
import java.sql.Date;
import java.time.LocalDate;

/** Jeux de données réutilisés par les tests. */
public final class Donnees {

    private Donnees() {
    }

    public static patient patient(int id, String nom, String prenom, LocalDate naissance, boolean masculin) {
        patient p = new patient();
        p.setIdentPat(id);
        p.setNom(nom);
        p.setPrenom(prenom);
        p.setDat_nais(Date.valueOf(naissance));
        p.setSexe(masculin);
        return p;
    }

    public static patient patient(int id, String nom) {
        return patient(id, nom, "Prénom" + id, LocalDate.of(1980, 5, 17), true);
    }

    public static medecin medecin(int id) {
        medecin m = new medecin();
        m.setIdmedecin(id);
        m.setTitre("Dr");
        m.setNom("SAHAB");
        m.setPrenom("Said");
        m.setSpecialite("Médecine générale");
        m.setOrdmedcin("12345");
        m.setAdresse("Fréha, Tizi-Ouzou");
        m.setTelephone1("026 00 00 00");
        m.setTelephone2(null);
        m.setEmail("cabinet@example.org");
        m.setNom_cabinet("Cabinet médical");
        return m;
    }

    public static User utilisateur(int id) {
        return new User(id, "SAHAB", "Said", "Médecin principal");
    }

    public static medicament medicament(int id, String nom, int unite, int posologie, int mode, int duree) {
        medicament m = new medicament();
        m.setIdmed(id);
        m.setDesignmed(nom);
        m.setUnite(unite);
        m.setPosologie(posologie);
        m.setMode(mode);
        m.setDuree(duree);
        return m;
    }

    public static analyse analyse(int id, String designation, String observation) {
        analyse a = new analyse();
        a.setIdAnal(id);
        a.setDesign(designation);
        a.setObserv(observation);
        return a;
    }

    /** Base en mémoire avec un médecin (1), un utilisateur (1) et deux patients. */
    public static MemoireDAOFactory base() {
        MemoireDAOFactory b = new MemoireDAOFactory();
        b.medecins.put(1, medecin(1));
        b.utilisateurs.put(1, utilisateur(1));
        b.patients.put(1, patient(1, "AMRANI"));
        b.patients.put(2, patient(2, "BELKACEM"));
        return b;
    }
}
