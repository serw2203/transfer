package ru.transfer.dao;

import ru.transfer.model.Client;
import ru.transfer.query.impl.ClientQuery;
import ru.transfer.query.impl.CurrencyQuery;
import ru.transfer.helper.JdbcHelper;
import ru.transfer.model.Currency;

import java.util.List;

/**
 *
 */
public class AnalyticalDao {

    public List<Currency> currencies() throws Exception {
        JdbcHelper currencyHelper = new JdbcHelper<>();
        CurrencyQuery query = new CurrencyQuery();
        return currencyHelper.executeQuery(query);
    }

    public Client client(Integer clientId) throws Exception {
        JdbcHelper clientHelper = new JdbcHelper<>();
        ClientQuery query = new ClientQuery();
        query.setParams(new Integer[]{clientId});
        List<Client> clients = clientHelper.executeQuery(query);
        if (clients.size() == 1) {
            return clients.get(0);
        } else {
            throw new RuntimeException(String.format("Invalid client search : found %s", clients.size()) );
        }
    }
}
