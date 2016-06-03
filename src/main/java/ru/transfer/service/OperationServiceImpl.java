package ru.transfer.service;

import ru.transfer.dao.OperationDao;
import ru.transfer.dao.OperationDaoImpl;
import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;

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

    public Extracts addOpers (ComplexOper complexOper) throws Exception {
        Extracts result = new Extracts();
        Jdbc jdbc = new Jdbc();
        jdbc.createTrans();
        try {
            for (Operation oper : complexOper.getOperations()) {
                if (oper instanceof InputOperation) {
                    result.getExtracts().add(operationDao.input(jdbc, (InputOperation) oper));
                } else
                if (oper instanceof OutputOperation) {
                    result.getExtracts().add(operationDao.output(jdbc, (OutputOperation) oper));
                } else
                if (oper instanceof TransferOperation) {
                        result.getExtracts().add(operationDao.transfer(jdbc, (TransferOperation) oper));
                } else {
                    throw new IllegalArgumentException("Unknown operation");
                }
            }
            jdbc.commitTrans();
            return result;
        } catch (Exception e) {
            jdbc.rollbackTrans();
            e.printStackTrace();
            throw e;
        }
    }
}
