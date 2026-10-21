package My_application.dao.jdbc;

import My_application.dao.DataAccessException;
import My_application.dao.UniteDeTravail;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Exécute un traitement dans une transaction JDBC : tout est validé
 * ({@code commit}) ou tout est annulé ({@code rollback}).
 * <p>
 * Exemple : enregistrer une consultation écrit dans six tables (consultation,
 * consultsymptome, consultpathologie, prescrire, demandanalyse). Sans
 * transaction, une erreur au milieu laissait une consultation à moitié
 * enregistrée.
 * <p>
 * Les DAO demandent leur connexion à ce gestionnaire (il est lui-même un
 * {@link ConnectionProvider}) :
 * <ul>
 * <li>pendant une transaction, ils reçoivent <b>la</b> connexion de la
 * transaction, jamais une nouvelle. Si elle a été perdue (coupure réseau,
 * serveur redémarré), l'opération échoue au lieu de se poursuivre sur une
 * connexion neuve en autocommit, qui aurait validé la fin du traitement sans
 * son début ;</li>
 * <li>hors transaction, ils reçoivent la connexion du fournisseur.</li>
 * </ul>
 * La connexion étant partagée par toute l'application, l'accès à la base est
 * sérialisé par {@link #verrou()} : une transaction le garde jusqu'au commit,
 * pour qu'une requête lancée par un autre thread (tâche en arrière-plan) ne
 * s'y mêle pas.
 * <p>
 * Les appels imbriqués sont gérés : seule la transaction la plus externe
 * valide ou annule.
 */
public class TransactionManager implements ConnectionProvider {

    private final ConnectionProvider provider;
    private final ReentrantLock verrou = new ReentrantLock();
    private final ThreadLocal<Connection> connexionEnCours = new ThreadLocal<>();

    public TransactionManager(ConnectionProvider provider) {
        this.provider = provider;
    }

    /** Verrou d'accès à la base, partagé avec {@link JdbcHelper}. */
    public ReentrantLock verrou() {
        return verrou;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection c = connexionEnCours.get();
        if (c == null) {
            return provider.getConnection();
        }
        if (c.isClosed()) {
            throw new SQLException("Connexion à la base perdue pendant la transaction : rien n'a été enregistré.");
        }
        return c;
    }

    public <T> T executer(UniteDeTravail<T> travail) {
        if (connexionEnCours.get() != null) {
            // Transaction déjà ouverte par un appelant : on s'y joint.
            return travail.executer();
        }
        verrou.lock();
        try {
            Connection c;
            try {
                c = provider.getConnection();
            } catch (SQLException e) {
                throw new DataAccessException("Connexion à la base impossible", e);
            }
            return executer(c, travail);
        } finally {
            verrou.unlock();
        }
    }

    private <T> T executer(Connection c, UniteDeTravail<T> travail) {
        boolean autoCommitInitial = true;
        try {
            autoCommitInitial = c.getAutoCommit();
            c.setAutoCommit(false);
            connexionEnCours.set(c);
            T resultat = travail.executer();
            c.commit();
            return resultat;
        } catch (SQLException e) {
            annuler(c);
            throw new DataAccessException("Erreur de transaction", e);
        } catch (RuntimeException | Error e) {
            annuler(c);
            throw e;
        } finally {
            connexionEnCours.remove();
            try {
                c.setAutoCommit(autoCommitInitial);
            } catch (SQLException ignored) {
                // la connexion sera de toute façon revalidée au prochain usage
            }
        }
    }

    private static void annuler(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignored) {
            // l'exception d'origine est plus utile que celle du rollback
        }
    }
}
