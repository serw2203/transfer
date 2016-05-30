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

    private Connection connection = null;

    private void check() {
        if (this.connection == null) {
            throw new IllegalStateException("Transaction is not initialized");
        }
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

    public boolean hasTrans() throws Exception {
        return this.connection != null && !this.connection.getAutoCommit();
    }

    /**
     * @throws Exception
     */
    public void createTrans() throws Exception {
        if (this.connection != null) {
            throw new IllegalStateException("Transaction already exist. Nested transaction not supported");
        }
        this.log.trace("CREATE TRANS ***********************");
        this.connection = ConnectionHelper.connection();
        this.connection.setAutoCommit(false);
    }

    /**
     * @throws SQLException
     */
    public void commitTrans() throws Exception {
        check();
        try {
            this.log.trace("COMMIT TRANS ***********************");
            this.connection.commit();
        } finally {
            releaseTrans();
        }
    }

    /**
     * @throws SQLException
     */
    public void rollbackTrans() throws Exception {
        check();
        try {
            this.log.trace("ROLLBACK TRANS ***********************");
            this.connection.rollback();
        } finally {
            releaseTrans();
        }
    }

    /**
     * @throws SQLException
     */
    protected void releaseTrans() throws Exception {
        check();
        try {
            this.connection.setAutoCommit(true);
        } finally {
            JdbcUtils.closeSilently(this.connection);
            this.connection = null;
        }
    }

    /**
     * @param sql
     * @param params
     * @return
     * @throws Exception
     */
    public List<Map<String, ?>> executeQuery(String sql, Object[] params) throws Exception {
        try (
                Connection conn = conn();
                PreparedStatement stmt = stmt(conn, sql, params);
                ResultSet resultSet = stmt.executeQuery();
        ) {
            List<Map<String, ?>> result = new ArrayList();
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                int colCount = resultSet.getMetaData().getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    row.put(resultSet.getMetaData().getColumnLabel(i), resultSet.getObject(i));
                }
                result.add(row);
            }
            return result;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * @param sql
     * @return
     * @throws Exception
     */
    public List<Map<String, ?>> executeQuery(String sql) throws Exception {
        return executeQuery(sql, null);
    }

    /**
     * @param query
     * @return
     * @throws Exception
     */
    public <T> List<T> executeQuery(DataQuery<T> query) throws Exception {
        try (
                Connection connection = conn();
                PreparedStatement stmt = stmt(connection, query);
                ResultSet resultSet = stmt.executeQuery()) {
            return query.handle(resultSet);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * @param query
     * @return
     * @throws Exception
     */
    public int executeUpdate(UpdateQuery query) throws Exception {
        try (
                Connection connection = conn();
                PreparedStatement stmt = stmt(connection, query)) {
            return stmt.executeUpdate();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * @param sql
     * @param params
     * @return
     * @throws Exception
     */
    public int executeUpdate(String sql, Object[] params) throws Exception {
        try (
                Connection connection = conn();
                PreparedStatement stmt = stmt(connection, sql, params)) {
            return stmt.executeUpdate();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * @param query
     * @return
     * @throws Exception
     */
    public int[] executeBatch(BatchQueries query) throws Exception {
        try (
                Connection connection = conn();
                Statement stmt = stmt(connection, query)) {
            return stmt.executeBatch();
        } catch (Exception e) {
            throw e;
        }
    }
}
