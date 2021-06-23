package net.kjp12.plymouth.database.cache;// Created 2021-14-06T14:56:31

import java.lang.annotation.*;

/**
 * @author KJP12
 * @since ${version}
 **/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Tables.class)
public @interface Table {
    int table() default 0;

    String value();

    Match match() default @Match;
}
