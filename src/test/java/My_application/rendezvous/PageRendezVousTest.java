package My_application.rendezvous;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import My_application.entities.patient;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import My_application.service.RendezVousService;
import org.junit.Test;

/** Page « Rendez-vous » du panneau central, sans l'afficher dans une fenêtre. */
public class PageRendezVousTest {

    private int choixPatient;

    private PageRendezVous page() {
        MemoireDAOFactory base = Donnees.base();
        return new PageRendezVous(Donnees.utilisateur(1), new RendezVousService(base), null, () -> choixPatient++);
    }

    @Test
    public void sOuvreSansPatientPuisPourUnPatient() {
        PageRendezVous page = page();
        assertNull(page.getPatient());

        patient p = Donnees.patient(1, "AMRANI");
        page.afficher(p);
        assertSame(p, page.getPatient());

        // Menu « Rendez-vous » : l'agenda revient sans patient.
        page.afficher(null);
        assertNull(page.getPatient());
        page.rafraichir();
        assertEquals(0, choixPatient);
    }
}
