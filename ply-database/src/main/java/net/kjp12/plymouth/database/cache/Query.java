package net.kjp12.plymouth.database.cache;// Created 2021-18-06T01:24:12

import java.lang.annotation.*;

/**
 * @author KJP12
 * @since ${version}
 **/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Queries.class)
public @interface Query {
    /**
     * Context based off the input record.
     * A breakout to provider can be done by prefixing with <code>^</code>.
     * Variable instructions are prefixed by <code>::</code>.
     * <p>
     * Load: <code>::I&lt;0</code>
     * <p>
     * Store: <code>variable::I&gt;0</code>
     */
    String[] values();

    /**
     * Raw SQL where query parameter using <code>?</code> as the
     */
    String query();

    int mask();

    int maskRq() default -1;
}
