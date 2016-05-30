package ru.transfer.query;

import ru.transfer.util.Utils;

import java.sql.*;

/**
 *
 */
public class CommonQuery implements Query {
    private String sql;
    private Object[] params;

    protected CommonQuery() {
    }

    public static CommonQuery instance(String sql, Object[] params) {
        CommonQuery query = new CommonQuery();
        query.sql = sql;
        query.params = params;
        Utils.traceSql(sql, params);
        return query;
    }

    public static CommonQuery instance(String sql) {
        return instance(sql, null);
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(this.sql);
        if (this.params != null) {
            for (int i = 0; i < this.params.length; i++) {
                if (this.params[i] != null) {
                    stmt.setObject(i + 1, this.params[i]);
                } else {
                    stmt.setNull(i + 1, Types.VARCHAR);
                }
            }
        }
        return stmt;
    }
}
