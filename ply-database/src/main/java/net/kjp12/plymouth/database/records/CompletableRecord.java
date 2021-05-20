package net.kjp12.plymouth.database.records;// Created 2021-07-05T01:42:06

/**
 * A record that is a request and carries a future that can either be completed or failed.
 *
 * @author KJP12
 * @since ${version}
 **/
public interface CompletableRecord<T> {
    /**
     * Polyglot interface for the underlying future.
     *
     * @param object The object that the request completed with.
     */
    void complete(T object);

    /**
     * Interface for the underlying future. Implementations of this method must never fail.
     *
     * @param throwable The error that was thrown when the request failed.
     */
    void fail(Throwable throwable);
}
