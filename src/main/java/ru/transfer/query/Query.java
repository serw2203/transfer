package ru.transfer.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public interface Query {
    PreparedStatement createPreparedStatement(Connection connection) throws SQLException;
}
