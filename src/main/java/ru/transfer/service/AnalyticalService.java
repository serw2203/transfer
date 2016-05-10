package ru.transfer.service;

import ru.transfer.dao.AnalyticalDao;
import ru.transfer.model.Client;
import ru.transfer.model.CurrencyRoot;

/**
 *
 */
public class AnalyticalService {

    private AnalyticalDao analyticalDao = new AnalyticalDao();

    public CurrencyRoot currencies () throws Exception {
        CurrencyRoot root = new CurrencyRoot();
        root.getCurrencies().addAll(this.analyticalDao.currencies());
        return root;
    }

    public Client client(Integer clientId) throws Exception {
       return this.analyticalDao.client(clientId);
    }
}
