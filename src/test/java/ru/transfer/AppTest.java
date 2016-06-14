package ru.transfer;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.transfer.helper.Jdbc;
import ru.transfer.init.CrossRateBatchQueries;
import ru.transfer.init.DdlBatchQueries;
import ru.transfer.model.*;
import ru.transfer.query.CommonQuery;
import ru.transfer.query.DataQuery;
import ru.transfer.service.AnalyticalService;
import ru.transfer.service.AnalyticalServiceImpl;
import ru.transfer.service.OperationService;
import ru.transfer.service.OperationServiceImpl;
import ru.transfer.util.Utils;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 *
 */
public class AppTest extends Assert {

    private final AnalyticalService analytical = new AnalyticalServiceImpl();

    private final OperationService operation = new OperationServiceImpl();

    @BeforeClass
    public static void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Jdbc jdbc = new Jdbc();
        try {
            jdbc.executeBatch(new DdlBatchQueries());
            jdbc.executeBatch(new CrossRateBatchQueries());
//            jdbc.executeBatch(new ClientAccountBatchQueries());
//            jdbc.executeBatch(new OperationBatchQueries(10));

            OperationService operation = new OperationServiceImpl();

            Client client = new Client();
            client.setLastName("Lastname1");
            client.setFirstName("Firstname1");
            client.setModifyDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            operation.addClient(client);

            Account account = new Account();
            account.setAccNum("ACC00001");
            account.setClientId(client.getClientId());
            operation.addAccount(account);

            client = new Client();
            client.setLastName("Lastname2");
            client.setFirstName("Firstname2");
            client.setModifyDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            client = operation.addClient(client);

            account = new Account();
            account.setAccNum("ACC00002");
            account.setClientId(client.getClientId());
            operation.addAccount(account);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class CurrencyPair {
        private String sCurCode;
        private String tCurCode;

        String getsCurCode() {
            return sCurCode;
        }

        void setsCurCode(String sCurCode) {
            this.sCurCode = sCurCode;
        }

        String gettCurCode() {
            return tCurCode;
        }

        void settCurCode(String tCurCode) {
            this.tCurCode = tCurCode;
        }
    }

    private static class CurrencyPairDataQuery implements DataQuery<CurrencyPair> {
        @Override
        public List<CurrencyPair> handle(ResultSet resultSet) throws Exception {
            List<CurrencyPair> result = new ArrayList<>();
            while (resultSet.next()) {
                CurrencyPair pair = new CurrencyPair();
                pair.setsCurCode(resultSet.getString("SCUR_CODE"));
                pair.settCurCode(resultSet.getString("TCUR_CODE"));
                result.add(pair);
            }
            return result;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            return CommonQuery.instance(
                    "select c1.cur_code as scur_code, c2.cur_code as tcur_code from aaa_currency c1\n" +
                            "join aaa_currency c2 on c1.cur_code != c2.cur_code").createPreparedStatement(connection);
        }
    }

    @Ignore
    @Test
    public void checkCurrencyRate() {
        try {
            CurrencyPairDataQuery query = new CurrencyPairDataQuery();
            List<CurrencyPair> pairs = new Jdbc().executeQuery(query);
            AnalyticalService service = new AnalyticalServiceImpl();
            for (CurrencyPair pair : pairs) {
                Rate rate = service.rate(pair.getsCurCode(), pair.gettCurCode(), new Timestamp(System.currentTimeMillis()));
                assertNotNull(rate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

    private BigDecimal balanceFrom(List<Balance> list, String curr) {
        for (Balance balance : list) {
            if (curr.equals(balance.getCurCode())) {
                return balance.getBalance();
            }
        }
        throw new RuntimeException(String.format("%s - not found", curr));
    }

    @Test
    public void checkAccountClient() {
        try {
            Account account = analytical.account("ACC00001");
            assertNotNull(account);
            assertNotNull(account.getAccId());
            assertNotNull(account.getAccNum());
            assertEquals(account.getAccNum(), "ACC00001");
            assertNotNull(analytical.accounts(account.getClientId()));

            Client client = analytical.client(account.getClientId());
            assertNotNull(client);
            assertNotNull(client.getClientId());
            assertNotNull(client.getLastName());

            Account account2 = analytical.account("ACC00002");
            assertNotNull(account2);
            assertNotNull(account2.getAccId());
            assertNotNull(account2.getAccNum());
            assertEquals(account2.getAccNum(), "ACC00002");
            assertNotNull(analytical.accounts(account2.getClientId()));

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test
    public void checkIO() {
        try {
            Timestamp iDate = Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000");
            Timestamp oDate = Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000");

            ComplexOper complexOper = new ComplexOper();
            OutputOperation output = new OutputOperation();
            output.setAccount("ACC00001");
            output.setAmount(new BigDecimal("99.00"));
            output.setCurrency("RUB");
            output.setOperDate(oDate);
            complexOper.getOperations().add(output);
            InputOperation input = new InputOperation();
            input.setAmount(new BigDecimal("100.00"));
            input.setAccount("ACC00001");
            input.setCurrency("RUB");
            input.setOperDate(iDate);
            complexOper.getOperations().add(input);
            operation.call(complexOper);
            assertEquals(analytical.saldo("ACC00001", "RUB").compareTo(BigDecimal.ONE), 0);
            ExtractRoot extract = analytical.extracts("ACC00001", iDate, oDate);
            assertEquals(extract.getExtracts().size(), 2);
            assertEquals( balanceFrom(extract.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
            assertEquals( balanceFrom(extract.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);

            iDate = Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000");
            oDate = Utils.dateTimeToTimestamp("2006-01-01T10:00:01+0000");
            input.setOperDate(iDate);
            output.setOperDate(oDate);

            operation.call(complexOper);
            assertEquals(analytical.saldo("ACC00001", "RUB").compareTo(new BigDecimal("2")), 0);
            extract = analytical.extracts("ACC00001", iDate, oDate);
            assertEquals( balanceFrom(extract.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
            assertEquals( balanceFrom(extract.getOutput(), "RUB").compareTo(new BigDecimal("2")), 0);

            Timestamp tDate = Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000");
            complexOper.getOperations().clear();
            TransferOperation transfer = new TransferOperation();
            transfer.setAccount("ACC00001");
            transfer.setCurrency("RUB");
            transfer.setOperDate(tDate);
            transfer.setAmount(BigDecimal.ONE);
            transfer.setDestAccount("ACC00002");
            transfer.setDestCurrency("RUB");
            complexOper.getOperations().add(transfer);
            operation.call(complexOper);
            assertEquals(analytical.saldo("ACC00001", "RUB").compareTo(BigDecimal.ONE), 0);
            extract = analytical.extracts("ACC00001", iDate, oDate);
            assertEquals( balanceFrom(extract.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
            assertEquals( balanceFrom(extract.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);
            assertEquals(analytical.saldo("ACC00002", "RUB").compareTo(BigDecimal.ONE), 0);
            extract = analytical.extracts("ACC00002", iDate, oDate);
            assertEquals( balanceFrom(extract.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
            assertEquals( balanceFrom(extract.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);


            tDate = Utils.dateTimeToTimestamp("2006-01-01T10:00:02+0000");
            transfer.setOperDate(tDate);
            operation.call(complexOper);
            assertEquals(analytical.saldo("ACC00001", "RUB").compareTo(BigDecimal.ZERO), 0);
            assertEquals(analytical.saldo("ACC00002", "RUB").compareTo(new BigDecimal("2")), 0);

            extract = analytical.extracts("ACC00001", iDate, oDate);
            assertEquals( balanceFrom(extract.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
            assertEquals(balanceFrom(extract.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);
            extract = analytical.extracts("ACC00001", iDate, tDate);
            assertEquals( balanceFrom(extract.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
            assertEquals(balanceFrom(extract.getOutput(), "RUB").compareTo(BigDecimal.ZERO), 0);
            extract = analytical.extracts("ACC00002", iDate, oDate);
            assertEquals( balanceFrom(extract.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
            assertEquals( balanceFrom(extract.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);
            extract = analytical.extracts("ACC00002", iDate, tDate);
            assertEquals( balanceFrom(extract.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
            assertEquals( balanceFrom(extract.getOutput(), "RUB").compareTo(new BigDecimal("2")), 0);

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }
}
