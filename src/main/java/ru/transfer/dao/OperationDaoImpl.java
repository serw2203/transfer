package ru.transfer.dao;

import ru.transfer.expt.TransferAppException;
import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;
import ru.transfer.util.Utils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

/**
 *
 */
public class OperationDaoImpl implements OperationDao {

    private final AnalyticalDao analyticalDao = new AnalyticalDaoImpl();

    private Long sequence(Jdbc jdbc) throws Exception {
        return Utils.NNE(Utils.first(jdbc.executeQuery("select seq_id.nextval as id")).get("ID"));
    }

    /**
     * @param jdbc
     * @param operDate
     * @param operType
     * @param acc
     * @param cur
     * @param corAcc
     * @param corCur
     * @param corAmount
     * @return
     * @throws Exception
     */
    private Extract operation(Jdbc jdbc, Timestamp operDate, String operType, String acc, String cur,
                              String corAcc, String corCur, BigDecimal corAmount) throws Exception {
        Map<String, ?> map = Utils.first(jdbc.executeQuery(
                "select top 1 " +
                        "hc.h_client_id, a.client_id, a.acc_id, ta.acc_id as cor_acc_id, hc.last_name, hc.first_name, hc.middle_name from aaa_account a\n" +
                        "join aaa_h_client hc on hc.client_id = a.client_id and a.acc_num = ?\n" +
                        "left join aaa_account ta on ta.acc_num = ?\n" +
                        "where hc.modify_date <= ?\n" +
                        "order by hc.modify_date desc", new Object[]{acc, corAcc, operDate}));
        Extract ext = new Extract();
        ext.setOperId(sequence(jdbc));
        ext.setHClientId(Utils.NNE(map.get("H_CLIENT_ID")));
        ext.setClientId(Utils.NNE(map.get("CLIENT_ID")));
        ext.setLastName(Utils.NNE(map.get("LAST_NAME")));
        ext.setFirstName(map.get("FIRST_NAME") == null ? null : map.get("FIRST_NAME").toString());
        ext.setMiddleName(map.get("MIDDLE_NAME") == null ? null : map.get("MIDDLE_NAME").toString());
        ext.setOperType(operType);
        ext.setOperDate(operDate);
        Rate rate = analyticalDao.rate(jdbc, corCur, cur, operDate);
        ext.setAmount(corAmount.multiply(rate.getRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
        ext.setAccId(Utils.NNE(map.get("ACC_ID")));
        ext.setAccNum(acc);
        ext.setCurCode(cur);
        ext.setTurnDate(operDate);
        Object corAccId = map.get("COR_ACC_ID");
        ext.setCorAccId(corAccId == null ? null : (Long) corAccId);
        ext.setCorAccNum(corAcc);
        ext.setCorAmount(corAmount);
        ext.setCorCurCode(corCur);
        ext.setCorTurnDate(operDate);
        if (jdbc.executeUpdate(
                "insert into aaa_oper (oper_id, h_client_id, oper_date, oper_type, oper_acc_id, oper_cur_code)\n" +
                        "values (?, ?, ?, ?, ?, ?)", new Object[]{
                        ext.getOperId(),
                        ext.getHClientId(),
                        ext.getOperDate(),
                        ext.getOperType(),
                        ext.getAccId(),
                        ext.getCurCode()}) != 1) throw new RuntimeException("Insert operation failed");
        return ext;
    }

    /**
     * @param jdbc
     * @param operId
     * @param accId
     * @param cur
     * @param dAmount
     * @param kAmount
     * @param turnDate
     * @throws Exception
     */
    private void turn(Jdbc jdbc, Long operId, Long accId, String cur, BigDecimal dAmount,
                      BigDecimal kAmount, Timestamp turnDate) throws Exception {
        if (jdbc.executeUpdate(
                "insert into aaa_turn (oper_id, acc_id, cur_code, d_amount, k_amount, turn_date)\n" +
                        "values (?, ?, ?, ?, ?, ?)", new Object[]{
                        operId,
                        accId,
                        cur,
                        dAmount,
                        kAmount,
                        turnDate}) != 1) throw new RuntimeException("Insert turns failed");
    }

    /**
     * @param jdbc
     * @param debitAccId
     * @param debitCur
     * @param debitAmount
     * @param creditAccId
     * @param creditAccNum
     * @param creditCur
     * @param creditAmount
     * @throws Exception
     */
    private void balance(Jdbc jdbc, Long debitAccId, String debitCur, BigDecimal debitAmount,
                         Long creditAccId, String creditAccNum, String creditCur, BigDecimal creditAmount) throws Exception {
        if (creditAccId != null) {
            BigDecimal balance = Utils.NNE(Utils.first(jdbc.executeQuery(
                    "select coalesce(b.balance, 0) as balance from (select cast(?  as bigint) as acc_id, cast(? as varchar(5)) as cur_code) x \n" +
                            "left join aaa_balance b on x.acc_id = b.acc_id and x.cur_code = b.cur_code",
                    new Object[]{creditAccId, creditCur})).get("BALANCE"));
            if (creditAmount.compareTo(balance) > 0) {
                throw new TransferAppException(String.format(
                        "Insufficient funds in the account %s to be debited", creditAccNum));
            }
        }

        if (jdbc.executeUpdate("merge into aaa_balance key (acc_id, cur_code) \n" +
                        "select x.acc_id, x.cur_code, coalesce(b.balance,0) + x.amount from \n" +
                        "       (select cast(? as bigint) as acc_id, cast(? as varchar(5)) as cur_code, cast(? as decimal(18, 2)) as amount from dual union \n" +
                        "        select cast(? as bigint) as acc_id, cast(? as varchar(5)) as cur_code, (-1)* cast(? as decimal(18, 2)) as amount) x \n" +
                        "left join aaa_balance b on b.acc_id = x.acc_id and b.cur_code = x.cur_code \n" +
                        "where x.acc_id != 0",
                new Object[]{
                        debitAccId,
                        debitCur,
                        debitAmount,
                        creditAccId,
                        creditCur,
                        creditAmount}) < 1) throw new RuntimeException("Merge balances failed");
    }

    @Override
    public Extract input(Jdbc jdbc, InputOperation operation) throws Exception {
        if (jdbc.hasTrans()) {
            Extract extr = operation(jdbc, operation.getInputDate(), "INPUT", operation.getInputAccount(), operation.getInputCurrency(),
                    null, operation.getInputCurrency(), operation.getInputAmount());
            turn(jdbc, extr.getOperId(), extr.getAccId(), extr.getCurCode(), extr.getAmount(), BigDecimal.ZERO, operation.getInputDate());
            balance(jdbc, extr.getAccId(), extr.getCurCode(), extr.getAmount(),
                    extr.getCorAccId(), extr.getCorAccNum(), extr.getCorCurCode(), extr.getCorAmount());
            return extr;
        }
        throw new IllegalStateException("Transaction not found");
    }

    @Override
    public Extract output(Jdbc jdbc, OutputOperation operation) throws Exception {
        if (jdbc.hasTrans()) {
            Extract extr = operation(jdbc, operation.getOutputDate(), "OUTPUT", operation.getOutputAccount(), operation.getOutputCurrency(),
                    null, operation.getOutputCurrency(), operation.getOutputAmount());
            turn(jdbc, extr.getOperId(), extr.getAccId(), extr.getCurCode(), BigDecimal.ZERO, extr.getAmount(), operation.getOutputDate());
            balance(jdbc, extr.getCorAccId(), extr.getCorCurCode(), extr.getCorAmount(),
                    extr.getAccId(), extr.getAccNum(), extr.getCurCode(), extr.getAmount());
            return extr;
        }
        throw new IllegalStateException("Transaction not found");
    }

    @Override
    public Extract transfer(Jdbc jdbc, TransferOperation operation) throws Exception {
        if (jdbc.hasTrans()) {
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
                            Utils.NNE(client.getLastName()),
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
                        account.getClientId(),
                        account.getAccNum()}) != 1) {
            throw new RuntimeException("Account insert failed");
        }
        return account;
    }
}
