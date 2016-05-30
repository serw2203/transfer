package ru.transfer.query.impl;

import ru.transfer.query.CommonQuery;
import ru.transfer.util.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public class AccountByNumber extends CommonAccount {
    private String[] params;

    public AccountByNumber withParams(String accNum) {
        this.params = new String[]{Utils.NNE(accNum)};
        return this;
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        this.params = Utils.NNE(params);
        return CommonQuery.instance(
                "select acc_id, client_id, acc_num from aaa_account where acc_num = ?", this.params)
                .createPreparedStatement(connection);
    }
}
