package My_application.dao.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.dao.DataAccessException;
import My_application.dao.UniteDeTravail;
import My_application.fake.FauxJdbc;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class TransactionManagerTest {

    private final FauxJdbc base = new FauxJdbc();
    private final TransactionManager tx = new TransactionManager(base);

    @Test
    public void commitSiToutSePasseBien() {
        String r = tx.executer(new UniteDeTravail<String>() {
            @Override
            public String executer() {
                assertEquals("autocommit désactivé pendant la transaction", false, base.autoCommit);
                return "ok";
            }
        });
        assertEquals("ok", r);
        assertEquals(1, base.commits);
        assertEquals(0, base.rollbacks);
        assertTrue("autocommit restauré", base.autoCommit);
    }

    @Test
    public void rollbackEtExceptionPropagéeEnCasDErreur() {
        try {
            tx.executer(new UniteDeTravail<Void>() {
                @Override
                public Void executer() {
                    throw new IllegalStateException("boum");
                }
            });
            fail();
        } catch (IllegalStateException e) {
            assertEquals("boum", e.getMessage());
        }
        assertEquals(0, base.commits);
        assertEquals(1, base.rollbacks);
        assertTrue(base.autoCommit);
    }

    @Test
    public void uneTransactionImbriqueeRejointLaTransactionExterne() {
        tx.executer(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                tx.executer(new UniteDeTravail<Void>() {
                    @Override
                    public Void executer() {
                        return null;
                    }
                });
                assertEquals("pas de commit intermédiaire", 0, base.commits);
                return null;
            }
        });
        assertEquals(1, base.commits);
    }

    /** Fournisseur qui ouvre une nouvelle connexion à chaque appel (comme Database après une coupure). */
    private static final class FournisseurQuiRouvre implements ConnectionProvider {
        final FauxJdbc base = new FauxJdbc();
        int ouvertures;

        @Override
        public Connection getConnection() {
            ouvertures++;
            return base.getConnection();
        }
    }

    @Test
    public void pendantLaTransactionLesDaoRecoiventLaConnexionDeLaTransaction() {
        final FournisseurQuiRouvre f = new FournisseurQuiRouvre();
        final TransactionManager t = new TransactionManager(f);
        t.executer(new UniteDeTravail<Void>() {
            @Override
            public Void executer() {
                try {
                    Connection a = t.getConnection();
                    Connection b = t.getConnection();
                    assertSame(a, b);
                } catch (SQLException e) {
                    throw new AssertionError(e);
                }
                return null;
            }
        });
        assertEquals("une seule ouverture : celle du début de la transaction", 1, f.ouvertures);
        try {
            t.getConnection();
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
        assertEquals("hors transaction : le fournisseur", 2, f.ouvertures);
    }

    @Test
    public void connexionPerdueEnPleineTransactionEchoueSansEnRouvrirUne() {
        final FournisseurQuiRouvre f = new FournisseurQuiRouvre();
        final TransactionManager t = new TransactionManager(f);
        final JdbcHelper jdbc = new JdbcHelper(t, t.verrou());
        try {
            t.executer(new UniteDeTravail<Void>() {
                @Override
                public Void executer() {
                    jdbc.update("INSERT INTO consultation (idPatient) VALUES (?)", 1);
                    f.base.connexionFermee = true; // coupure réseau
                    jdbc.update("INSERT INTO prescrire (idConsul) VALUES (?)", 1);
                    return null;
                }
            });
            fail();
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("perdue pendant la transaction"));
        }
        assertEquals("aucune connexion neuve en autocommit", 1, f.ouvertures);
        assertEquals(0, f.base.commits);
        assertEquals(1, f.base.rollbacks);
        assertEquals("seul le premier INSERT a été préparé", 1, f.base.preparees.size());
    }

    @Test(timeout = 10000)
    public void uneRequeteDUnAutreThreadAttendLaFinDeLaTransaction() throws Exception {
        final JdbcHelper jdbc = new JdbcHelper(tx, tx.verrou());
        final List<String> ordre = Collections.synchronizedList(new ArrayList<String>());
        final CountDownLatch transactionOuverte = new CountDownLatch(1);
        final CountDownLatch fin = new CountDownLatch(1);
        Thread transaction = new Thread(new Runnable() {
            @Override
            public void run() {
                tx.executer(new UniteDeTravail<Void>() {
                    @Override
                    public Void executer() {
                        transactionOuverte.countDown();
                        try {
                            fin.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        ordre.add("commit");
                        return null;
                    }
                });
            }
        });
        transaction.start();
        transactionOuverte.await();
        Thread autre = new Thread(new Runnable() {
            @Override
            public void run() {
                jdbc.update("UPDATE patient SET nomPatient=? WHERE idPatient=?", "X", 1);
                ordre.add("requete");
            }
        });
        autre.start();
        Thread.sleep(200);
        assertTrue("la requête attend tant que la transaction est ouverte", ordre.isEmpty());
        fin.countDown();
        transaction.join();
        autre.join();
        assertEquals("[commit, requete]", ordre.toString());
    }
}
