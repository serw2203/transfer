package ru.transfer.init;

import ru.transfer.query.BatchQuery;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 *
 */
public class CrossRateBatchQuery implements BatchQuery{

    private final static SimpleDateFormat DDMMYYYY = new SimpleDateFormat("dd.MM.yyyy");
    private final static SimpleDateFormat YYYYMMDD = new SimpleDateFormat("yyyy-MM-dd");

    private final static Date D01012006;

    private final static long STEP = new Long(10 * 24 * 60 * 60 * 1000).longValue();

    private final static Random RANDOM = new Random();

    static {
        try {
            D01012006 = DDMMYYYY.parse("01.01.2006");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private final static String INSERT_CROSS_RATE = "insert into aaa_cross_rate (scur_code, tcur_code, date_rate, rate)\n" +
            "select s.cur_code , t.cur_code, '%s', case s.cur_code when 'RUB' then round(s.ranq / t.ranq, 2) else round(s.ranq / t.ranq, 4) end from\n" +
            "(select cur_code,\n" +
            "          case cur_code when 'RUB' then 65.75 * %s\n" +
            "                        when 'EUR' then 1.15  * %s\n" +
            "                        when 'GBP' then 0.68  * %s\n" +
            "                        when 'SHF' then 1.23  * %s\n" +
            "          else 1 end ranq\n" +
            "          from aaa_currency)  s\n" +
            "join (select cur_code,\n" +
            "          case cur_code when 'RUB' then 65.75\n" +
            "                        when 'EUR' then 1.15\n" +
            "                        when 'GBP' then 0.68\n" +
            "                        when 'SHF' then 1.23\n" +
            "          else 1 end ranq\n" +
            "          from aaa_currency) t on s.cur_code != t.cur_code";


    @Override
    public Statement createStatement(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        for (long i = D01012006.getTime(); i < System.currentTimeMillis(); i +=  STEP ) {
            double d = 1d + 1d/(1 + RANDOM.nextInt(10));
            statement.addBatch(
                    String.format(INSERT_CROSS_RATE, YYYYMMDD.format(new Date(i)),
                            String.valueOf(d), String.valueOf(d), String.valueOf(d), String.valueOf(d) ));
        }
        return statement;
    }
}
