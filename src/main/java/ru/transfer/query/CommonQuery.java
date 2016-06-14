package ru.transfer.query;

import ru.transfer.util.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class CommonQuery implements Query {
    private String sql;
    private Object[] params;

    private CommonQuery() {
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
        final PreparedStatement stmt = connection.prepareStatement(this.sql);
        final AtomicInteger i = new AtomicInteger(1);
        if (this.params != null) {
            Arrays.stream(this.params).forEach(param -> {
                try {
                    if (param != null) {
                        stmt.setObject(i.get(), param);
                    } else {
                        stmt.setNull(i.get(), Types.NULL);
                    }
                    i.set(i.get() + 1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return stmt;
    }
}
