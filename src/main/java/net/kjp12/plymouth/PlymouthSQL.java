package net.kjp12.plymouth;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * SQL Driver Adaptor. Used for sharing common functions between the many SQL databases out there.
 *
 * @author kjp12
 * @since 0.0.0
 */
public abstract class PlymouthSQL implements Plymouth {
    protected Driver driver;
    protected Connection connection;

    protected PlymouthSQL(Driver driver) {
        this.driver = driver;
    }

    protected PlymouthSQL(Driver driver, String uri, Properties properties) throws PlymouthException {
        this(driver);
        try {
            this.connection = driver.connect(uri, properties);
        } catch (SQLException sql) {
            throw new PlymouthException(sql);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
