package ru.transfer.init;

import ru.transfer.query.BatchQueries;

import java.sql.Connection;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class CrossRateBatchQueries implements BatchQueries {

    //every 10 days
    private final static Long STEP = 10L * 24L * 60L * 60L * 1000L;

    private final static SimpleDateFormat YYYYMMDD = new SimpleDateFormat("yyyy-MM-dd");

    private final static String INSERT_CROSS_RATE =
            "insert into aaa_cross_rate (scur_code, tcur_code, date_rate, rate) " +
                    "values ('%s', '%s', '%s', %s)";

    private final static String[] RATEROWS = new String[]{
            "GBP/RUB/96.2293",
            "EUR/RUB/73.3324",
            "USD/RUB/65.8643",
            "CHF/RUB/66.3305",
            "EUR/USD/1.1138",
            "EUR/GBP/0.7619",
            "EUR/CHF/1.1056",
            "USD/EUR/0.8976",
            "USD/GBP/0.6839",
            "USD/CHF/0.9927",
            "GBP/USD/1.4614",
            "GBP/EUR/1.3118",
            "GBP/CHF/1.4508",
            "GBP/CHF/1.4509",
            "EUR/CHF/1.1053",
            "USD/CHF/0.9928"};

    @Override
    public Statement createStatement (Connection connection) throws Exception {
        Statement statement = connection.createStatement();
        for (long i = YYYYMMDD.parse("2006-01-01").getTime(); i < System.currentTimeMillis(); i += STEP) {
            for (String row : RATEROWS) {
                String[] flds = row.split("/");
                statement.addBatch(
                        String.format(INSERT_CROSS_RATE, flds[0], flds[1], YYYYMMDD.format( new Date(i)), flds[2]) );
            }
        }

        return statement;
    }
}
