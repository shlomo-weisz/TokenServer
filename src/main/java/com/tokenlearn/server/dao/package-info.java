/**
 * JDBC-based data access layer.
 *
 * <p>DAOs talk directly to SQL Server through {@code NamedParameterJdbcTemplate}
 * and are responsible for persistence concerns only. Business rules live in the
 * service layer above them.
 */
package com.tokenlearn.server.dao;
