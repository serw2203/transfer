package ru.transfer.init;

import ru.transfer.query.BatchQueries;

import java.sql.Connection;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 *
 */
public class CrossRateBatchQueries implements BatchQueries {
    private final static SimpleDateFormat YYYYMMDD = new SimpleDateFormat("yyyy-MM-dd");

    private final static Date D01012006;

    private final static Long STEP = 10L * 24L * 60L * 60L * 1000L;

    private final static Random RANDOM = new Random();

    static {
        try {
            D01012006 = YYYYMMDD.parse("2006-01-01");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private final static String INSERT_CROSS_RATE = "insert into aaa_cross_rate (scur_code, tcur_code, date_rate, rate) " +
            "select s.cur_code , t.cur_code, '%s', round(case when s.ranq / t.ranq > 1 then k.koef * s.ranq / t.ranq else s.ranq / (k.koef * t.ranq) end, 8) end from " +
            "(select cur_code, " +
            "          case cur_code when 'RUB' then 100.00 " +
            "                        when 'EUR' then 10.00 " +
            "                        when 'GBP' then 0.10 " +
            "                        when 'CHF' then 0.01 " +
            "          else 1 end ranq " +
            "          from aaa_currency)  s " +
            "join (select cur_code, " +
            "          case cur_code when 'RUB' then 100.00 " +
            "                        when 'EUR' then 10.00 " +
            "                        when 'GBP' then 0.10 " +
            "                        when 'CHF' then 0.01 " +
            "          else 1 end ranq " +
            "          from aaa_currency) t on s.cur_code != t.cur_code " +
            "join (select round(%s, 8) koef) k on 1 = 1";


    @Override
    public Statement createStatement(Connection connection) throws Exception {
        Statement statement = connection.createStatement();
        for (long i = D01012006.getTime(); i < System.currentTimeMillis(); i += STEP) {
            double d = 1.0 + 1.0 / (1 + RANDOM.nextInt(100));
            statement.addBatch(
                    String.format(INSERT_CROSS_RATE, YYYYMMDD.format(new Date(i)), String.valueOf(d)));
        }

        return statement;
    }
}
