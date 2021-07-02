package net.kjp12.plymouth.database.cache;// Created 2021-30-06T14:14:14

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author KJP12
 * @since ${version}
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pagination {
    Value sort() default @Value({});

    String limit() default "";

    String offset() default "";
}
