package My_application.dao;

import My_application.entities.Antecedent;
import My_application.entities.NoteDossier;
import java.util.List;

/** Antécédents et notes du dossier médical (migration V18). */
public interface DossierCliniqueDao {

    /** Antécédents du patient, par rubrique puis dans l'ordre de saisie. */
    List<Antecedent> findAntecedents(int idPatient);

    void insert(Antecedent a);

    boolean supprimerAntecedent(int idAntecedent);

    /** Notes du patient, de la plus récente à la plus ancienne. */
    List<NoteDossier> findNotes(int idPatient);

    void insert(NoteDossier n);

    boolean supprimerNote(int idNote);
}
