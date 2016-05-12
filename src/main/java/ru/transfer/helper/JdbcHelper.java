package ru.transfer.helper;

import ru.transfer.query.BatchQuery;
import ru.transfer.query.DataQuery;
import ru.transfer.query.UpdateQuery;

import java.sql.*;
import java.util.List;

/**
 *
 */
public class JdbcHelper<T> {

    public List<T> executeQuery(DataQuery<T> query) throws Exception {
        try (
                Connection connection = ConnectionHelper.connection();
                PreparedStatement stmt = query.createPreparedStatement(connection);) {
            try (ResultSet resultSet = stmt.executeQuery()) {
                return query.handle(resultSet);
            } catch (SQLException e) {
                throw new SQLException(e);
            }
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    public int executeUpdate(UpdateQuery query) throws Exception {
        try (
                Connection connection = ConnectionHelper.connection();
                PreparedStatement stmt = query.createPreparedStatement(connection);) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    public void executeUpdates(UpdateQuery[] queries) throws Exception {
        try (
                Connection connection = ConnectionHelper.connection();
        ) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                for (UpdateQuery query : queries) {
                    try (PreparedStatement stmt = query.createPreparedStatement(connection);) {
                        if (query instanceof DataQuery) {
                            ResultSet resultSet = stmt.executeQuery();
                            ((DataQuery) query).handle(resultSet);
                        } else {
                            stmt.executeUpdate();
                        }
                    } catch (Exception e) {
                        connection.rollback();
                        throw new SQLException(e);
                    }
                }
                connection.commit();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public void executeBatch(BatchQuery query) throws Exception {
        try (
                Connection connection = ConnectionHelper.connection();
                Statement stmt = query.createStatement(connection);) {
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }
}
