package ru.transfer.service;

import ru.transfer.dao.AnalyticalDao;
import ru.transfer.dao.IAnalyticalDao;
import ru.transfer.model.AccountRoot;
import ru.transfer.model.Client;
import ru.transfer.model.CurrencyRoot;

import java.math.BigInteger;

/**
 *
 */
public class AnalyticalService implements IAnalyticalService {

    private final IAnalyticalDao analyticalDao = new AnalyticalDao();

    @Override
    public CurrencyRoot currencies() throws Exception {
        CurrencyRoot root = new CurrencyRoot();
        root.getCurrencies().addAll(this.analyticalDao.currencies());
        return root;
    }

    @Override
    public Client client(BigInteger clientId) throws Exception {
       return this.analyticalDao.client(clientId);
    }

    @Override
    public AccountRoot accounts(BigInteger clientId) throws Exception {
        AccountRoot root = new AccountRoot();
        root.getAccounts().addAll(this.analyticalDao.accounts(clientId));
        return root;
    }
}
