package ru.transfer.service;

import ru.transfer.model.*;

/**
 *
 */
public interface OperationService {
    Client addClient(Client client) throws Exception;
    Account addAccount(Account account) throws Exception;
    Extracts call (ComplexOper complexOper) throws Exception;
}
