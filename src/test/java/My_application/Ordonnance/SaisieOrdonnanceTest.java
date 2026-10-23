package My_application.Ordonnance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import My_application.entities.consultation;
import My_application.entities.medicament;
import My_application.entities.ordonnance;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import My_application.service.CatalogueService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Saisie de l'ordonnance dans l'onglet du dossier, sans l'afficher. */
public class SaisieOrdonnanceTest {

    private MemoireDAOFactory base;
    private ordonnance ordo;
    private List<String> allergies;
    private int changements;
    private final List<String> questions = new ArrayList<>();
    private boolean reponse = true;

    @Before
    public void preparer() {
        base = Donnees.base();
        base.medicaments.add(Donnees.medicament(10, "PARACETAMOL 1 G CP", 0, 0, 0, 0));
        base.medicaments.add(Donnees.medicament(11, "AMOXICILLINE 1 G CP", 0, 0, 0, 0));
        ordo = new ordonnance();
        allergies = new ArrayList<>();
        changements = 0;
    }

    private SaisieOrdonnance saisie(consultation precedente) {
        SaisieOrdonnance s = new SaisieOrdonnance(ordo, new CatalogueService(base), precedente,
                () -> allergies, () -> changements++);
        s.confirmation = q -> {
            questions.add(q);
            return reponse;
        };
        s.creation = nom -> null;
        return s;
    }

    @Test
    public void ajouteAvecLaPosologieSaisieEtRemetLaSaisieAZero() {
        SaisieOrdonnance s = saisie(null);
        s.champPrise().setValue(2);
        s.champFois().setValue(3);
        s.champDuree().setValue(7);
        s.boutonMatin().setSelected(true);
        assertEquals("Soit 42 unités pour 7 jours", s.besoin().getText());

        assertTrue(s.ajouter(base.medicaments.get(0)));
        assertEquals(1, ordo.getNombreMedicament());
        medicament m = ordo.getMedicament(0);
        assertEquals(10, m.getIdmed());
        assertEquals(2, m.getUnite());
        assertEquals(3, m.getPosologie());
        assertEquals(7, m.getDuree());
        assertEquals(medicament.MODE_MATIN, m.getMode());
        assertEquals("2 unités, 3 fois par jour, le matin", s.modele().getValueAt(0, 2));
        assertEquals("7 jours", s.modele().getValueAt(0, 3));
        assertEquals(1, s.champPrise().getValue());
        assertEquals(medicament.MODE_INDIFFERENT, s.mode());
        assertTrue(changements > 0);
    }

    @Test
    public void refuseUnDoublon() {
        SaisieOrdonnance s = saisie(null);
        assertTrue(s.ajouter(base.medicaments.get(0)));
        s.confirmation = q -> true;
        // Le doublon affiche une erreur (sans écran : la boîte de dialogue est remplacée).
        int avant = ordo.getNombreMedicament();
        try {
            s.ajouter(base.medicaments.get(0));
        } catch (java.awt.HeadlessException e) {
            // la boîte de dialogue ne peut pas s'ouvrir sans écran
        }
        assertEquals(avant, ordo.getNombreMedicament());
    }

    @Test
    public void allergieDemandeConfirmation() {
        allergies.add("Amoxicilline");
        SaisieOrdonnance s = saisie(null);
        reponse = false;
        assertFalse(s.ajouter(base.medicaments.get(1)));
        assertEquals(1, questions.size());
        assertTrue(questions.get(0).contains("allergique"));
        assertTrue(ordo.estVide());

        reponse = true;
        assertTrue(s.ajouter(base.medicaments.get(1)));
        assertEquals(1, ordo.getNombreMedicament());
    }

    @Test
    public void alerteQuandOnTapeUnMedicamentConcerne() {
        allergies.add("Amoxicilline");
        SaisieOrdonnance s = saisie(null);
        ((javax.swing.text.JTextComponent) s.champMedicament().getEditor().getEditorComponent())
                .setText("amoxicilline 500");
        assertTrue(s.alerte().isVisible());
        assertTrue(s.alerte().getText().contains("Amoxicilline"));
    }

    @Test
    public void modifierPuisMettreAJour() {
        ordo.ajouter(Donnees.medicament(10, "PARACETAMOL 1 G CP", 1, 3, medicament.MODE_INDIFFERENT, 5));
        SaisieOrdonnance s = saisie(null);
        s.modifier(0);
        assertEquals(SaisieOrdonnance.METTRE_A_JOUR, s.boutonAjouter().getText());
        assertEquals(3, s.champFois().getValue());
        s.champDuree().setValue(10);
        s.boutonMatin().setSelected(true);
        s.valider();
        assertEquals(1, ordo.getNombreMedicament());
        assertEquals(10, ordo.getMedicament(0).getDuree());
        assertEquals(medicament.MODE_MATIN, ordo.getMedicament(0).getMode());
        assertEquals(SaisieOrdonnance.AJOUTER, s.boutonAjouter().getText());
    }

    @Test
    public void retirerEtToutRetirer() {
        ordo.ajouter(Donnees.medicament(10, "PARACETAMOL 1 G CP", 1, 3, 2, 5));
        ordo.ajouter(Donnees.medicament(11, "AMOXICILLINE 1 G CP", 1, 2, 2, 7));
        SaisieOrdonnance s = saisie(null);
        s.retirer(0);
        assertEquals("AMOXICILLINE 1 G CP", ordo.getMedicament(0).getDesignmed());
        s.retirer(5);
        assertEquals(1, s.modele().getRowCount());
        s.toutRetirer();
        assertTrue(ordo.estVide());
        assertEquals(0, s.modele().getRowCount());
    }

    @Test
    public void reprendLOrdonnancePrecedenteSansDoublon() {
        consultation avant = new consultation();
        avant.setDate_cons(LocalDate.of(2026, 10, 15));
        avant.setHeure_cons(LocalTime.of(10, 12));
        avant.addOrdonnance();
        avant.getOrdoPat().ajouter(Donnees.medicament(10, "PARACETAMOL 1 G CP", 1, 3, 2, 5));
        avant.getOrdoPat().ajouter(Donnees.medicament(12, "AMLODIPINE 5 MG CP", 1, 1, 1, 30));
        ordo.ajouter(Donnees.medicament(10, "PARACETAMOL 1 G CP", 2, 2, 2, 3));
        SaisieOrdonnance s = saisie(avant);
        assertTrue(s.boutonReprendre().isVisible());
        assertEquals("Reprendre l'ordonnance du 15/10/2026", s.boutonReprendre().getText());
        assertEquals(1, s.reprendre());
        assertEquals(2, ordo.getNombreMedicament());
        // Une copie : modifier la nouvelle ligne ne change pas l'ancienne ordonnance.
        ordo.getMedicament(1).setDuree(1);
        assertEquals(30, avant.getOrdoPat().getMedicament(1).getDuree());
    }

    @Test
    public void sansOrdonnancePrecedentePasDeBoutonReprendre() {
        SaisieOrdonnance s = saisie(null);
        assertFalse(s.boutonReprendre().isVisible());
        assertEquals(0, s.reprendre());
        allergies = null;
        s.valider(); // saisie vide : rien
        assertEquals(Collections.emptyList(), s.lignes());
    }
}
