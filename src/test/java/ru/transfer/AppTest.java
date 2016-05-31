package ru.transfer;

import org.junit.Before;
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
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AppTest {

    private static Logger log = LoggerFactory.getLogger(AppTest.class);
    private static boolean isInit;
    private AnalyticalService analytical = new AnalyticalServiceImpl();
    private OperationService operation = new OperationServiceImpl();

    @Before
    public void init() {
        if (!isInit) {
            //TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            isInit = true;
            Jdbc jdbc = new Jdbc();
            try {
                jdbc.executeBatch(new DdlBatchQueries());
                jdbc.executeBatch(new CrossRateBatchQueries());
                jdbc.executeBatch(new ClientAccountBatchQueries());
                jdbc.executeBatch(new OperationBatchQueries());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
                assert rate != null && rate.getRate() != null;
                log.trace("{}/{} = {}", new Object[]{rate.getScurCode(), rate.getTcurCode(), rate.getRate()});
            }

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test
    public void test () {
        try {
            log.info("{}", Utils.first( new Jdbc().executeQuery("select count(*) as cnt from aaa_oper")).get("CNT"));

            Client client = new Client();
            client.setLastName("Lastname1");
            client.setFirstName("Firstname1");
            client.setModifyDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            client = operation.addClient(client);
            assert analytical.client(client.getClientId()) != null;

            Account account = new Account();
            account.setAccNum("ACC00001");
            account.setClientId(client.getClientId());
            account = operation.addAccount(account);
            assert analytical.accounts(client.getClientId()) != null;
            assert analytical.account(account.getAccNum()) != null;

            client = new Client();
            client.setLastName("Lastname2");
            client.setFirstName("Firstname2");
            client = operation.addClient(client);
            client.setModifyDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            assert analytical.client(client.getClientId()) != null;

            account = new Account();
            account.setAccNum("ACC00002");
            account.setClientId(client.getClientId());
            account = operation.addAccount(account);

            assert analytical.accounts(client.getClientId()) != null;
            assert analytical.account(account.getAccNum()) != null;

            ComplexOper complexOper =  new ComplexOper();

            InputOperation inputOperation = new InputOperation();
            inputOperation.setParishAmount(new BigDecimal("100.00"));
            inputOperation.setParishCurrency("USD");
            inputOperation.setInputAccount("ACC00001");
            inputOperation.setInputCurrency("RUB");
            inputOperation.setInputDate(Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"));
            complexOper.getOperations().add(inputOperation);

            inputOperation = new InputOperation();
            inputOperation.setParishAmount(new BigDecimal("100.00"));
            inputOperation.setParishCurrency("EUR");
            inputOperation.setInputAccount("ACC00001");
            inputOperation.setInputCurrency("CHF");
            inputOperation.setInputDate(Utils.dateTimeToTimestamp("2007-01-01T10:00:00+0000"));
            complexOper.getOperations().add(inputOperation);

            operation.addOpers(complexOper);
            ExtractRoot extractRoot = analytical.extracts("ACC00001", Utils.dateTimeToTimestamp("2006-01-01T10:00:00+0000"), new Timestamp(System.currentTimeMillis()));

            assert extractRoot != null;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

}
