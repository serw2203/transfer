package ru.transfer.service;

import ru.transfer.model.Account;
import ru.transfer.model.AccountRoot;
import ru.transfer.model.Client;
import ru.transfer.model.CurrencyRoot;

import java.math.BigInteger;

/**
 *
 */
public interface IAnalyticalService {
    CurrencyRoot currencies() throws Exception;

    Client client(BigInteger clientId) throws Exception;

    AccountRoot accounts(BigInteger clientId) throws Exception;
}
