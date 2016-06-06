package ru.transfer.service;

import ru.transfer.dao.OperationDaoImpl;
import ru.transfer.dao.OperationDao;
import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;

import java.sql.Timestamp;
import java.util.*;

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

    private static class OperationDescriptor {
        private String accNum;
        private String curCode;
        private Timestamp operDate;
        private boolean needCheck;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OperationDescriptor that = (OperationDescriptor) o;
            return Objects.equals(accNum, that.accNum) &&
                    Objects.equals(curCode, that.curCode) &&
                    Objects.equals(operDate, that.operDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accNum, curCode, operDate);
        }
    }

    @Override
    public Extracts call(ComplexOper complexOper) throws Exception {
        Extracts result = new Extracts();
        Jdbc jdbc = new Jdbc();
        jdbc.createTrans();
        try {
            //todo: call
            jdbc.commitTrans();
            return result;
        } catch (Exception e) {
            jdbc.rollbackTrans();
            e.printStackTrace();
            throw e;
        }
    }

    private Extract call(Jdbc jdbc, Operation oper) throws Exception {
        if (oper instanceof InputOperation) {
            return operationDao.input(jdbc, (InputOperation) oper);
        } else if (oper instanceof OutputOperation) {
            return operationDao.output(jdbc, (OutputOperation) oper);
        } else if (oper instanceof TransferOperation) {
            return operationDao.transfer(jdbc, (TransferOperation) oper);
        } else {
            throw new IllegalArgumentException("Unknown operation");
        }
    }
}
