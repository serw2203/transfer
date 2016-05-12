package ru.transfer.dao;

import ru.transfer.model.Account;
import ru.transfer.model.Client;
import ru.transfer.query.impl.AccountsByClientDataQuery;
import ru.transfer.query.impl.ClientDataQuery;
import ru.transfer.query.impl.CurrencyDataQuery;
import ru.transfer.helper.JdbcHelper;
import ru.transfer.model.Currency;

import java.math.BigInteger;
import java.util.List;

/**
 *
 */
public class AnalyticalDao implements IAnalyticalDao {

    @Override
    public List<Currency> currencies() throws Exception {
        JdbcHelper<Currency> currencyHelper = new JdbcHelper<>();
        return currencyHelper.executeQuery(new CurrencyDataQuery());
    }

    @Override
    public Client client(BigInteger clientId) throws Exception {
        JdbcHelper<Client> clientHelper = new JdbcHelper<>();
        ClientDataQuery query = new ClientDataQuery();
        query.setClientId(clientId);
        List<Client> clients = clientHelper.executeQuery(query);
        if (clients.size() == 1) {
            return clients.get(0);
        } else {
            throw new RuntimeException(String.format("Invalid search client : found %d elements", clients.size()) );
        }
    }

    @Override
    public List<Account> accounts(BigInteger clientId) throws Exception {
        JdbcHelper<Account> accountHelper = new JdbcHelper<>();
        AccountsByClientDataQuery query = new AccountsByClientDataQuery();
        query.setClientId(clientId);
        List<Account> accounts = accountHelper.executeQuery(query);
        if (!accounts.isEmpty()) {
            return accounts;
        } else {
            throw new RuntimeException(String.format("Not found accounts by clientId = %d", clientId) );
        }
    }
}
