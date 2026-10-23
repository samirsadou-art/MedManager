package My_application.rendezvous;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import My_application.entities.Rendez_Vous;
import My_application.entities.medecin;
import My_application.entities.patient;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import My_application.service.RendezVousService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.Test;

/** Page « Rendez-vous » du panneau central, sans l'afficher dans une fenêtre. */
public class PageRendezVousTest {

    private int choixPatient;

    private PageRendezVous page() {
        MemoireDAOFactory base = Donnees.base();
        return new PageRendezVous(Donnees.utilisateur(1), new RendezVousService(base), null, () -> choixPatient++, null);
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

    @Test
    public void unMedecinConnecteNeVoitQueSesRendezVousSansListe() {
        MemoireDAOFactory base = Donnees.base();
        medecin m2 = Donnees.medecin(2);
        m2.setNom("AMRANI");
        base.medecins.put(2, m2);
        LocalDate jour = RendezVousService.dimancheDeLaSemaine(LocalDate.now()).plusDays(2);
        base.rendezVous.add(rdv(1, 1, jour, 9));
        base.rendezVous.add(rdv(2, 2, jour, 10));
        RendezVousService service = new RendezVousService(base);

        PageRendezVous secretaire = new PageRendezVous(Donnees.utilisateur(1), service, null, null, null);
        assertEquals(2, secretaire.agenda().nombreAffiches());

        PageRendezVous medecin = new PageRendezVous(Donnees.utilisateur(1), service, null, null, 2);
        assertFalse(medecin.listeDesMedecins());
        assertEquals(1, medecin.agenda().nombreAffiches());
    }

    private static Rendez_Vous rdv(int id, int idMedecin, LocalDate jour, int heure) {
        Rendez_Vous r = new Rendez_Vous();
        r.setIdRendezVous(id);
        r.setPatient(Donnees.patient(id, "PATIENT" + id));
        r.setDate(jour);
        r.setHeure(LocalTime.of(heure, 0));
        r.setIdMedecin(idMedecin);
        return r;
    }
}
