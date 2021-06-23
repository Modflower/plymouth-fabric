package net.kjp12.plymouth.database.cache;// Created 2021-14-06T14:58:12

import java.sql.Connection;

/**
 * @author KJP12
 * @since ${version}
 **/
public interface SqlConnectionProvider {
    Connection getConnection();
}
