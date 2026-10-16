package My_application.dao.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import My_application.dao.UniteDeTravail;
import My_application.fake.FauxJdbc;
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
}
