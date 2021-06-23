package net.kjp12.plymouth.database.cache;// Created 2021-14-06T14:56:15

import java.lang.annotation.Target;

/**
 * @author KJP12
 * @since ${version}
 **/
@Target({})
public @interface Match {
    int table() default 0;

    String primary() default "";

    String secondary() default "";
}
