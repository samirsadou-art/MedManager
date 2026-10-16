package My_application.dao.jdbc;

import My_application.dao.DataAccessException;
import My_application.dao.UniteDeTravail;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Exécute un traitement dans une transaction JDBC : tout est validé
 * ({@code commit}) ou tout est annulé ({@code rollback}).
 * <p>
 * Exemple : enregistrer une consultation écrit dans six tables (consultation,
 * consultsymptome, consultpathologie, prescrire, demandanalyse). Sans
 * transaction, une erreur au milieu laissait une consultation à moitié
 * enregistrée. Les DAO partageant la connexion du {@link ConnectionProvider},
 * ils participent automatiquement à la transaction en cours.
 * <p>
 * Les appels imbriqués sont gérés : seule la transaction la plus externe
 * valide ou annule.
 */
public class TransactionManager {

    private final ConnectionProvider provider;
    private final ThreadLocal<Integer> profondeur = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    public TransactionManager(ConnectionProvider provider) {
        this.provider = provider;
    }

    public <T> T executer(UniteDeTravail<T> travail) {
        Connection c;
        try {
            c = provider.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("Connexion à la base impossible", e);
        }
        int niveau = profondeur.get();
        if (niveau > 0) {
            // Transaction déjà ouverte par un appelant : on s'y joint.
            profondeur.set(niveau + 1);
            try {
                return travail.executer();
            } finally {
                profondeur.set(niveau);
            }
        }
        boolean autoCommitInitial = true;
        try {
            autoCommitInitial = c.getAutoCommit();
            c.setAutoCommit(false);
            profondeur.set(1);
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
            profondeur.set(0);
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
