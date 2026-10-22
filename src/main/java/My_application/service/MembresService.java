package My_application.service;

import My_application.dao.DAOFactory;
import My_application.dao.UniteDeTravail;
import My_application.dao.UtilisateurDao;
import My_application.entities.Role;
import My_application.entities.User;
import java.util.List;

/**
 * Membres du cabinet (table {@code utilisateur}) : réservé aux comptes
 * administrateurs. Les identifiants de connexion se règlent ensuite avec
 * {@link AuthentificationService#definirIdentifiants}.
 * <p>
 * Règles : un médecin est rattaché à sa fiche {@code medecin} ; une
 * secrétaire ou un administrateur ne l'est pas ; le cabinet garde toujours au
 * moins un administrateur actif.
 */
public class MembresService {

    private final DAOFactory daos;
    private final UtilisateurDao utilisateurDao;
    private final JournalAcces journal;
    private final ControleAcces acces;

    public MembresService(DAOFactory daos, JournalAcces journal, ControleAcces acces) {
        this.daos = daos;
        this.utilisateurDao = daos.getUtilisateurDao();
        this.journal = journal;
        this.acces = acces;
    }

    /** Tous les membres, actifs ou non, par nom. */
    public List<User> lister() {
        acces.exiger(Role.ADMIN);
        return utilisateurDao.findAll();
    }

    /** Ajoute un membre, sans identifiants de connexion. */
    public User ajouter(String nom, String prenom, Role role, Integer idMedecin) {
        acces.exiger(Role.ADMIN);
        final User u = new User(0, obligatoire(nom, "nom").toUpperCase(java.util.Locale.FRENCH),
                PatientService.capitaliser(obligatoire(prenom, "prénom")), role.getLibelle());
        u.setRole(role);
        u.setIdMedecin(medecinRattache(role, idMedecin));
        return daos.enTransaction(new UniteDeTravail<User>() {
            @Override
            public User executer() {
                User cree = utilisateurDao.insert(u);
                journal.noter(JournalAcces.Action.CREATION_COMPTE, null,
                        "membre n° " + cree.getIdent() + " (" + cree + ", " + role.getLibelle() + ")");
                return cree;
            }
        });
    }

    /** Change le rôle d'un membre (et son médecin rattaché). */
    public void definirRole(final int idUtilisateur, final Role role, Integer idMedecin) {
        acces.exiger(Role.ADMIN);
        User u = utilisateurDao.findById(idUtilisateur);
        if (u == null) {
            throw new MetierException("L'utilisateur n° " + idUtilisateur + " est introuvable.");
        }
        if (u.getRole() == Role.ADMIN && role != Role.ADMIN) {
            verifierQuIlResteUnAdministrateur(utilisateurDao, idUtilisateur);
        }
        final Integer medecin = medecinRattache(role, idMedecin);
        daos.enTransaction(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                utilisateurDao.definirRole(idUtilisateur, role, medecin);
                journal.noter(JournalAcces.Action.MODIFICATION_COMPTE, null,
                        "rôle de l'utilisateur n° " + idUtilisateur + " : " + role.getLibelle());
                return null;
            }
        });
    }

    /**
     * Refuse de retirer le dernier administrateur actif : sans lui, plus
     * personne ne pourrait gérer les comptes depuis l'application.
     */
    static void verifierQuIlResteUnAdministrateur(UtilisateurDao dao, int idQuiPart) {
        for (User autre : dao.findAll()) {
            if (autre.getIdent() != idQuiPart && autre.getRole() == Role.ADMIN && autre.isActif()) {
                return;
            }
        }
        throw new MetierException("C'est le dernier compte administrateur actif : "
                + "créez ou activez d'abord un autre administrateur.");
    }

    private Integer medecinRattache(Role role, Integer idMedecin) {
        if (role != Role.MEDECIN) {
            return null;
        }
        if (idMedecin == null) {
            throw new MetierException("Un compte médecin doit être rattaché à une fiche médecin.");
        }
        if (daos.getMedecinDao().findById(idMedecin) == null) {
            throw new MetierException("Le médecin n° " + idMedecin + " est introuvable.");
        }
        return idMedecin;
    }

    private static String obligatoire(String valeur, String champ) {
        String v = valeur == null ? "" : valeur.trim();
        if (v.isEmpty()) {
            throw new MetierException("Le " + champ + " du membre est obligatoire.");
        }
        return v;
    }
}
