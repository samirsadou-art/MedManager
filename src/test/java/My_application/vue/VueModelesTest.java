package My_application.vue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.DossierPatient;
import My_application.entities.consultation;
import My_application.entities.medicament;
import My_application.fake.Donnees;
import My_application.modelTableau.HistoriqueVisites;
import My_application.modelTableau.ListeTableModel;
import My_application.modelTableau.my_modelAnalyse;
import My_application.modelTableau.my_modelOrdonnance;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import org.junit.Test;

/** Modèles d'affichage Swing (couche Vue), testables sans afficher de fenêtre. */
public class VueModelesTest {

    @Test
    public void iconeSelonSexeEtAge() {
        assertEquals("res/svg/patients/homme.svg", Icones.nomIconePatient(Donnees.patient(1, "A", "a", LocalDate.of(1970, 1, 1), true)));
        assertEquals("res/svg/patients/fille.svg", Icones.nomIconePatient(Donnees.patient(1, "A", "a", LocalDate.now().minusYears(8), false)));
        assertEquals("res/svg/patients/bebe-garcon.svg", Icones.nomIconePatient(Donnees.patient(1, "A", "a", LocalDate.now().minusMonths(3), true)));
        for (String nom : new String[]{"homme", "femme", "garcon", "fille", "bebe-garcon", "bebe-fille"}) {
            assertNotNull("avatar " + nom, Icones.class.getResource("/res/svg/patients/" + nom + ".svg"));
        }
        for (int taille : new int[]{16, 32, 64, 128}) {
            assertNotNull("icône présente pour " + taille, Icones.icone("/res/icons/f" + taille + ".png"));
        }
        assertNull(Icones.icone("/res/icons/inexistant.png"));
    }

    @Test
    public void deuxModelesDAnalyseNeSePartagentPlusLeNombreDeColonnes() {
        my_modelAnalyse resultats = new my_modelAnalyse(4);
        new my_modelAnalyse(); // le formulaire de demande (2 colonnes)
        assertEquals("régression du champ static", 4, resultats.getColumnCount());
    }

    @Test
    public void seuleLaColonneResultatEstEditable() {
        my_modelAnalyse m = new my_modelAnalyse(4);
        m.setListAnalyse(new ArrayList<>(Arrays.asList(Donnees.analyse(1, "TSH", ""))));
        assertTrue(m.isCellEditable(0, 2));
        m.setValueAt("2.1 mUI/l", 0, 2);
        assertEquals("2.1 mUI/l", m.getAnal(0).getResultat());
    }

    @Test
    public void leModeleDOrdonnanceTravailleSurUneCopie() {
        List<medicament> originaux = Arrays.asList(Donnees.medicament(1, "A", 1, 1, 0, 0));
        my_modelOrdonnance m = new my_modelOrdonnance(originaux);
        m.getMedicament(0).setUnite(9);
        m.ajout(Donnees.medicament(2, "B", 1, 1, 0, 0));
        assertEquals(1, originaux.get(0).getUnite()); // « Annuler » ne modifie rien
        assertEquals(2, m.getListMedicament().size());
    }

    @Test
    public void listeTableModel() {
        ListeTableModel<String> m = new ListeTableModel<>("Nom");
        m.setDonnees(Arrays.asList("a", "b"));
        assertEquals(2, m.getRowCount());
        assertEquals("b", m.getValueAt(1, 0));
        m.clear();
        assertEquals(0, m.getSize());
    }

    @Test
    public void historiqueRangeParAnneeCalendaire() {
        // Avec le motif « YYYY » d'origine, le 29/12/2025 était classé en 2026.
        consultation c = new consultation(Donnees.patient(1, "A"), Donnees.medecin(1));
        c.setDate_cons(LocalDate.of(2025, 12, 29));
        c.setHeure_cons(LocalTime.parse("09:30:00"));
        DossierPatient d = new DossierPatient(c.getPatient(), c.getMedecin());
        d.setConsultations(Arrays.asList(c));
        HistoriqueVisites h = new HistoriqueVisites(d);
        h.chargerHistorique();
        DefaultMutableTreeNode annee = (DefaultMutableTreeNode) h.getConsultNoeud().getChildAt(0);
        assertEquals("2025", annee.toString());
        assertNotNull(h.lookForNoeud(h.getVisitesNoeud(), 0));
        assertNull(h.lookForNoeud(h.getVisitesNoeud(), 5));
        assertEquals("seule la branche des consultations", 1, h.getVisitesNoeud().getChildCount());
    }
}
