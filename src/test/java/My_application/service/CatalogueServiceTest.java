package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import My_application.entities.CategAnalyse;
import My_application.entities.medicament;
import My_application.entities.symptome;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import org.junit.Before;
import org.junit.Test;

public class CatalogueServiceTest {

    private MemoireDAOFactory base;
    private CatalogueService service;

    @Before
    public void preparer() {
        base = Donnees.base();
        service = new CatalogueService(base);
    }

    @Test
    public void unSymptomeExistantNEstPasRecree() {
        symptome s = service.obtenirOuCreerSymptome("Maux d'estomac");
        assertSame(s, service.obtenirOuCreerSymptome("  maux   d'estomac "));
        assertEquals(1, base.symptomes.size());
    }

    @Test(expected = MetierException.class)
    public void saisieVideRefusee() {
        service.obtenirOuCreerPathologie("   ");
    }

    @Test
    public void pasDeRequetePourUnPrefixeVide() {
        base.requetes = 0;
        assertTrue(service.symptomes("").isEmpty());
        assertTrue(service.medicaments(null).isEmpty());
        assertEquals(0, base.requetes);
    }

    @Test
    public void designationDuMedicamentComposee() {
        medicament m = service.creerMedicament("doliprane", "comprimé", "1g");
        assertEquals("DOLIPRANE comprimé 1g", m.getDesignmed());
        assertEquals("comprimé", base.formes.get(m.getIdmed()));
    }

    @Test(expected = MetierException.class)
    public void medicamentEnDouble() {
        service.creerMedicament("DOLIPRANE", "", "");
        service.creerMedicament("doliprane", null, null);
    }

    @Test
    public void analyseCreeeEnMajusculesDansSaCategorie() {
        CategAnalyse bilan = new CategAnalyse(3, "Bilan lipidique");
        base.categories.add(bilan);
        service.creerAnalyse("cholestérol total", bilan);
        assertEquals("CHOLESTÉROL TOTAL", service.analysesDeCategorie(bilan).get(0).getDesign());
    }

    @Test(expected = MetierException.class)
    public void analyseSansCategorie() {
        service.creerAnalyse("TSH", null);
    }
}
