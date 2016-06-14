package ru.transfer.helper;

import org.h2.util.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.transfer.query.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@SuppressWarnings("unchecked")
public class Jdbc {
    private final static Logger log = LoggerFactory.getLogger(Jdbc.class);

    private long trst;
    private Connection connection = null;

    private void checkTrans() {
        if (this.connection == null) {
            throw new IllegalStateException("Transaction is not initialized");
        }
    }

    private long start() {
        return System.currentTimeMillis();
    }

    private void stop(long start) {
        log.trace("Elapsed time - {} s <---", (System.currentTimeMillis() - start) / 1000.0);
    }

    private Connection conn() throws Exception {
        return this.connection == null ? ConnectionHelper.connection() : null;
    }

    private PreparedStatement stmt(Connection conn, String sql, Object[] params) throws Exception {
        return conn == null ? CommonQuery.instance(sql, params).createPreparedStatement(this.connection) :
                CommonQuery.instance(sql, params).createPreparedStatement(conn);
    }

    private PreparedStatement stmt(Connection conn, Query query) throws Exception {
        return conn == null ? query.createPreparedStatement(this.connection) : query.createPreparedStatement(conn);
    }

    private Statement stmt(Connection conn, BatchQueries query) throws Exception {
        return conn == null ? query.createStatement(this.connection) : query.createStatement(conn);
    }

    public boolean inTrans() throws Exception {
        return this.connection != null && !this.connection.getAutoCommit();
    }

    public void createTrans() throws Exception {
        if (this.connection != null) {
            throw new IllegalStateException("Transaction already exist. Nested transaction not supported");
        }
        log.trace("CREATE TRANS ***********************");
        this.connection = ConnectionHelper.connection();
        this.connection.setAutoCommit(false);
        this.trst = System.currentTimeMillis();
    }

    public void commitTrans() throws Exception {
        checkTrans();
        try {
            log.trace("COMMIT TRANS *********************** Elapsed time - {} s",
                    (System.currentTimeMillis() - this.trst) / 1000.0);
            this.connection.commit();
        } finally {
            releaseTrans();
        }
    }

    public void rollbackTrans() throws Exception {
        checkTrans();
        try {
            log.trace("ROLLBACK TRANS *********************** Elapsed time - {} s",
                    (System.currentTimeMillis() - this.trst) / 1000.0);
            this.connection.rollback();
        } finally {
            releaseTrans();
        }
    }

    private void releaseTrans() throws Exception {
        try {
            this.connection.setAutoCommit(true);
        } finally {
            JdbcUtils.closeSilently(this.connection);
            this.connection = null;
        }
    }

    public List<Map<String, Object>> executeQuery(String sql, Object[] params) throws Exception {
        long st = start();
        try (
                Connection conn = conn();
                PreparedStatement stmt = stmt(conn, sql, params);
                ResultSet resultSet = stmt.executeQuery()
        ) {
            List<Map<String, Object>> result = new ArrayList();
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                int colCount = resultSet.getMetaData().getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    row.put(resultSet.getMetaData().getColumnLabel(i), resultSet.getObject(i));
                }
                result.add(row);
            }
            stop(st);
            return result;
        }
    }

    public  List<Map<String, Object>> executeQuery(String sql) throws Exception {
        return executeQuery(sql, null);
    }

    public <T> List<T> executeQuery(DataQuery<T> query) throws Exception {
        long st = start();
        try (
                Connection connection = conn();
                PreparedStatement stmt = stmt(connection, query);
                ResultSet resultSet = stmt.executeQuery()) {
            stop(st);
            return query.handle(resultSet);
        }
    }

    public int executeUpdate(String sql, Object[] params) throws Exception {
        long st = start();
        try (
                Connection connection = conn();
                PreparedStatement stmt = stmt(connection, sql, params)) {
            int result = stmt.executeUpdate();
            stop(st);
            return result;
        }
    }

    public int executeUpdate(UpdateQuery query) throws Exception {
        long st = start();
        try (
                Connection connection = conn();
                PreparedStatement stmt = stmt(connection, query)) {
            int result = stmt.executeUpdate();
            stop(st);
            return result;
        }
    }

    public int[] executeBatch(BatchQueries query) throws Exception {
        try (
                Connection connection = conn();
                Statement stmt = stmt(connection, query)) {
            return stmt.executeBatch();
        }
    }
}
