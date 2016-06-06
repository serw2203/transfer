package ru.transfer.dao;

import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;
import ru.transfer.query.CommonQuery;
import ru.transfer.query.UpdateQuery;
import ru.transfer.util.Utils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

/**
 *
 */
public class OperationDaoImpl implements OperationDao {

    private final AnalyticalDao analyticalDao = new AnalyticalDaoImpl();

    @Override
    public BigDecimal currentSaldo(Jdbc jdbc, Long accId, String curCode) throws Exception {
        return Utils.NNE(Utils.first(jdbc.executeQuery(
                "select coalesce(b.balance, 0) as balance from (select cast(?  as bigint) as acc_id, cast(? as varchar(5)) as cur_code) x \n" +
                        "left join aaa_balance b on x.acc_id = b.acc_id and x.cur_code = b.cur_code",
                new Object[]{accId, curCode})).get("BALANCE"));
    }

    private Long sequence(Jdbc jdbc) throws Exception {
        return Utils.NNE(Utils.first(jdbc.executeQuery("select seq_id.nextval as id")).get("ID"));
    }

    private Extract operation(Jdbc jdbc,
                              Timestamp operDate, String operType, String accNum, String curCode, BigDecimal amount,
                              String corAccNum, String corCurCode) throws Exception {

        Map<String, ?> map = Utils.first(jdbc.executeQuery(
                "select top 1 hc.h_client_id, a.client_id, a.acc_id, ta.acc_id as cor_acc_id, " +
                        "hc.last_name, hc.first_name, hc.middle_name from aaa_account a\n" +
                        "join aaa_h_client hc on hc.client_id = a.client_id and a.acc_num = ?\n" +
                        "left join aaa_account ta on ta.acc_num = ?\n" +
                        "where hc.modify_date <= ?\n" +
                        "order by hc.modify_date desc", new Object[]{accNum, corAccNum, operDate}));

        Extract extract = new Extract();
        extract.setOperId(sequence(jdbc));
        extract.setHClientId(Utils.NNE(map.get("H_CLIENT_ID"), "Unknown client history"));
        extract.setClientId(Utils.NNE(map.get("CLIENT_ID"), "Unknown client"));
        extract.setLastName(Utils.NNE(map.get("LAST_NAME"), "Last name must  to be not null"));
        extract.setFirstName(map.get("FIRST_NAME") == null ? null : map.get("FIRST_NAME").toString());
        extract.setMiddleName(map.get("MIDDLE_NAME") == null ? null : map.get("MIDDLE_NAME").toString());
        extract.setOperType(Utils.NNE(operType, "Operation type must to be not null"));
        extract.setOperDate(Utils.NNE(operDate, "Operation date  to be not null"));

        extract.setAmount(Utils.NNE(amount, "Amount must to be not null"));
        extract.setAccId(Utils.NNE(map.get("ACC_ID"), "Unknown account"));
        extract.setAccNum(Utils.NNE(accNum, "Account must to be not null"));
        extract.setCurCode(Utils.NNE(curCode, "Currency code must to be not null"));
        extract.setTurnDate(Utils.NNE(operDate));

        Object corAccId = map.get("COR_ACC_ID");
        if (corAccId != null) {
            Rate rate = analyticalDao.rate(jdbc, curCode, Utils.NNE(corCurCode, "Mail currency code must to be not null"), operDate);
            extract.setCorAccId((Long) corAccId);
            extract.setCorAccNum(corAccNum);
            extract.setCorAmount(amount.multiply(rate.getRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
            extract.setCorCurCode(corCurCode);
            extract.setCorTurnDate(operDate);
        } else {
            extract.setCorAccId(0L);
        }

        defineTurn(extract);

        if (jdbc.executeUpdate(
                "insert into aaa_oper (oper_id, h_client_id, oper_date, oper_type, oper_acc_id, oper_cur_code)\n" +
                        "values (?, ?, ?, ?, ?, ?)", new Object[]{
                        extract.getOperId(),
                        extract.getHClientId(),
                        extract.getOperDate(),
                        extract.getOperType(),
                        extract.getAccId(),
                        extract.getCurCode()}) != 1) throw new RuntimeException("Insert operation failed");
        return extract;
    }

    private void defineTurn(Extract extract) {
        Turn turn = new Turn();
        if ("INPUT".equals(extract.getOperType())) {
            turn.setDebitAccId(extract.getAccId());
            turn.setDebitCur(extract.getCurCode());
            turn.setDebitAmount(extract.getAmount());
            turn.setCreditAccId(extract.getCorAccId());
            turn.setCreditCur(extract.getCorCurCode());
            turn.setCreditAmount(extract.getCorAmount());
        } else
        if ("OUTPUT".equals(extract.getOperType())) {
            turn.setDebitAccId(extract.getCorAccId());
            turn.setDebitCur(extract.getCorCurCode());
            turn.setDebitAmount(extract.getCorAmount());
            turn.setCreditAccId(extract.getAccId());
            turn.setCreditCur(extract.getCurCode());
            turn.setCreditAmount(extract.getAmount());
        } else
            throw new RuntimeException(String.format("Unknown operation type - %s", extract.getOperType()));
        extract.setTurn(turn);
    }

    private static class TurnUpdateQuery implements UpdateQuery {
        private Object[] params;

        public TurnUpdateQuery withParams(Long operId, Long accId, String curCode,
                                          BigDecimal debitAmount, BigDecimal creditAmount, Timestamp turnDate) {
            this.params = new Object[]{
                    Utils.NNE(operId),
                    Utils.NNE(accId),
                    Utils.NNE(curCode),
                    Utils.NNE(debitAmount),
                    Utils.NNE(creditAmount),
                    Utils.NNE(turnDate)
            };
            return this;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            this.params = Utils.NNE(this.params);
            return CommonQuery.instance(
                    "insert into aaa_turn (oper_id, acc_id, cur_code, d_amount, k_amount, turn_date)\n" +
                            "values (?, ?, ?, ?, ?, ?)", this.params)
                    .createPreparedStatement(connection);

        }
    }

    private void turn(Jdbc jdbc, Extract extract) throws Exception {
        if (extract.getTurn().getDebitAccId().longValue() != 0L) {
            if (jdbc.executeUpdate(new TurnUpdateQuery().withParams(extract.getOperId(),
                    extract.getTurn().getDebitAccId(),
                    extract.getTurn().getDebitCur(),
                    extract.getTurn().getDebitAmount(),
                    BigDecimal.ZERO,
                    extract.getOperDate())) != 1) throw new RuntimeException("Insert turns failed");
        }
        if (extract.getTurn().getCreditAccId().longValue() != 0L) {
            if (jdbc.executeUpdate(new TurnUpdateQuery().withParams(extract.getOperId(),
                    extract.getTurn().getCreditAccId(),
                    extract.getTurn().getCreditCur(),
                    BigDecimal.ZERO,
                    extract.getTurn().getCreditAmount(),
                    extract.getOperDate())) != 1) throw new RuntimeException("Insert turns failed");
        }
    }

    private void mergeBalance(Jdbc jdbc, Extract extract) throws Exception {
        if (jdbc.executeUpdate(
                "merge into aaa_balance key (acc_id, cur_code) \n" +
                        "select x.acc_id, x.cur_code, coalesce(b.balance,0) + x.amount from \n" +
                        "       (select cast(? as bigint) as acc_id, cast(? as varchar(5)) as cur_code, cast(? as decimal(18, 2)) as amount from dual union \n" +
                        "        select cast(? as bigint) as acc_id, cast(? as varchar(5)) as cur_code, (-1)* cast(? as decimal(18, 2)) as amount) x \n" +
                        "left join aaa_balance b on b.acc_id = x.acc_id and b.cur_code = x.cur_code \n" +
                        "where x.acc_id != 0",
                new Object[]{
                        extract.getTurn().getDebitAccId(),
                        extract.getTurn().getDebitCur(),
                        extract.getTurn().getDebitAmount(),
                        extract.getTurn().getCreditAccId(),
                        extract.getTurn().getCreditCur(),
                        extract.getTurn().getCreditAmount()}) < 1) throw new RuntimeException("Merge balances failed");
    }

    @Override
    public Extract input(Jdbc jdbc, InputOperation operation) throws Exception {
        if (jdbc.inTrans()) {
            Extract extract = operation(jdbc,
                    operation.getOperDate(), "INPUT",
                    operation.getAccount(), operation.getCurrency(), operation.getInputAmount(),
                    null, null);
            turn(jdbc, extract);
            mergeBalance(jdbc, extract);
            return extract;
        }
        throw new IllegalStateException("Transaction not found");
    }

    @Override
    public Extract output(Jdbc jdbc, OutputOperation operation) throws Exception {
        if (jdbc.inTrans()) {
            Extract extract = operation(jdbc, operation.getOperDate(), "OUTPUT",
                    operation.getAccount(), operation.getCurrency(), operation.getOutputAmount(),
                    null, null);
            turn(jdbc, extract);
            mergeBalance(jdbc, extract);
            return extract;
        }
        throw new IllegalStateException("Transaction not found");
    }

    @Override
    public Extract transfer(Jdbc jdbc, TransferOperation operation) throws Exception {
        if (jdbc.inTrans()) {
        }
        throw new IllegalStateException("Transaction not found");
    }

    @Override
    public Client addClient(Jdbc jdbc, Client client) throws Exception {
        client.setModifyDate(client.getModifyDate() == null ? new Timestamp(System.currentTimeMillis())
                : client.getModifyDate());
        jdbc.createTrans();
        try {
            client.setClientId(sequence(jdbc));
            if (jdbc.executeUpdate(
                    "insert into aaa_client (client_id) values (?)", new Object[]{client.getClientId()}) != 1) {
                throw new RuntimeException("Client insert failed");
            }
            if (jdbc.executeUpdate(
                    "insert into aaa_h_client (client_id, last_name, first_name, middle_name, modify_date, cli_version)" +
                            "values (?, ?, ?, ?, ?, ?)", new Object[]{
                            client.getClientId(),
                            Utils.NNE(client.getLastName(), "Last name must to be not null"),
                            client.getFirstName(),
                            client.getMiddleName(),
                            client.getModifyDate(),
                            0
                    }) != 1) {
                throw new RuntimeException("Client's history insert failed");
            }
            jdbc.commitTrans();
            return client;
        } catch (Exception e) {
            jdbc.rollbackTrans();
            throw e;
        }
    }

    @Override
    public Account addAccount(Jdbc jdbc, Account account) throws Exception {
        account.setAccId(sequence(jdbc));
        if (jdbc.executeUpdate(
                "insert into aaa_account (acc_id, client_id, acc_num) values (?, ?, ?)",
                new Object[]{
                        account.getAccId(),
                        Utils.NNE(account.getClientId(), "Unknown client"),
                        Utils.NNE(account.getAccNum(), "Account number must to be not null")}) != 1) {
            throw new RuntimeException("Account insert failed");
        }
        return account;
    }
}
