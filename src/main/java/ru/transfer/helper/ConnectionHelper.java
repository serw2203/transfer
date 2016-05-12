package ru.transfer.helper;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 */
public class ConnectionHelper {
    private static ComboPooledDataSource cpds;

    static {
        try {
            cpds = new ComboPooledDataSource();
            cpds.setDriverClass("org.h2.Driver");
            cpds.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;LOCK_MODE=3");
            cpds.setMinPoolSize(10);
            cpds.setAcquireIncrement(2);
            cpds.setMaxPoolSize(100);
            cpds.setMaxStatements(0);
            cpds.setNumHelperThreads(8);
            cpds.setAcquireRetryAttempts(50);
            cpds.setTestConnectionOnCheckin(true);
            cpds.setTestConnectionOnCheckout(true);
            cpds.setPreferredTestQuery("select 1");
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    public static Connection connection() throws PropertyVetoException, SQLException {
        return cpds.getConnection();
    }
}
