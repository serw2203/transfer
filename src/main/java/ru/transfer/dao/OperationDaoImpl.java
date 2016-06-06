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

    private final static String INPUT = "INPUT";
    private final static String OUTPUT = "OUTPUT";
    private final static String TRANSFER = "TRANSFER";

    private final AnalyticalDao analyticalDao = new AnalyticalDaoImpl();

    private Long sequence(Jdbc jdbc) throws Exception {
        return (Long) Utils.first(jdbc.executeQuery("select seq_id.nextval as id")).get("ID");
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

    @Override
    public Extract call(Jdbc jdbc, Operation operation) throws Exception {
        if (jdbc.inTrans()) {
            Extract extract = dispatch(jdbc, operation);
            Turn turn = turnFromExtract(extract);
            turn(jdbc,  turn);
            mergeBalance(jdbc, turn);
            return extract;
        }
        throw new IllegalStateException("Transaction not found");
    }

    private Extract dispatch (Jdbc jdbc, Operation operation) throws Exception {
        if (operation instanceof InputOperation) {
            return input(jdbc, (InputOperation) operation);
        } else
        if (operation instanceof OutputOperation) {
            return output(jdbc, (OutputOperation) operation);
        } else
        if (operation instanceof TransferOperation) {
            return transfer(jdbc, (TransferOperation) operation);
        } else
            throw new RuntimeException("Unknown operation type");
    }

    private Extract input(Jdbc jdbc, InputOperation operation) throws Exception {
        return operation(jdbc, operation.getOperDate(), INPUT,
                operation.getAccount(), operation.getCurrency(), operation.getAmount(),
                null, null);
    }

    private Extract output(Jdbc jdbc, OutputOperation operation) throws Exception {
        return operation(jdbc, operation.getOperDate(), OUTPUT,
                operation.getAccount(), operation.getCurrency(), operation.getAmount(),
                null, null);
    }

    private Extract transfer(Jdbc jdbc, TransferOperation operation) throws Exception {
        return operation(jdbc, operation.getOperDate(), TRANSFER,
                operation.getAccount(), operation.getCurrency(), operation.getAmount(),
                operation.getDestAccount(), operation.getDestCurrency());
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

    private static class Turn {
        private Long operId;
        private Long debitAccId;
        private String debitCur;
        private BigDecimal debitAmount;
        private Long creditAccId;
        private String creditCur;
        private BigDecimal creditAmount;
        private Timestamp turnDate;
    }
    
    private void turn(Jdbc jdbc, Turn turn) throws Exception {
        if (turn.debitAccId.longValue() != 0L) {
            if (jdbc.executeUpdate(new TurnUpdateQuery().withParams(turn.operId,
                    turn.debitAccId,
                    turn.debitCur,
                    turn.debitAmount,
                    BigDecimal.ZERO,
                    turn.turnDate)) != 1) throw new RuntimeException("Insert turns failed");
        }
        if (turn.creditAccId.longValue() != 0L) {
            if (jdbc.executeUpdate(new TurnUpdateQuery().withParams(turn.operId,
                    turn.creditAccId,
                    turn.creditCur,
                    BigDecimal.ZERO,
                    turn.creditAmount,
                    turn.turnDate)) != 1) throw new RuntimeException("Insert turns failed");
        }
    }

    private Turn turnFromExtract (Extract extract) {
        Turn turn = new Turn();
        turn.operId = extract.getOperId();
        turn.turnDate = extract.getOperDate();
        if (INPUT.equals(extract.getOperType())) {
            turn.debitAccId = extract.getAccId();
            turn.debitCur = extract.getCurCode();
            turn.debitAmount = extract.getAmount();
            turn.creditAccId = 0L;
        } else
        if (OUTPUT.equals(extract.getOperType())) {
            turn.debitAccId=0L;
            turn.creditAccId=extract.getAccId();
            turn.creditCur=extract.getCurCode();
            turn.creditAmount=extract.getAmount();
        } else
        if (TRANSFER.equals(extract.getOperType())) {
            turn.debitAccId = extract.getCorAccId();
            turn.debitCur = extract.getCorCurCode();
            turn.debitAmount = extract.getCorAmount();
            turn.creditAccId = extract.getAccId();
            turn.creditCur = extract.getCurCode();
            turn.creditAmount = extract.getAmount();
        } else
            throw new RuntimeException(String.format("Unknown operation type - %s", extract.getOperType()));
        return turn;
    }

    private void mergeBalance(Jdbc jdbc, Turn turn) throws Exception {
        if (jdbc.executeUpdate(
                "merge into aaa_balance key (acc_id, cur_code) \n" +
                        "select x.acc_id, x.cur_code, coalesce(b.balance,0) + x.amount from \n" +
                        "       (select cast(? as bigint) as acc_id, cast(? as varchar(5)) as cur_code, cast(? as decimal(18, 2)) as amount from dual union \n" +
                        "        select cast(? as bigint) as acc_id, cast(? as varchar(5)) as cur_code, (-1)* cast(? as decimal(18, 2)) as amount) x \n" +
                        "left join aaa_balance b on b.acc_id = x.acc_id and b.cur_code = x.cur_code \n" +
                        "where x.acc_id != 0",
                new Object[]{
                        turn.debitAccId,
                        turn.debitCur,
                        turn.debitAmount,
                        turn.creditAccId,
                        turn.creditCur,
                        turn.creditAmount}) < 1) throw new RuntimeException("Merge balances failed");
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
