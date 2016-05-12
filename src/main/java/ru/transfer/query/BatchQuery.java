package ru.transfer.query;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
public interface BatchQuery {
    Statement createStatement(Connection connection) throws SQLException;
}
