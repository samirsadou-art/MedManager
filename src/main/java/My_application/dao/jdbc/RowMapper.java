package My_application.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Transforme la ligne courante d'un {@link ResultSet} en objet métier. */
public interface RowMapper<T> {

    T map(ResultSet rs) throws SQLException;
}
