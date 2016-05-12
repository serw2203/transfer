package ru.transfer.dao;

import ru.transfer.model.Account;
import ru.transfer.model.Client;
import ru.transfer.model.Currency;

import java.math.BigInteger;
import java.util.List;

/**
 *
 */
public interface IAnalyticalDao {
    List<Currency> currencies() throws Exception;

    Client client(BigInteger clientId) throws Exception;

    List<Account> accounts(BigInteger clientId) throws Exception;
}
