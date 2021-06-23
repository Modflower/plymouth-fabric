package gay.ampflower.plymouth.database.cache;

import gay.ampflower.plymouth.database.PlymouthException;
import gay.ampflower.plymouth.database.records.LookupRecord;
import gay.ampflower.plymouth.database.records.PlymouthRecord;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Intermediate statement handler class.
 *
 * @author Ampflower
 * @since ${version}
 **/
abstract class StatementHandler<I extends LookupRecord<O>, O extends PlymouthRecord> {
    protected final String statementRaw;
    protected final SqlConnectionProvider provider;
    protected PreparedStatement statement;
    long lastUsed;

    protected StatementHandler(SqlConnectionProvider provider, String statementRaw) {
        this.provider = provider;
        this.statementRaw = statementRaw;
        prepareStatement();
    }

    abstract void query(I i);

    final void prepareStatement() throws PlymouthException {
        try {
            closeStatement();
            statement = provider.getConnection().prepareStatement(statementRaw);
        } catch (SQLException sql) {
            throw new PlymouthException(sql, statement, statementRaw);
        }
    }

    final void closeStatement() throws PlymouthException {
        if (statement != null) try {
            statement.close();
        } catch (SQLException sql) {
            throw new PlymouthException(sql, statement);
        } finally {
            statement = null;
        }
    }
}
