package net.kjp12.plymouth.database;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;

/**
 * Exception for database failures or malformed requests.
 * <p>
 * If {@link SQLException} is passed in any throwable-accepting constructor,
 * any following {@link SQLException} contained within the batch update exception
 * will be added as suppressed exceptions to allow examining.
 *
 * @author KJP12
 * @since ${version}
 */
public class PlymouthException extends RuntimeException {
    /**
     * Exception with only the cause known. No request aliased.
     */
    public PlymouthException(Throwable cause) {
        super(cause);
        if (cause instanceof SQLException) {
            addBatchedSuppressed((SQLException) cause);
        }
    }

    /**
     * Exception with the cause known. One request aliased.
     */
    public PlymouthException(Throwable cause, Object statement) {
        super(Objects.toString(statement), cause);
        if (cause instanceof SQLException) {
            addBatchedSuppressed((SQLException) cause);
        }
    }

    /**
     * Exception with the cause known. Multiple requests aliased.
     */
    public PlymouthException(Throwable cause, Object... statements) {
        super(writeOut(statements), cause);
        if (cause instanceof SQLException) {
            addBatchedSuppressed((SQLException) cause);
        }
    }

    private static String writeOut(Object[] statements) {
        var sb = new StringBuilder("Failure point:\n");
        for (var s : statements) {
            sb.append("\n - ").append(s);
        }
        return sb.toString();
    }

    private void addBatchedSuppressed(SQLException exception) {
        var dejavu = new HashSet<Throwable>();
        dejavu.add(exception);
        for (var throwable : exception) if (dejavu.add(throwable)) addSuppressed(throwable);
    }
}
