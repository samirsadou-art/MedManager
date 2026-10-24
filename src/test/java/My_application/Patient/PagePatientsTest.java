package My_application.Patient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import My_application.controller.ListePatientController;
import My_application.entities.DossierPatient;
import My_application.entities.patient;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import My_application.service.DossierService;
import My_application.service.PatientService;
import My_application.service.Session;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Page « Patients » du panneau central, sans l'afficher dans une fenêtre. */
public class PagePatientsTest {

    private MemoireDAOFactory base;
    private final List<DossierPatient> ouverts = new ArrayList<>();

    @Before
    public void preparer() {
        base = Donnees.base();
    }

    private PagePatients page(boolean medecin) {
        PatientService patients = new PatientService(base);
        return new PagePatients(patients,
                v -> new ListePatientController(v, patients, new DossierService(base), new Session(base, 1, 1)),
                medecin, ouverts::add, p -> { });
    }

    @Test
    public void listeTousLesPatientsAuDepartPuisFiltre() {
        PagePatients page = page(true);
        page.rafraichir();
        assertEquals(base.patients.size(), page.modele().getRowCount());

        page.champRecherche().setText("AM");
        page.rechercher();
        assertEquals(1, page.modele().getRowCount());
        assertEquals("AMRANI", page.modele().getElement(0).getNom());
    }

    @Test
    public void resumeSelonLeNombreEtLeRole() {
        assertEquals("Aucun patient inscrit.", PagePatients.resume(0, false, true));
        assertEquals("Aucun patient ne correspond à la recherche.", PagePatients.resume(0, true, false));
        assertEquals("1 patient · double-cliquez pour ouvrir le dossier médical",
                PagePatients.resume(1, false, true));
        assertEquals("3 patients trouvés · double-cliquez pour ouvrir les détails",
                PagePatients.resume(3, true, false));
    }

    @Test
    public void lePanneauSeConstruitPourLaSecretaire() {
        assertNotNull(page(false));
    }

    @Test
    public void colonneTelephoneRetireeEtBoutonDetails() {
        PagePatients page = page(false);
        assertEquals(5, page.modele().getColumnCount());
        for (int i = 0; i < page.modele().getColumnCount(); i++) {
            assertFalse("Téléphone".equals(page.modele().getColumnName(i)));
        }
        assertTrue(page.boutonDetails().isVisible());
        assertFalse(page.boutonDetails().isEnabled());
    }

    @Test
    public void adresseSansPartiesVides() {
        patient p = new patient();
        p.setAdresse(" 12 rue des Oliviers ");
        p.setVille("Tizi Ouzou");
        p.setLocalite("");
        assertEquals("12 rue des Oliviers, Tizi Ouzou", DetailsPatient.adresse(p));
    }

    @Test
    public void salleDAttenteProposeeALaSecretaireSeulement() {
        assertTrue(page(false).boutonSalleAttente().isVisible());
        assertFalse(page(true).boutonSalleAttente().isVisible());
    }
}
