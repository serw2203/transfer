package ru.transfer.init;

import ru.transfer.query.BatchQueries;

import java.sql.Connection;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 */
public class OperationBatchQueries implements BatchQueries {
    private final static SimpleDateFormat YYYYMMDDHHMMSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final int dayCount;
    private final static int STEP = 366;

    private final static String INSERT_OPER =
            "insert into aaa_oper (oper_id, h_client_id, oper_date, oper_type, oper_acc_id, oper_cur_code)\n" +
                    "select seq_id.nextval as oper_id,\n" +
                    "       hc.h_client_id, '%s' as oper_date, '%s' as oper_type,\n" +
                    "                              a.acc_id as oper_acc_id, c.cur_code as oper_cur_code from aaa_account a\n" +
                    "join aaa_currency c on 1 = 1\n" +
                    "join aaa_h_client hc on hc.client_id = a.client_id and hc.cli_version = 0\n" +
                    "where a.acc_id != 0 ";

    private final static String INSERT_INPUT_TURNS =
            "insert into aaa_turn (turn_id, oper_id, acc_id, cur_code, d_amount, k_amount, turn_date)\n" +
                    "select seq_id.nextval as turn_id, o.oper_id,\n" +
                    "        o.oper_acc_id, o.oper_cur_code,\n" +
                    "        s.amount as d_amount,\n" +
                    "        0 as k_amount, o.oper_date from aaa_oper o\n" +
                    "join (select %s as amount) s on 1 = 1\n" +
                    "where oper_date = '%s' order by o.oper_id";

    private final static String INSERT_OUTPUT_TURNS =
            "insert into aaa_turn (turn_id, oper_id, acc_id, cur_code, d_amount, k_amount, turn_date)\n" +
                    "select seq_id.nextval as turn_id, o.oper_id,\n" +
                    "        o.oper_acc_id as oper_acc_id, o.oper_cur_code,\n" +
                    "        0 as d_amount,\n" +
                    "        s.amount  as k_amount, o.oper_date from aaa_oper o\n" +
                    "join (select %s as amount) s on 1 = 1\n" +
                    "where oper_date = '%s' order by o.oper_id";

    private final static String SYNC_BALANCES_ALL =
            "insert into aaa_balance (acc_id, cur_code, balance)\n" +
                    "select t.acc_id, t.cur_code, sum(t.d_amount - t.k_amount) from aaa_turn t\n" +
                    "group by t.acc_id, t.cur_code";


    public OperationBatchQueries(int dayCount) {
        this.dayCount = dayCount;
    }

    @Override
    public Statement createStatement(Connection connection) throws Exception {
        Statement statement = connection.createStatement();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(YYYYMMDDHHMMSS.parse("2006-01-01 10:00:00"));
        for (int i = 0; i < dayCount; i++) {
            String inputDate = YYYYMMDDHHMMSS.format(calendar.getTime());
            statement.addBatch(String.format(INSERT_OPER, inputDate, "INPUT"));
            statement.addBatch(String.format(INSERT_INPUT_TURNS, "3.00", inputDate));
            calendar.add(Calendar.HOUR_OF_DAY, 5);
            String outputDate = YYYYMMDDHHMMSS.format(calendar.getTime());
            statement.addBatch(String.format(INSERT_OPER, outputDate, "OUTPUT"));
            statement.addBatch(String.format(INSERT_OUTPUT_TURNS, "1.00", outputDate));
            calendar.add(Calendar.HOUR_OF_DAY, 19 + 24 * STEP);
        }
        statement.addBatch(SYNC_BALANCES_ALL);
        return statement;
    }
}
