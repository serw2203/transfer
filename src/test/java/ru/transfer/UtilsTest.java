package ru.transfer;

import org.testng.Assert;
import org.testng.annotations.Test;
import ru.transfer.expt.TransferAppException;
import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;
import ru.transfer.util.Utils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;


/**
 *
 */
public class UtilsTest extends Assert {

    @Test
    public void checkArrayToString() {
        Object[] array = new Object[]{1, 2, null, "Hello", null};
        String s = Utils.arrayToString(array);
        System.out.println(s);
        assertEquals(s, "{1, 2, null, Hello, null}");
    }

    @Test
    public void checkFirst() {
        List l = new ArrayList();
        try {
            Utils.first(l);
        } catch (IllegalStateException ise) {
            assertEquals("Invalid result : found 0 elements", ise.getMessage());
        }
        Object o = new Object();
        assertTrue(l.add(o));
        assertEquals(o, Utils.first(l));
    }

    private InputOperation io() {
        InputOperation o = new InputOperation();
        o.setAccount("A1");
        o.setCurrency("RUB");
        o.setOperDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
        o.setAmount(BigDecimal.ONE);
        return o;
    }

    private OutputOperation oo() {
        OutputOperation o = new OutputOperation();
        o.setAccount("A1");
        o.setCurrency("RUB");
        o.setOperDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
        o.setAmount(BigDecimal.ONE);
        return o;
    }

    private Operation od(Operation o, String d) {
        o.setOperDate(Utils.dateTimeToTimestamp(d));
        return o;
    }

    private static class Descriptor implements Comparable<Descriptor> {
        private String accNum;
        private String curCode;
        private long moment;

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

        @Override
        public String toString() {
            return accNum + " : " + curCode + " : " + new Date(moment);
        }

        @Override
        public int compareTo(Descriptor o) {
            return o.moment > this.moment ? -1 : o.moment == this.moment ? 0 : 1;
        }
    }

    private Descriptor desc(Operation o) {
        Descriptor d = new Descriptor();
        d.accNum = o.getAccount();
        d.curCode = o.getCurrency();
        d.moment = o.getOperDate().getTime();
        return d;
    }

    private String printOper(Operation o) {
        return "Operation{" +
                "operDate=" + o.getOperDate() +
                ", currency='" + o.getCurrency() + '\'' +
                ", account='" + o.getAccount() + '\'' +
                ", amount=" + o.getAmount() +
                '}';
    }

    private Extract call(Jdbc jdbc, Operation o) {
        Extract extract = new Extract();
        extract.setOperDate(o.getOperDate());
        extract.setAmount(BigDecimal.ONE);
        extract.setAccNum(o.getAccount());
        extract.setCurCode(o.getCurrency());
        System.out.println("call : " + printOper(o));
        return extract;
    }

    private BigDecimal saldo(Jdbc jdbc, String accNum, String curCode) {
        System.out.println("saldo : " + accNum + " : " + curCode);
        return BigDecimal.ONE;
    }

    @Test
    public void checkIt() {
        ComplexOper complexOper = new ComplexOper();
        complexOper.getOperations().add(od(io(), "2006-01-01T10:00:01+0000"));
        complexOper.getOperations().add(od(oo(), "2006-01-01T10:00:01+0000"));
        complexOper.getOperations().add(od(io(), "2006-01-01T10:00:00+0000"));
        complexOper.getOperations().add(od(oo(), "2006-01-01T10:00:00+0000"));
        complexOper.getOperations().add(od(io(), "2006-01-01T10:00:03+0000"));
        complexOper.getOperations().add(od(oo(), "2006-01-01T10:00:04+0000"));

        Jdbc jdbc = new Jdbc();
        List<Extract> extracts = new ArrayList<>();

        complexOper.getOperations().stream().collect(
                groupingBy(this::desc, TreeMap::new, Collectors.toList())).entrySet().stream()
                .forEach( element -> {
                    if (element.getValue().stream().map(operation -> {
                        extracts.add(call(jdbc, operation));
                        return operation instanceof CheckedOperation;
                    }).reduce(false, (accumulator, value) -> accumulator || value)) {
                        if (saldo(jdbc, element.getKey().accNum, element.getKey().curCode).compareTo(BigDecimal.ZERO) < 0) {
                            throw new TransferAppException(
                                    String.format("Insufficient funds in the account '%s' ('%s')"
                                            , element.getKey().accNum
                                            , element.getKey().curCode));
                        }
                    }
                });

        assert true;
    }
}
