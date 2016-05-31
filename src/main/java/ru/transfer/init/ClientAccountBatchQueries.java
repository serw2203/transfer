package ru.transfer.init;

import ru.transfer.query.BatchQueries;

import java.sql.Connection;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
public class ClientAccountBatchQueries implements BatchQueries {

    private final static SimpleDateFormat YYYYMMDDHHMMSS = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private final static SimpleDateFormat DDMMYYYY = new SimpleDateFormat("dd.MM.yyyy");

    private final static int CLIENT_COUNT = 10000;

    private final static Date D01012006;
    private final static Date D01012010;
    private final static Date D01012015;

    static {
        try {
            D01012006 = DDMMYYYY.parse("01.01.2006");
            D01012010 = DDMMYYYY.parse("01.01.2010");
            D01012015 = DDMMYYYY.parse("01.01.2015");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private final static String INSERT_CLIENT = "insert into aaa_client values (%d);";
    private final static String INSERT_H_CLIENT = "insert into aaa_h_client (h_client_id, client_id, last_name, first_name, " +
            "middle_name, modify_date, cli_version) values (%d, %d, '%s', '%s', '%s', '%s', %d);";
    private final static String INSERT_ACCOUNT = "insert into aaa_account (acc_id, client_id, acc_num) values (%d, %d, '%s' );";

    @Override
    public Statement createStatement(Connection connection) throws Exception {
        Statement statement = connection.createStatement();
        for (int i = 1; i < CLIENT_COUNT; i++) {
            statement.addBatch(String.format(INSERT_CLIENT, i));
            statement.addBatch(String.format(INSERT_H_CLIENT,
                    i, i, "CLI_" + i, "FN1", "MN1", YYYYMMDDHHMMSS.format(D01012006), -2));
            statement.addBatch(String.format(INSERT_ACCOUNT, i, i, "ACC" + i));
        }

        for (int i = CLIENT_COUNT + 1; i < CLIENT_COUNT * 2; i++) {
            statement.addBatch(String.format(INSERT_H_CLIENT,
                    i, i - CLIENT_COUNT, "CLI_" + (i - CLIENT_COUNT), "FN2", "MN2", YYYYMMDDHHMMSS.format(D01012010), -1));
        }

        for (int i = CLIENT_COUNT * 2 + 1; i < CLIENT_COUNT * 3; i++) {
            statement.addBatch(String.format(INSERT_H_CLIENT,
                    i, i - CLIENT_COUNT * 2, "CLI_" + (i - CLIENT_COUNT * 2), "FN3", "MN3", YYYYMMDDHHMMSS.format(D01012015), 0));
        }

        return statement;
    }
}
