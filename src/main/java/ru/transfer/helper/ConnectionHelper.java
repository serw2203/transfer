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

    public static Connection connection() throws PropertyVetoException, SQLException {
        if (cpds == null) {
            cpds = new ComboPooledDataSource();
            cpds.setDriverClass("org.firebirdsql.jdbc.FBDriver");
            cpds.setJdbcUrl("jdbc:firebirdsql:localhost/3050:d:/databases/alf_data_off_sp.gdb?sql_dialect=1;encoding=WIN1251");
            cpds.setUser("sysdba");
            cpds.setPassword("masterkey");
            cpds.setMinPoolSize(10);
            cpds.setAcquireIncrement(2);
            cpds.setMaxPoolSize(100);
            cpds.setMaxStatements(0);
            cpds.setNumHelperThreads(8);
            cpds.setAcquireRetryAttempts(50);
            cpds.setTestConnectionOnCheckin(true);
            cpds.setTestConnectionOnCheckout(true);
        }
        return cpds.getConnection();
    }
}
