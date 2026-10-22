package My_application.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import My_application.entities.Role;
import My_application.entities.Statistiques;
import My_application.entities.User;
import My_application.entities.consultation;
import My_application.entities.medecin;
import My_application.fake.Donnees;
import My_application.fake.FausseVue;
import My_application.fake.MemoireDAOFactory;
import My_application.report.RapportService;
import My_application.service.Session;
import My_application.service.StatistiquesService;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class StatistiquesControllerTest {

    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    private static final class FaussesStats extends FausseVue implements StatistiquesVue {
        List<medecin> medecins;
        Statistiques stats;
        List<StatistiquesService.Barre> serie;

        @Override
        public void afficherMedecins(List<medecin> m) {
            medecins = m;
        }

        @Override
        public void afficherStatistiques(Statistiques s, List<StatistiquesService.Barre> b, Map<Integer, medecin> m) {
            stats = s;
            serie = b;
        }
    }

    private MemoireDAOFactory base;
    private Session session;
    private FaussesStats vue;
    private StatistiquesController controleur;
    private final List<File> ouverts = new ArrayList<>();

    @Before
    public void preparer() throws Exception {
        base = Donnees.base();
        session = new Session(base, 1, 1);
        User admin = Donnees.utilisateur(9);
        admin.setRole(Role.ADMIN);
        session.ouvrir(admin);
        consultation c = new consultation(base.patients.get(1), base.medecins.get(1));
        c.setDate_cons(LocalDate.of(2026, 10, 5));
        c.setHeure_cons(LocalTime.of(9, 0));
        base.consultations.add(c);
        RapportService rapports = new RapportService(dossier.newFolder("documents"), new RapportService.Afficheur() {
            @Override
            public void ouvrir(File pdf) {
                ouverts.add(pdf);
            }

            @Override
            public boolean imprimer(File pdf) {
                return false;
            }
        });
        vue = new FaussesStats();
        controleur = new StatistiquesController(vue, new StatistiquesService(base, session), rapports,
                Executeur.IMMEDIAT);
    }

    @Test
    public void calculPuisExports() throws Exception {
        controleur.exporterPdf();
        assertTrue("rien à exporter avant le premier calcul", ouverts.isEmpty());
        assertEquals("Statistiques.csv", controleur.nomCsv());

        controleur.chargerMedecins();
        assertEquals(1, vue.medecins.size());
        controleur.calculer(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null);
        assertEquals(1, vue.stats.getConsultations());
        assertEquals(31, vue.serie.size());
        assertEquals("Statistiques_2026-10-01_2026-10-31.csv", controleur.nomCsv());

        controleur.exporterPdf();
        assertEquals(1, ouverts.size());
        File csv = new File(dossier.getRoot(), "stats.csv");
        controleur.exporterCsv(csv);
        assertTrue(csv.length() > 0);
        assertTrue(vue.informations.get(0).contains(csv.getAbsolutePath()));
        assertTrue(vue.erreurs.isEmpty());
    }

    @Test
    public void periodeRefuseeEtPanneSignalees() {
        controleur.calculer(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null);
        assertNull(vue.stats);
        assertTrue(vue.erreurs.get(0).contains("avant"));
        base.echouerSur("stats.salleAttente");
        controleur.calculer(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 1);
        assertNull(vue.stats);
        assertEquals(2, vue.erreurs.size());
        assertTrue(!vue.erreurs.get(1).contains("Panne simulée"));
    }
}
