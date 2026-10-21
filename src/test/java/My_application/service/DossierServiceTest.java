package My_application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.dao.DataAccessException;
import My_application.entities.DossierPatient;
import My_application.entities.analyse;
import My_application.entities.consultation;
import My_application.entities.pathologie;
import My_application.entities.symptome;
import My_application.fake.Donnees;
import My_application.fake.MemoireDAOFactory;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class DossierServiceTest {

    private MemoireDAOFactory base;
    private DossierService service;

    @Before
    public void preparer() {
        base = Donnees.base();
        base.symptomes.add(new symptome(1, "Toux"));
        base.pathologies.add(new pathologie(2, "GRIPPE"));
        service = new DossierService(base);
    }

    /** Ouvre le dossier du patient 1 et y saisit une consultation complète. */
    private DossierPatient dossierAvecNouvelleConsultation() {
        DossierPatient d = service.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        consultation c = service.nouvelleConsultation(d);
        c.setTemperature(38.5);
        c.addSymptome(base.symptomes.get(0));
        c.addPathologie(base.pathologies.get(0));
        c.getOrdoPat().ajouter(Donnees.medicament(10, "DOLIPRANE 1G", 1, 3, 2, 5));
        c.getAnaPat().setListeAnaly(Arrays.asList(Donnees.analyse(20, "GLYCEMIE", "à jeun")));
        return d;
    }

    @Test
    public void enregistrerEcritToutLAgregatEnUneTransaction() {
        DossierPatient d = dossierAvecNouvelleConsultation();
        assertEquals(1, service.enregistrer(d));

        consultation c = d.getConsultationAt(0);
        assertFalse(c.estNouvelle());
        assertEquals(1, base.commits);
        assertEquals(1, base.consultations.size());
        assertEquals(1, base.consultSymptome.size());
        assertEquals(1, base.consultPathologie.size());
        assertEquals(1, base.prescriptions.get(c.getIdConsul()).size());
        assertEquals(1, base.demandes.get(c.getIdConsul()).size());
    }

    @Test
    public void enCasDErreurRienNEstEnregistreEtLaConsultationResteAEnregistrer() {
        DossierPatient d = dossierAvecNouvelleConsultation();
        base.echouerSur("prescription.insertAll"); // panne au milieu de l'enregistrement
        try {
            service.enregistrer(d);
            fail("l'erreur doit remonter au contrôleur");
        } catch (DataAccessException attendu) {
            // ok
        }
        assertEquals(1, base.rollbacks);
        assertEquals("pas de consultation à moitié enregistrée", 0, base.consultations.size());
        assertEquals(0, base.consultSymptome.size());
        assertTrue("on pourra réessayer", d.getConsultationAt(0).estNouvelle());

        // Deuxième tentative, la base répond : tout est écrit.
        assertEquals(1, service.enregistrer(d));
        assertEquals(1, base.consultations.size());
    }

    @Test
    public void rouvrirLeDossierRelitToutesLesDonnees() {
        service.enregistrer(dossierAvecNouvelleConsultation());

        DossierPatient relu = service.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        assertEquals(1, relu.getNombreConsultation());
        consultation c = relu.getConsultationAt(0);
        assertEquals("Toux", c.getSymptomes().get(0).getDesignation());
        assertEquals("GRIPPE", c.getPathologies().get(0).getDesignation());
        assertEquals("DOLIPRANE 1G", c.getOrdoPat().getMedicament(0).getDesignmed());
        assertEquals("GLYCEMIE", c.getAnaPat().getListeAnaly().get(0).getDesign());
        assertEquals("SAHAB", c.getMedecin().getNom());
    }

    @Test
    public void leNombreDeRequetesNeDependPasDuNombreDeConsultations() {
        for (int i = 0; i < 3; i++) {
            service.enregistrer(dossierAvecNouvelleConsultation());
        }
        base.requetes = 0;
        service.ouvrirDossier(base.patients.get(1), null);
        // 1 requête consultations + 4 requêtes groupées (symptômes, pathologies,
        // ordonnances, analyses) + 1 seule lecture du médecin (cache) : 6, que
        // le dossier compte 3 consultations ou 300.
        assertEquals(6, base.requetes);
        DossierPatient d = service.ouvrirDossier(base.patients.get(1), null);
        assertEquals(3, d.getNombreConsultation());
        assertEquals("Toux", d.getConsultationAt(2).getSymptomes().get(0).getDesignation());
        assertEquals("GLYCEMIE", d.getConsultationAt(2).getAnaPat().getListeAnaly().get(0).getDesign());
    }

    @Test
    public void lesResultatsDAnalysesModifiesSontEnregistres() {
        service.enregistrer(dossierAvecNouvelleConsultation());
        DossierPatient d = service.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        consultation c = d.getConsultationAt(0);
        analyse a = c.getAnaPat().getListeAnaly().get(0);
        a.setResultat("0.95 g/l");
        a.setNote(2);
        c.getAnaPat().setChange(true);
        assertTrue(service.aDesModifications(d));

        assertEquals(1, service.enregistrer(d));
        assertEquals("0.95 g/l", base.demandes.get(c.getIdConsul()).get(0).getResultat());
        assertFalse(c.isChange());
        assertFalse(service.aDesModifications(d));
    }

    @Test
    public void rienAEnregistrer() {
        DossierPatient d = service.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        assertEquals(0, service.enregistrer(d));
        assertEquals(0, base.commits);
    }

    @Test(expected = MetierException.class)
    public void uneSeuleNouvelleConsultationALaFois() {
        DossierPatient d = service.ouvrirDossier(base.patients.get(1), base.medecins.get(1));
        service.nouvelleConsultation(d);
        service.nouvelleConsultation(d);
    }

    @Test(expected = MetierException.class)
    public void uneConsultationSansMedecinEstRefusee() {
        DossierPatient d = service.ouvrirDossier(base.patients.get(1), null);
        service.nouvelleConsultation(d);
        service.enregistrer(d);
    }

    @Test
    public void consultationsDuJour() {
        DossierPatient d = dossierAvecNouvelleConsultation();
        service.enregistrer(d);
        assertNotNull(service.consultationsDuJour(java.time.LocalDate.now()));
        assertEquals(1, service.consultationsDuJour(java.time.LocalDate.now()).size());
    }
}
