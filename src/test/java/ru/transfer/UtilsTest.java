package ru.transfer;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.module.SimpleModule;
import org.testng.Assert;
import org.testng.annotations.Test;
import ru.transfer.conf.Config;
import ru.transfer.expt.TransferAppException;
import ru.transfer.helper.Jdbc;
import ru.transfer.model.*;
import ru.transfer.util.Utils;

import java.io.IOException;
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
        assertNotEquals(s, "{1, 2, null, Hello, not null}");
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

    private <O extends Operation> O io() {
        InputOperation o = new InputOperation();
        o.setOperType(OperTypeEnum.INPUT);
        o.setAccount("A1");
        o.setCurrency("RUB");
        o.setOperDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
        o.setAmount(BigDecimal.ONE);
        return (O) o;
    }

    private <O extends Operation> O oo() {
        OutputOperation o = new OutputOperation();
        o.setOperType(OperTypeEnum.OUTPUT);
        o.setAccount("A1");
        o.setCurrency("RUB");
        o.setOperDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
        o.setAmount(BigDecimal.ONE);
        return (O) o;
    }

    private  <O extends Operation> O od( O o, String d) {
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

    @Test(enabled = false)
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
                .forEach(element -> {
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

    @Test(enabled = false)
    public void checkJson () {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("scheme4", Version.unknownVersion());
        module.addDeserializer(Operation.class, new Config.Scheme4Desirializer());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
        mapper.configure(SerializationConfig.Feature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        SerializationConfig serializationConfig = mapper.getSerializationConfig();
        mapper.setSerializationConfig(serializationConfig.withDateFormat(Config.dateFmt()));
        DeserializationConfig deserializationConfig = mapper.getDeserializationConfig();
        mapper.setDeserializationConfig(deserializationConfig.withDateFormat(Config.dateFmt()));

        ComplexOper co = new ComplexOper();
        co.getOperations().add(io());
        co.getOperations().add(oo());

        TransferOperation to = new TransferOperation();
        to.setOperType(OperTypeEnum.TRANSFER);
        to.setAccount("A1");
        to.setCurrency("RUB");
        to.setOperDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
        to.setAmount(BigDecimal.ONE);
        to.setDestAccount("A2");
        to.setDestCurrency("USD");

        co.getOperations().add(to);

        try {
            String jsonInString = mapper.writeValueAsString(co);
            System.out.println(jsonInString);

            ComplexOper obj = mapper.readValue(jsonInString, ComplexOper.class);
            System.out.println(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
