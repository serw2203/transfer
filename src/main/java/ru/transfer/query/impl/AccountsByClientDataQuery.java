package ru.transfer.query.impl;

import ru.transfer.model.Account;
import ru.transfer.query.DataQuery;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AccountsByClientDataQuery implements DataQuery<Account> {

    private static final String SQL = "select acc_id, client_id, acc_num from aaa_account where client_id = ?";

    private BigInteger clientId;

    public BigInteger getClientId() {
        return clientId;
    }

    public void setClientId(BigInteger clientId) {
        this.clientId = clientId;
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        if (getClientId() != null) {
            Object[] params = new Long[]{this.getClientId().longValue()};
            return CommonUpdateQuery.instance(SQL, params).createPreparedStatement(connection);
        } else throw new RuntimeException("AccountsByClientDataQuery.createPreparedStatement : clientId must to be not null");
    }

    @Override
    public List<Account> handle(ResultSet resultSet) throws Exception {
        List<Account> result = new ArrayList<>();
        while (resultSet.next()) {
            Account account = new Account();
            account.setAccId(BigInteger.valueOf(resultSet.getLong("ACC_ID")));
            account.setClientId(BigInteger.valueOf(resultSet.getLong("CLIENT_ID")));
            account.setAccNum(resultSet.getString("ACC_NUM"));
            result.add(account);
        }
        return result;
    }


}
