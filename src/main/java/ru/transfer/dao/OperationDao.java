package ru.transfer.dao;

import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;

/**
 *
 */
public interface OperationDao {
    Client addClient(Jdbc jdbc, Client client) throws Exception;
    Account addAccount(Jdbc jdbc, Account account) throws Exception;
    Extract input(Jdbc jdbc, InputOperation operation) throws Exception;
    Extract output(Jdbc jdbc, OutputOperation operation) throws Exception;
    Extract transfer(Jdbc jdbc, TransferOperation operation) throws Exception;
}
