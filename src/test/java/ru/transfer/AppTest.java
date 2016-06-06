package ru.transfer;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.transfer.helper.Jdbc;
import ru.transfer.init.ClientAccountBatchQueries;
import ru.transfer.init.CrossRateBatchQueries;
import ru.transfer.init.DdlBatchQueries;
import ru.transfer.init.OperationBatchQueries;
import ru.transfer.model.*;
import ru.transfer.query.CommonQuery;
import ru.transfer.query.DataQuery;
import ru.transfer.service.AnalyticalService;
import ru.transfer.service.AnalyticalServiceImpl;
import ru.transfer.service.OperationService;
import ru.transfer.service.OperationServiceImpl;
import ru.transfer.util.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 *
 */
public class AppTest extends Assert {
    private static Logger log = LoggerFactory.getLogger(AppTest.class);

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
//            jdbc.executeBatch(new OperationBatchQueries(1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class CurrencyPair {
        private String sCurCode;
        private String tCurCode;

        public String getsCurCode() {
            return sCurCode;
        }

        public void setsCurCode(String sCurCode) {
            this.sCurCode = sCurCode;
        }

        public String gettCurCode() {
            return tCurCode;
        }

        public void settCurCode(String tCurCode) {
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
    public void currencyRate() {
        try {
            CurrencyPairDataQuery query = new CurrencyPairDataQuery();
            List<CurrencyPair> pairs = new Jdbc().executeQuery(query);
            AnalyticalService service = new AnalyticalServiceImpl();
            for (CurrencyPair pair : pairs) {
                Rate rate = service.rate(pair.getsCurCode(), pair.gettCurCode(), new Timestamp(System.currentTimeMillis()));
                assertNotNull(rate);
                log.trace("{}/{} = {}", new Object[]{rate.getScurCode(), rate.getTcurCode(), rate.getRate()});
            }

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

    private BigDecimal balanceFrom (List<Balance> list, String curr) {
      for (Balance balance  : list) {
          if ( curr.equals(balance.getCurCode()) ) {
              return balance.getBalance();
          }
      }
      throw new RuntimeException(String.format("%s - not found", curr));
    }

    @Test
    public void test() {
        try {
            Client client = new Client();
            client.setLastName("Lastname1");
            client.setFirstName("Firstname1");
            client.setModifyDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            client = operation.addClient(client);
            assertNotNull(client.getClientId());
            assertNotNull(analytical.client(client.getClientId()));

            Account account = new Account();
            account.setAccNum("ACC00001");
            account.setClientId(client.getClientId());
            account = operation.addAccount(account);
            assertNotNull(account.getAccId());
            assertNotNull(analytical.accounts(client.getClientId()));

            account = analytical.account(account.getAccNum());
            assertNotNull(account);
            assertEquals(account.getAccNum(), "ACC00001");

            client = new Client();
            client.setLastName("Lastname2");
            client.setFirstName("Firstname2");
            client = operation.addClient(client);
            client.setModifyDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            assertNotNull(analytical.client(client.getClientId()));

            account = new Account();
            account.setAccNum("ACC00002");
            account.setClientId(client.getClientId());
            operation.addAccount(account);
            account = analytical.account(account.getAccNum());
            assertNotNull(account);
            assertEquals(account.getAccNum(), "ACC00002");

            //--------
            ComplexOper complexOper = new ComplexOper();

            InputOperation input = new InputOperation();
            input.setInputAmount(new BigDecimal("100.00"));
            input.setAccount("ACC00001");
            input.setCurrency("RUB");
            input.setOperDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            complexOper.getOperations().add(input);

            InputOperation input1 = new InputOperation();
            input1.setInputAmount(new BigDecimal("10.00"));
            input1.setAccount("ACC00001");
            input1.setCurrency("RUB");
            input1.setOperDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            complexOper.getOperations().add(input1);

            InputOperation input2 = new InputOperation();
            input2.setInputAmount(new BigDecimal("100.00"));
            input2.setCurrency("EUR");
            input2.setAccount("ACC00001");
            input2.setOperDate(Utils.dateTimeToTimestamp("2007-01-01T10:00:00+0000"));
            complexOper.getOperations().add(input2);

            operation.call(complexOper);

            ExtractRoot inputExtract = analytical.extracts("ACC00001", Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"), Utils.dateTimeToTimestamp("2007-01-01T10:00:00+0000"));

            for (Balance balance : inputExtract.getInput()) {
                assertEquals(balance.getBalance().compareTo(BigDecimal.ZERO), 0);
            }

            assertEquals(balanceFrom(inputExtract.getOutput(), "RUB").compareTo(new BigDecimal("110")), 0);
            assertEquals(balanceFrom(inputExtract.getOutput(), "EUR").compareTo(new BigDecimal("100")), 0);

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

}
