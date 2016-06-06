package ru.transfer.dao;

import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;
import ru.transfer.model.Currency;
import ru.transfer.query.CommonQuery;
import ru.transfer.query.DataQuery;
import ru.transfer.query.impl.AccountByClient;
import ru.transfer.query.impl.AccountByNumber;
import ru.transfer.util.Utils;

import javax.xml.datatype.DatatypeConfigurationException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 *
 */
public class AnalyticalDaoImpl implements AnalyticalDao {

    /*CURRENCY*/
    private static class CurrencyDataQuery implements DataQuery<Currency> {
        @Override
        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            return CommonQuery.instance(
                    "select cur_code from aaa_currency").createPreparedStatement(connection);
        }

        @Override
        public List<Currency> handle(ResultSet resultSet) throws Exception {
            List<Currency> result = new ArrayList<>();
            while (resultSet.next()) {
                Currency currency = new Currency();
                currency.setCode(resultSet.getString("CUR_CODE"));
                result.add(currency);
            }
            return result;
        }
    }

    @Override
    public List<Currency> currencies(Jdbc jdbc) throws Exception {
        return jdbc.executeQuery(new CurrencyDataQuery());
    }

    /*CLIENT*/
    private static class ClientDataQuery implements DataQuery<Client> {
        private Long[] params;

        public ClientDataQuery withParams(Long clientId) {
            this.params = new Long[]{Utils.NNE(clientId)};
            return this;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            this.params = Utils.NNE(params);
            return CommonQuery.instance(
                    "select c.client_id, hc.last_name, hc.first_name, hc.middle_name, hc.modify_date from aaa_client c\n" +
                            "join aaa_h_client hc on c.client_id = hc.client_id and hc.cli_version = 0\n" +
                            "where c.client_id = ?", this.params).createPreparedStatement(connection);
        }

        @Override
        public List<Client> handle(ResultSet resultSet) throws SQLException, DatatypeConfigurationException {
            List<Client> result = new ArrayList<>();
            while (resultSet.next()) {
                Client client = new Client();
                client.setClientId(resultSet.getLong("CLIENT_ID"));
                client.setFirstName(resultSet.getString("FIRST_NAME"));
                client.setLastName(resultSet.getString("LAST_NAME"));
                client.setMiddleName(resultSet.getString("MIDDLE_NAME"));
                client.setModifyDate(resultSet.getTimestamp("MODIFY_DATE"));
                result.add(client);
            }
            return result;
        }
    }

    @Override
    public Client client(Jdbc jdbc, Long clientId) throws Exception {
        return Utils.first(
                jdbc.executeQuery(new ClientDataQuery().withParams(Utils.NNE(clientId))));
    }

    /*ACCOUNT*/
    @Override
    public List<Account> accounts(Jdbc jdbc, Long clientId) throws Exception {
        return Utils.NNE(
                jdbc.executeQuery(new AccountByClient().withParams(Utils.NNE(clientId))));
    }

    @Override
    public Account account(Jdbc jdbc, String accNum) throws Exception {
        return Utils.first(
                jdbc.executeQuery(new AccountByNumber().withParams(Utils.NNE(accNum))));
    }

    /*RATE*/
    private static class RateDataQuery implements DataQuery<Rate> {
        private Object[] params;

        public RateDataQuery withParams(String sCur, String tCur, Timestamp dateRate) {
            if (dateRate == null)
                dateRate = new Timestamp(System.currentTimeMillis());
            this.params = new Object[]{Utils.NNE(sCur), Utils.NNE(tCur), dateRate};
            return this;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            this.params = Utils.NNE(params);
            return CommonQuery.instance(
                    "select top 1 scur_code, tcur_code, date_rate, rate from aaa_cross_rate \n" +
                            "where scur_code = ? and tcur_code = ? and date_rate <= ? order by date_rate desc", this.params)
                    .createPreparedStatement(connection);
        }

        @Override
        public List<Rate> handle(ResultSet resultSet) throws Exception {
            List<Rate> result = new ArrayList<>();
            while (resultSet.next()) {
                Rate rate = new Rate();
                rate.setScurCode(resultSet.getString("SCUR_CODE"));
                rate.setTcurCode(resultSet.getString("TCUR_CODE"));
                rate.setDateRate(resultSet.getTimestamp("DATE_RATE"));
                rate.setRate(resultSet.getBigDecimal("RATE"));
                result.add(rate);
            }
            return result;
        }
    }

    @Override
    public Rate rate(Jdbc jdbc, String sCur, String tCur, Timestamp dateRate) throws Exception {
        sCur = Utils.NNE(sCur);
        tCur = Utils.NNE(tCur);
        dateRate = Utils.NNE(dateRate);
        if (sCur.equals(tCur)) {
            Rate rate = new Rate();
            rate.setScurCode(sCur);
            rate.setTcurCode(tCur);
            rate.setDateRate(dateRate);
            rate.setRate(BigDecimal.ONE);
            return rate;
        } else {
            List<Rate> list = jdbc.executeQuery(new RateDataQuery().withParams(sCur, tCur, dateRate));
            if (list.isEmpty()) {
                Rate rate = Utils.first(jdbc.executeQuery(new RateDataQuery().withParams(tCur, sCur, dateRate)));
                rate.setScurCode(sCur);
                rate.setTcurCode(tCur);
                rate.setRate(BigDecimal.ONE.divide(rate.getRate(), 8, BigDecimal.ROUND_HALF_UP));
                return rate;
            } else
                return Utils.first(list);
        }
    }

    @Override
    public List<Rate> rates(Jdbc jdbc, Timestamp dateRate) throws Exception {
        List<Rate> result = new ArrayList<>();
        jdbc.createTrans();
        try {
            List<Map<String, ?>> rows = jdbc.executeQuery(
                    "select s.cur_code as scur_code, t.cur_code as tcur_code from aaa_currency s " +
                            "join aaa_currency t on s.cur_code != t.cur_code");
            for (Map<String, ?> row : rows) {
                result.add(rate(jdbc, Utils.NNE(row.get("SCUR_CODE")), Utils.NNE(row.get("TCUR_CODE")), Utils.NNE(dateRate)));
            }
            jdbc.commitTrans();
        } catch (Exception e) {
            jdbc.rollbackTrans();
            e.printStackTrace();
            throw e;
        }
        return result;
    }

    /*BALANCE*/
    private static class BalanceDataQuery implements DataQuery<Balance> {
        private Object[] params;
        private boolean includeDate;

        public BalanceDataQuery withParams(String accNum, Timestamp date, boolean includeDate) {
            this.includeDate = includeDate;
            this.params = new Object[]{Utils.NNE(date), Utils.NNE(accNum)};
            return this;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            this.params = Utils.NNE(params);
            return CommonQuery.instance(
                    "select a.acc_id, a.acc_num, coalesce (b.balance, 0) - coalesce(sum(t.d_amount - t.k_amount), 0) as balance, c.cur_code\n" +
                            "from aaa_account a join aaa_currency c on 1 = 1\n" +
                            "left join aaa_balance b on a.acc_id = b.acc_id and c.cur_code = b.cur_code\n" +
                            "left join aaa_turn t on t.acc_id = b.acc_id and t.cur_code = b.cur_code  and t.turn_date " + (includeDate ? ">" : ">=") + " ?\n" +
                            "where a.acc_num = ?\n" +
                            "group by a.acc_id, a.acc_num, c.cur_code, coalesce (b.balance, 0)", this.params)
                    .createPreparedStatement(connection);
        }

        @Override
        public List<Balance> handle(ResultSet resultSet) throws Exception {
            List<Balance> result = new ArrayList<>();
            while (resultSet.next()) {
                Balance rest = new Balance();
                rest.setAccId(resultSet.getLong("ACC_ID"));
                rest.setAccNum(resultSet.getString("ACC_NUM"));
                rest.setBalance(resultSet.getBigDecimal("BALANCE"));
                rest.setCurCode(resultSet.getString("CUR_CODE"));
                rest.setBalanceDate(Utils.NNE(this.params[0]));
                result.add(rest);
            }
            return result;
        }
    }

    @Override
    public BigDecimal saldo(Jdbc jdbc, String accNum, String curCode) throws Exception {
        return Utils.NNE(Utils.first(jdbc.executeQuery(
                "select coalesce(b.balance, 0) as balance from\n" +
                        "(select cast(?  as varchar(20)) as acc_num, cast(? as varchar(5)) as cur_code) x\n" +
                        "left join aaa_account a on a.acc_num = x.acc_num\n" +
                        "left join aaa_balance b on a.acc_id = b.acc_id and x.cur_code = b.cur_code",
                new Object[]{accNum, curCode})).get("BALANCE"));
    }

    @Override
    public List<Balance> balance(Jdbc jdbc, String accNum, Timestamp date, boolean includeDate) throws Exception {
        return jdbc.executeQuery(new BalanceDataQuery().withParams(accNum, date, includeDate));
    }

    @Override
    public List<Balance> balance(Jdbc jdbc, String accNum, Timestamp date) throws Exception {
        return jdbc.executeQuery(new BalanceDataQuery().withParams(accNum, date, true));
    }

    /*OPERATIONS*/
    private static class ExtractDataQuery implements DataQuery<Extract> {
        private Object[] params;

        public ExtractDataQuery withParams(String accNum, Timestamp startDate, Timestamp stopDate) {
            this.params = new Object[]{Utils.NNE(accNum), Utils.NNE(startDate), Utils.NNE(stopDate)};
            return this;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            this.params = Utils.NNE(params);
            return CommonQuery.instance(
                    "select x.oper_id, x.h_client_id, x.client_id, x.last_name, x.first_name, x.middle_name,\n" +
                            "       x.oper_date, x.oper_type, x.amount, x.acc_id, x.acc_num, x.cur_code, x.turn_date,\n" +
                            "       t.d_amount - t.k_amount as cor_amount, ta.acc_id as cor_acc_id,\n" +
                            "       ta.acc_num as cor_acc_num, t.cur_code as cor_cur_code,\n" +
                            "       t.turn_date as cor_turn_date\n" +
                            "from (\n" +
                            "    select o.oper_id, o.h_client_id, hc.client_id, hc.last_name, hc.first_name, hc.middle_name, o.oper_date, o.oper_type,\n" +
                            "           s.d_amount - s.k_amount as amount, sa.acc_id, sa.acc_num, s.cur_code, s.turn_date\n" +
                            "    from aaa_oper o\n" +
                            "    join aaa_h_client hc on hc.h_client_id = o.h_client_id \n" +
                            "    join aaa_turn s on o.oper_id = s.oper_id and o.oper_acc_id = s.acc_id and o.oper_cur_code = s.cur_code\n" +
                            "    join aaa_account sa on sa.acc_id = s.acc_id\n" +
                            "    where sa.acc_num = ? and o.oper_date between ? and ? ) x\n" +
                            "left join aaa_turn t on x.oper_id = t.oper_id and (x.acc_id != t.acc_id or x.cur_code != t.cur_code )\n" +
                            "left join aaa_account ta on t.acc_id = ta.acc_id\n", this.params)
                    .createPreparedStatement(connection);
        }

        @Override
        public List<Extract> handle(ResultSet resultSet) throws Exception {
            List<Extract> result = new ArrayList<>();
            while (resultSet.next()) {
                Extract extract = new Extract();
                extract.setOperId(resultSet.getLong("OPER_ID"));
                extract.setOperDate(resultSet.getTimestamp("OPER_DATE"));
                extract.setOperType(resultSet.getString("OPER_TYPE"));
                extract.setHClientId(resultSet.getLong("H_CLIENT_ID"));
                extract.setClientId(resultSet.getLong("CLIENT_ID"));
                extract.setLastName(resultSet.getString("LAST_NAME"));
                extract.setFirstName(resultSet.getString("FIRST_NAME"));
                extract.setMiddleName(resultSet.getString("MIDDLE_NAME"));
                extract.setAccId(resultSet.getLong("ACC_ID"));
                extract.setAccNum(resultSet.getString("ACC_NUM"));
                extract.setCurCode(resultSet.getString("CUR_CODE"));
                extract.setAmount(resultSet.getBigDecimal("AMOUNT"));
                extract.setCorAccId(resultSet.getLong("COR_ACC_ID"));
                extract.setCorAccNum(resultSet.getString("COR_ACC_NUM"));
                extract.setCorCurCode(resultSet.getString("COR_CUR_CODE"));
                extract.setCorAmount(resultSet.getBigDecimal("COR_AMOUNT"));
                extract.setCorTurnDate(resultSet.getTimestamp("COR_TURN_DATE"));
                result.add(extract);
            }
            return result;
        }
    }

    @Override
    public List<Extract> extracts(Jdbc jdbc, String accNum, Timestamp startDate, Timestamp stopDate) throws Exception {
        return jdbc.executeQuery(new ExtractDataQuery().withParams(accNum, startDate, stopDate));
    }
}
