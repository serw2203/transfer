package ru.transfer.query.impl;

import ru.transfer.query.UpdateQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public class CommonUpdateQuery implements UpdateQuery {
    private String sql;
    private Object[] params;

    protected CommonUpdateQuery() {
    }

    public static CommonUpdateQuery instance (String sql, Object[] params) {
        CommonUpdateQuery query = new CommonUpdateQuery();
        query.sql = sql;
        query.params = params;
        return query;
    }

    public static CommonUpdateQuery instance (String sql) {
        return instance (sql, null);
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(this.sql);
        if (this.params != null) {
            for (int i = 0; i < this.params.length; i++) {
                if (this.params[i] == null) {
                    throw new IllegalArgumentException(String.format("Param with index %d must to be not null", i));
                }
                stmt.setObject(i + 1, this.params[i]);
            }
        }
        return stmt;
    }
}
