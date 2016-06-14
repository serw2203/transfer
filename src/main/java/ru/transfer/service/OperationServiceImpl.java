package ru.transfer.service;

import ru.transfer.dao.AnalyticalDao;
import ru.transfer.dao.AnalyticalDaoImpl;
import ru.transfer.dao.OperationDaoImpl;
import ru.transfer.dao.OperationDao;
import ru.transfer.expt.TransferAppException;
import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;
import ru.transfer.util.Utils;

import java.math.BigDecimal;
import java.util.*;

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

    private static class Descriptor {
        String accNum;
        String curCode;
        long operDate;
        boolean needCheck;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Descriptor that = (Descriptor) o;
            return operDate == that.operDate && accNum.equals(that.accNum) && curCode.equals(that.curCode);

        }

        @Override
        public int hashCode() {
            int result = accNum.hashCode();
            result = 31 * result + curCode.hashCode();
            result = 31 * result + (int) (operDate ^ (operDate >>> 32));
            return result;
        }
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
            //prepare
            Map<Descriptor, List<Operation>> map = smartOrder(complexOper);
            for (Map.Entry<Descriptor, List<Operation>> item : map.entrySet()) {
                //call
                for (Operation operation : item.getValue()) {
                    result.getExtracts().add(operationDao.call(jdbc, operation));
                }
                //check current saldo
                Descriptor descriptor = item.getKey();
                if (descriptor.needCheck) {
                    if (analyticalDao.saldo(jdbc, descriptor.accNum, descriptor.curCode).compareTo(BigDecimal.ZERO) < 0) {
                        throw new TransferAppException(
                                String.format("Insufficient funds in the account '%s'", descriptor.accNum));
                    }
                }
            }
            jdbc.commitTrans();
            return result;
        } catch (Exception e) {
            jdbc.rollbackTrans();
            throw e;
        }
    }

    private Map<Descriptor, List<Operation>> smartOrder(ComplexOper complexOper) {
        Map< Descriptor, List<Operation> > map = new TreeMap<>((Descriptor o1, Descriptor o2)->
                ((o1.operDate > o2.operDate) ? 1 : (o1.operDate == o2.operDate) ? 0 : -1));

        complexOper.getOperations().forEach(operation -> {
            Descriptor descriptor = descriptor(operation);
            List<Operation> list;

            if (map.containsKey(descriptor)) {
                Descriptor key = Utils.valueFrom(descriptor, map.keySet());
                key.needCheck = descriptor.needCheck || key.needCheck;
                list = map.get(descriptor);
            } else {
                list = new ArrayList<>();
            }

            list.add(operation);
            map.put(descriptor, list);
        });
        return map;
    }

    private Descriptor descriptor(Operation operation) {
        Descriptor descriptor = new Descriptor();
        descriptor.needCheck = operation instanceof OutputOperation || operation instanceof TransferOperation;
        descriptor.accNum = operation.getAccount();
        descriptor.curCode = operation.getCurrency();
        descriptor.operDate = operation.getOperDate().getTime();
        return descriptor;
    }

}
