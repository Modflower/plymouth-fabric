package gay.ampflower.plymouth.database.cache;

import java.sql.Connection;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface SqlConnectionProvider {
    Connection getConnection();
}
