package ru.transfer.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public interface UpdateQuery {
    PreparedStatement createPreparedStatement(Connection connection) throws SQLException;
}
