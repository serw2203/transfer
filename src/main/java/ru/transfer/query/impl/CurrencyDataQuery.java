package ru.transfer.query.impl;

import ru.transfer.model.Currency;
import ru.transfer.query.DataQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CurrencyDataQuery implements DataQuery<Currency> {
    private static final String SQL = "select cur_code from aaa_currency";

    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return CommonUpdateQuery.instance(SQL).createPreparedStatement(connection);
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
