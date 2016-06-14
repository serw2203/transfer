package ru.transfer;

import org.junit.Assert;
import org.junit.Test;
import ru.transfer.model.ComplexOper;
import ru.transfer.model.InputOperation;
import ru.transfer.model.Operation;
import ru.transfer.model.OutputOperation;
import ru.transfer.service.OperationServiceImpl;
import ru.transfer.util.Utils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

    @Test
    public void checkValueFrom() {
        String s = "RUB";
        Set set = new HashSet();
        try {
            Utils.valueFrom(s, set);
        } catch (RuntimeException re) {
            assertEquals("Object 'RUB' not found", re.getMessage());
        }
        set.add("USD");
        set.add("EUR");
        set.add("RUB");
        assertEquals(Utils.valueFrom(s, set), s);
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

        @Override
        public String toString() {
            return accNum + " : " + curCode + " : " + operDate;
        }
    }

    private Descriptor desc(Operation o) {
        Descriptor d = new Descriptor();
        d.accNum = o.getAccount();
        d.curCode = o.getCurrency();
        d.operDate = o.getOperDate().getTime();
        return d;
    }

    private static class Workshop {

        private String name;
        private int attendance;

        public Workshop(String name, int attendance) {
            this.name = name;
            this.attendance = attendance;
        }

        public String getName() {
            return name;
        }

        public int getAttendance() {
            return attendance;
        }
    }

    @Test
    public void checkIt() {
        ComplexOper co = new ComplexOper();
        co.getOperations().add(od(io(), "2006-01-01T10:00:01+0000"));
        co.getOperations().add(od(oo(), "2006-01-01T10:00:01+0000"));
        co.getOperations().add(od(io(), "2006-01-01T10:00:00+0000"));
        co.getOperations().add(od(oo(), "2006-01-01T10:00:00+0000"));
        co.getOperations().add(od(io(), "2006-01-01T10:00:03+0000"));
        co.getOperations().add(od(oo(), "2006-01-01T10:00:04+0000"));

        co.getOperations().stream()
                .flatMap((it) -> Stream.of(desc(it))).distinct().forEach(System.out::println);

        Map<Descriptor, List<Operation>> m =
                co.getOperations().stream().collect(Collectors.groupingBy( it -> desc(it) ));

        m.keySet().stream().sorted((Descriptor o1, Descriptor o2)->
               ((o1.operDate > o2.operDate) ? 1 : (o1.operDate == o2.operDate) ? 0 : -1));

        System.out.println(m);
       assert true;
    }
}
