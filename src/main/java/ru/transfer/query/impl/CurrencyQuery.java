package ru.transfer.query.impl;

import ru.transfer.model.Currency;
import ru.transfer.query.AbstractExecute;
import ru.transfer.query.Query;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CurrencyQuery extends AbstractExecute implements Query<Currency> {
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

    @Override
    public String sql() {
        return "select cur_code from aaa_currency";
    }
}
