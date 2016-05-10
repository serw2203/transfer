package ru.transfer.helper;

import ru.transfer.query.Execute;
import ru.transfer.query.Query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *
 */
public class JdbcHelper<T> {
    private Connection connection;

    public JdbcHelper() {
        try {
            this.connection = ConnectionHelper.connection();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private void initStatement(PreparedStatement stmt, Execute handler) throws SQLException {
        if (handler.getParams() != null) {
            for (int i = 0; i < handler.getParams().length; i++) {
                if (handler.getParams()[i] == null) {
                    throw new IllegalArgumentException(String.format("Param with index %s must to be not null"));
                }
                stmt.setObject(i + 1, handler.getParams()[i]);
            }
        }
    }

    public List<T> executeQuery(Query handler) throws Exception {
        try (
                PreparedStatement stmt = getConnection().prepareStatement(handler.sql())) {
            initStatement(stmt, handler);
            try (ResultSet resultSet = stmt.executeQuery()) {
                return handler.handle(resultSet);
            } catch (SQLException e) {
                throw new SQLException(e);
            }
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    public boolean execute(Execute handler) throws Exception {
        try (
                PreparedStatement stmt = getConnection().prepareStatement(handler.sql())) {
            initStatement(stmt, handler);
            return stmt.execute();
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }
}
