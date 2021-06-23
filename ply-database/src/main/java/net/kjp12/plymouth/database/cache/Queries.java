package net.kjp12.plymouth.database.cache;// Created 2021-18-06T01:29:37

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author KJP12
 * @since ${version}
 **/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Queries {
    Query[] value();
}
