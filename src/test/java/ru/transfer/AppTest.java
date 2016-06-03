package ru.transfer;

import org.junit.Assert;
import org.junit.BeforeClass;
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
            jdbc.executeBatch(new ClientAccountBatchQueries());
            jdbc.executeBatch(new OperationBatchQueries(3));
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
            input.setInputCurrency("USD");
            input.setInputAccount("ACC00001");
            input.setInputCurrency("RUB");
            input.setInputDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            complexOper.getOperations().add(input);

            InputOperation input1 = new InputOperation();
            input1.setInputAmount(new BigDecimal("10.00"));
            input1.setInputCurrency("GBP");
            input1.setInputAccount("ACC00001");
            input1.setInputCurrency("RUB");
            input1.setInputDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            complexOper.getOperations().add(input1);

            InputOperation input2 = new InputOperation();
            input2.setInputAmount(new BigDecimal("100.00"));
            input2.setInputCurrency("EUR");
            input2.setInputAccount("ACC00001");
            input2.setInputCurrency("CHF");
            input2.setInputDate(Utils.dateTimeToTimestamp("2007-01-01T10:00:00+0000"));
            complexOper.getOperations().add(input2);

            operation.addOpers(complexOper);

            ExtractRoot inputExtract = analytical.extracts("ACC00001", Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"), Utils.dateTimeToTimestamp("2007-01-01T10:00:00+0000"));

            for (Balance balance : inputExtract.getInput()) {
                assertEquals(balance.getBalance().compareTo(BigDecimal.ZERO), 0);
            }

            for (Balance balance : inputExtract.getOutput()) {
                if ("RUB".equals(balance.getCurCode())) {
                    Rate rate = analytical.rate(input.getInputCurrency(), input.getInputCurrency(), input.getInputDate());
                    Rate rate1 = analytical.rate(input1.getInputCurrency(), input1.getInputCurrency(), input1.getInputDate());

                    BigDecimal calcBal  = input.getInputAmount().multiply(rate.getRate()).setScale(2, RoundingMode.HALF_UP);
                    calcBal = calcBal.add(input1.getInputAmount().multiply(rate1.getRate()).setScale(2, RoundingMode.HALF_UP)) ;

                    assertEquals(balance.getBalance().compareTo(calcBal), 0);
                } else
                if ("CHF".equals(balance.getCurCode())) {
                    Rate rate = analytical.rate(input2.getInputCurrency(), input2.getInputCurrency(), input2.getInputDate());
                    assertEquals(balance.getBalance().compareTo(input2.getInputAmount().multiply(rate.getRate()).setScale(2, RoundingMode.HALF_UP)), 0);
                } else {
                    assertEquals(balance.getBalance().compareTo(BigDecimal.ZERO), 0);
                }
            }

            //--------
            ComplexOper complexOutput = new ComplexOper();

            OutputOperation output = new OutputOperation();
            output.setOutputAmount(new BigDecimal("1.00"));
            output.setOutputCurrency("GBP");
            output.setOutputAccount("ACC00001");
            output.setOutputCurrency("RUB");
            output.setOutputDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            complexOutput.getOperations().add(output);

            OutputOperation output1 = new OutputOperation();
            output1.setOutputAmount(new BigDecimal("1.00"));
            output1.setOutputCurrency("EUR");
            output1.setOutputAccount("ACC00001");
            output1.setOutputCurrency("CHF");
            output1.setOutputDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            complexOutput.getOperations().add(output1);

            operation.addOpers(complexOutput);
            ExtractRoot outputExtract = analytical.extracts("ACC00001", Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"), Utils.dateTimeToTimestamp("2007-01-01T10:00:00+0000"));

            Rate rate = analytical.rate(output.getOutputCurrency(), output.getOutputCurrency(), output.getOutputDate());
            assertEquals(balanceFrom(inputExtract.getOutput(), "RUB").subtract(balanceFrom(outputExtract.getOutput(), "RUB")).compareTo(
                            rate.getRate().multiply(output.getOutputAmount()).setScale(2, RoundingMode.HALF_UP)), 0);

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

}
