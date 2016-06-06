package ru.transfer.dao;

import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;

import java.math.BigDecimal;

/**
 *
 */
public interface OperationDao {
    Client addClient(Jdbc jdbc, Client client) throws Exception;
    Account addAccount(Jdbc jdbc, Account account) throws Exception;
    Extract call (Jdbc jdbc, Operation operation) throws Exception;
}
