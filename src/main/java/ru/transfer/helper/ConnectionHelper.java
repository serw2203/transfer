package ru.transfer.helper;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.sql.Connection;

/**
 *
 */
public class ConnectionHelper {
    private static ComboPooledDataSource cpds;

    static {
        try {
            cpds = new ComboPooledDataSource();
            cpds.setDriverClass("org.h2.Driver");
            cpds.setJdbcUrl("jdbc:h2:mem:transfer;DB_CLOSE_DELAY=-1;LOCK_MODE=3;LOCK_TIMEOUT=60000");
            cpds.setMinPoolSize(10);
            cpds.setAcquireIncrement(2);
            cpds.setMaxPoolSize(100);
            cpds.setMaxStatements(0);
            cpds.setNumHelperThreads(8);
            cpds.setAcquireRetryAttempts(50);
            cpds.setMaxIdleTime(0);
            cpds.setIdleConnectionTestPeriod(300);
            cpds.setTestConnectionOnCheckin(true);
            cpds.setTestConnectionOnCheckout(true);
            cpds.setPreferredTestQuery("select 1");
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    public static Connection connection() throws Exception {
        return cpds.getConnection();
    }
}
