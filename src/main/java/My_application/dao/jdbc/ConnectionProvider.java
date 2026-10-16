package My_application.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Fournit la connexion JDBC utilisée par les DAO.
 * <p>
 * Les DAO ne créent jamais eux-mêmes de connexion : ils la demandent à un
 * fournisseur. Cela permet de partager une connexion (et donc une transaction)
 * entre plusieurs DAO, et de remplacer la base réelle par une base de test.
 * La connexion renvoyée appartient au fournisseur : un DAO ne doit pas la fermer.
 */
public interface ConnectionProvider {

    Connection getConnection() throws SQLException;
}
