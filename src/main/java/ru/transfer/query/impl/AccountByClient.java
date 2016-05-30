package ru.transfer.query.impl;

import ru.transfer.model.Account;
import ru.transfer.query.CommonQuery;
import ru.transfer.util.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AccountByClient extends CommonAccount {
    private Long[] params;

    public AccountByClient withParams(Long clientId) {
        this.params = new Long[]{Utils.NNE(clientId)};
        return this;
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        this.params = Utils.NNE(params);
        return CommonQuery.instance(
                "select acc_id, client_id, acc_num from aaa_account where client_id = ?", this.params)
                .createPreparedStatement(connection);
    }

    @Override
    public List<Account> handle(ResultSet resultSet) throws Exception {
        List<Account> result = new ArrayList<>();
        while (resultSet.next()) {
            Account account = new Account();
            account.setAccId(resultSet.getLong("ACC_ID"));
            account.setClientId(resultSet.getLong("CLIENT_ID"));
            account.setAccNum(resultSet.getString("ACC_NUM"));
            result.add(account);
        }
        return result;
    }
}
