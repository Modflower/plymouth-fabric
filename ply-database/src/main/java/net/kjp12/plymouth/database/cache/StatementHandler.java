package net.kjp12.plymouth.database.cache;// Created 2021-14-06T15:09:52

import net.kjp12.plymouth.database.PlymouthException;
import net.kjp12.plymouth.database.records.LookupRecord;
import net.kjp12.plymouth.database.records.PlymouthRecord;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Intermediate statement handler class.
 *
 * @author KJP12
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
