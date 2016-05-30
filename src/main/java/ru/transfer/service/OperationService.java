package ru.transfer.service;

import ru.transfer.model.Account;
import ru.transfer.model.Client;
import ru.transfer.model.Rate;

/**
 *
 */
public interface OperationService {
    Client addClient(Client client) throws Exception;
    Account addAccount(Account account) throws Exception;
    Rate addRate (Rate rate) throws Exception;
}
