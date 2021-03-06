package ru.transfer.service;

import ru.transfer.dao.AnalyticalDao;
import ru.transfer.dao.AnalyticalDaoImpl;
import ru.transfer.dao.OperationDao;
import ru.transfer.dao.OperationDaoImpl;
import ru.transfer.expt.TransferAppException;
import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;

import java.math.BigDecimal;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 *
 */
public class OperationServiceImpl implements OperationService {

    private OperationDao operationDao = new OperationDaoImpl();
    private AnalyticalDao analyticalDao = new AnalyticalDaoImpl();

    @Override
    public Client addClient(Client client) throws Exception {
        return operationDao.addClient(new Jdbc(), client);
    }

    @Override
    public Account addAccount(Account account) throws Exception {
        return operationDao.addAccount(new Jdbc(), account);
    }

    private static class Descriptor implements Comparable<Descriptor> {
        String accNum;
        String curCode;
        long moment;

        @Override
        public int compareTo(Descriptor o) {
            return o.moment > this.moment ? -1 : o.moment == this.moment ? 0 : 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Descriptor that = (Descriptor) o;
            return moment == that.moment && accNum.equals(that.accNum) && curCode.equals(that.curCode);

        }

        @Override
        public int hashCode() {
            int result = accNum.hashCode();
            result = 31 * result + curCode.hashCode();
            result = 31 * result + (int) (moment ^ (moment >>> 32));
            return result;
        }
    }

    private Descriptor descriptor(Operation operation) {
        Descriptor descriptor = new Descriptor();
        descriptor.accNum = operation.getAccount();
        descriptor.curCode = operation.getCurrency();
        descriptor.moment = operation.getOperDate().getTime();
        return descriptor;
    }

    @Override
    public Extract call(Operation operation) throws Exception {
        return operationDao.call(new Jdbc(), operation);
    }

    @Override
    public Extracts call(ComplexOper complexOper) throws Exception {
        Extracts result = new Extracts();
        Jdbc jdbc = new Jdbc();
        jdbc.createTrans();
        try {
            complexOper.getOperations().stream().collect(
                    //group operations by descriptor
                    groupingBy(this::descriptor, TreeMap::new, Collectors.toList())).entrySet().stream()
                    .forEach(element -> {
                        //need check saldo ?
                        if (/*if-->*/element.getValue().stream().map(operation -> {
                            try {
                                //call operation
                                result.getExtracts().add(operationDao.call(jdbc, operation));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return operation instanceof CheckedOperation;
                        }).reduce( false, (accumulator, value) -> accumulator || value) /*<--if*/) {
                            try {
                                //check saldo
                                if (analyticalDao.saldo(jdbc, element.getKey().accNum, element.getKey().curCode)
                                        .compareTo(BigDecimal.ZERO) < 0) {
                                    throw new TransferAppException(
                                            String.format("Insufficient funds in the account '%s' ('%s')"
                                                    , element.getKey().accNum
                                                    , element.getKey().curCode));
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
            jdbc.commitTrans();
            return result;
        } catch (Exception e) {
            jdbc.rollbackTrans();
            throw e;
        }
    }
}
