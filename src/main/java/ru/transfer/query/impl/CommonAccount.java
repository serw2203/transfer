package ru.transfer.query.impl;

import ru.transfer.model.Account;
import ru.transfer.query.DataQuery;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class CommonAccount implements DataQuery<Account> {

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
