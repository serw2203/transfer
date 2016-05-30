package ru.transfer.service;

import ru.transfer.dao.OperationDao;
import ru.transfer.dao.OperationDaoImpl;
import ru.transfer.helper.Jdbc;
import ru.transfer.model.Account;
import ru.transfer.model.Client;
import ru.transfer.model.Rate;

/**
 *
 */
public class OperationServiceImpl implements OperationService {

    private OperationDao operationDao = new OperationDaoImpl();

    @Override
    public Client addClient(Client client) throws Exception {
        return operationDao.addClient(new Jdbc(), client);
    }

    @Override
    public Account addAccount(Account account) throws Exception {
        return operationDao.addAccount(new Jdbc(), account);
    }

    @Override
    public Rate addRate(Rate rate) throws Exception {
        return operationDao.addRate(new Jdbc(), rate);
    }
}
