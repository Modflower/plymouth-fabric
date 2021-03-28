package gay.ampflower.plymouth.database;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Objects;

public class PlymouthException extends RuntimeException {
    public PlymouthException(Statement statement, Throwable cause) {
        super(Objects.toString(statement), cause);
        if (cause instanceof BatchUpdateException) {
            addBatchedSuppressed((BatchUpdateException) cause);
        }
    }

    public PlymouthException(Throwable cause, Statement... statements) {
        super(writeOut(statements), cause);
        if (cause instanceof BatchUpdateException) {
            addBatchedSuppressed((BatchUpdateException) cause);
        }
    }

    private static String writeOut(Statement[] statements) {
        var sb = new StringBuilder("Failure point:\n");
        for (var s : statements) {
            sb.append("\n - ").append(s);
        }
        return sb.toString();
    }

    private void addBatchedSuppressed(BatchUpdateException exception) {
        SQLException sql;
        var dejavu = new HashSet<Throwable>();
        dejavu.add(exception.getCause());
        while ((sql = exception.getNextException()) != null) {
            if (!dejavu.add(sql)) return;
            addSuppressed(sql);
        }
    }
}
