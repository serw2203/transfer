package ru.transfer;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import ru.transfer.conf.Config;
import ru.transfer.helper.Jdbc;
import ru.transfer.init.ClientAccountBatchQueries;
import ru.transfer.init.CrossRateBatchQueries;
import ru.transfer.init.OperationBatchQueries;
import ru.transfer.model.*;
import ru.transfer.query.CommonQuery;
import ru.transfer.query.DataQuery;
import ru.transfer.util.Utils;

import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AppTest extends Assert {

    private final static Logger log = LoggerFactory.getLogger(AppTest.class);

    private static class CurrencyPair {
        String firstCode;
        String secondCode;
    }

    private static class CurrencyPairDataQuery implements DataQuery<CurrencyPair> {
        @Override
        public List<CurrencyPair> handle(ResultSet resultSet) throws Exception {
            List<CurrencyPair> result = new ArrayList<>();
            while (resultSet.next()) {
                CurrencyPair pair = new CurrencyPair();
                pair.firstCode = resultSet.getString("SCUR_CODE");
                pair.secondCode = resultSet.getString("TCUR_CODE");
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


    private static <T> T httpGet(String path, Class<T> clazz, Object... pathParams) {
        return WebClient.create("http://0.0.0.0:9000/analytical/", Config.providers())
                .header("content-type", "application/json; charset=utf-8")
                .path(path, pathParams)
                .accept(MediaType.APPLICATION_JSON_TYPE).get(clazz);
    }

    private static <T> T httpPost(String path, Class<T> clazz, Object body) {
        return WebClient.create("http://0.0.0.0:9000/operation/", Config.providers())
                .header("content-type", "application/json; charset=utf-8")
                .path(path)
                .accept(MediaType.APPLICATION_JSON_TYPE).post(body, clazz);
    }

    private BigDecimal balanceFrom(List<Balance> list, String curr) {
        return list.stream().filter((Balance it) -> curr.equals(it.getCurCode())).findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("%s - not found", curr))).getBalance();
    }

    private Server server;

    @BeforeSuite
    public void init() {
        JAXRSServerFactoryBean serverFactory = new JAXRSServerFactoryBean();
        Config.config(serverFactory);
        server = serverFactory.create();
        Config.init(server);
    }

    @AfterSuite
    private void destroy() {
        server.destroy();
    }


    @Test(groups = {"main"})
    public void checkCurrency() {
        try {
            CurrencyRoot currencyRoot = httpGet("currency", CurrencyRoot.class);
            assertNotNull(currencyRoot);
            assertEquals(5, currencyRoot.getCurrencies().size());
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test(groups = {"main"}, dependsOnMethods = {"checkCurrency"})
    public void initXXX() {
        try {
            new Jdbc().executeBatch(new CrossRateBatchQueries());
            log.info("--- >>> - init count of cross rates - {}",
                    new Jdbc().executeQuery("select count(*) cnt from aaa_cross_rate").stream().findFirst().get().get("CNT"));

            new Jdbc().executeBatch(new ClientAccountBatchQueries());
            log.info("--- >>> - init count of accounts - {}",
                    new Jdbc().executeQuery("select count(*) cnt from aaa_account").stream().findFirst().get().get("CNT"));


            new Jdbc().executeBatch(new OperationBatchQueries(1));
            log.info("--- >>> - init count of operations - {}",
                    new Jdbc().executeQuery("select count(*) cnt from aaa_oper").stream().findFirst().get().get("CNT"));

            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test(groups = {"main"}, dependsOnMethods = {"checkCurrency", "initXXX"})
    public void checkCurrencyRate() {
        try {
            final String current = Utils.timestampToDateTime(System.currentTimeMillis());
            new Jdbc().executeQuery(new CurrencyPairDataQuery()).stream()
                    .forEach(pair -> {
                        assertNotNull(
                                httpGet("rate/{scur}/{tcur}/{date}", Rate.class,
                                        pair.firstCode, pair.secondCode, current));
                    });
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test(groups = {"main"}, dependsOnMethods = {"checkCurrency", "initXXX", "checkCurrencyRate"})
    public void checkAccountClient() {
        Client client = new Client();
        client.setLastName("CLI00001");
        client.setModifyDate(Utils.dateTimeToTimestamp("2006-01-01T06:00:00+0000"));
        client = httpPost("addClient", Client.class, client);
        assertNotNull(client.getClientId());

        Account account = new Account();
        account.setClientId(client.getClientId());
        account.setAccNum("ACC00001");
        Account r_account = httpPost("addAccount", Account.class, account);
        assertNotNull(r_account.getAccId());

        Account g_account = httpGet("account/{acc_num}", Account.class, "ACC00001");
        assertNotNull(g_account);
        assertNotNull(g_account.getAccId());

        Account account2 = new Account();
        account2.setClientId(client.getClientId());
        account2.setAccNum("ACC00002");
        Account r_account2 = httpPost("addAccount", Account.class, account2);
        assertNotNull(r_account2.getAccId());

        Account g_account2 = httpGet("account/{acc_num}", Account.class, "ACC00002");
        assertNotNull(g_account2);
        assertNotNull(g_account2.getAccId());
    }

    @Test(groups = {"main"},
            dependsOnMethods = {"checkCurrency", "initXXX", "checkCurrencyRate", "checkCurrencyRate", "checkAccountClient"},
            threadPoolSize = 4, invocationCount = 1000, timeOut = 180000)
    public void checkMtOpers() {

        String inputDateStr = "2006-01-01T10:00:00+0000";
        String outputDateStr = "2006-01-01T10:00:00+0000";
        String transferDateStr = "2006-01-01T10:00:00+0000";

        Timestamp inputDate = Utils.dateTimeToTimestamp(inputDateStr);
        Timestamp outputDate = Utils.dateTimeToTimestamp(outputDateStr);
        Timestamp transferDate = Utils.dateTimeToTimestamp(transferDateStr);

        ComplexOper complexOper = new ComplexOper();

        InputOperation input = new InputOperation();
        input.setOperType(OperTypeEnum.INPUT);
        input.setAmount(new BigDecimal("2.00"));
        input.setAccount("ACC00001");
        input.setCurrency("RUB");
        input.setOperDate(inputDate);
        complexOper.getOperations().add(input);

        OutputOperation output = new OutputOperation();
        output.setOperType(OperTypeEnum.OUTPUT);
        output.setAccount("ACC00001");
        output.setAmount(BigDecimal.ONE);
        output.setCurrency("RUB");
        output.setOperDate(outputDate);
        complexOper.getOperations().add(output);

        TransferOperation transfer = new TransferOperation();
        transfer.setOperType(OperTypeEnum.TRANSFER);
        transfer.setAccount("ACC00001");
        transfer.setCurrency("RUB");
        transfer.setOperDate(transferDate);
        transfer.setAmount(BigDecimal.ONE);
        transfer.setDestAccount("ACC00002");
        transfer.setDestCurrency("RUB");
        complexOper.getOperations().add(transfer);

        assertEquals(3, httpPost("addOperations", Extracts.class, complexOper)
                .getExtracts().size());
    }

    @Test(enabled = false,groups = {"main"}, dependsOnMethods = {"checkCurrency", "initXXX", "checkCurrencyRate", "checkCurrencyRate", "checkAccountClient", "checkMtOpers"})
    public void cc () {
        try {
            new Jdbc().executeQuery("select * from aaa_balance").stream().forEach(System.out::println);
            new Jdbc().executeQuery("select * from aaa_oper").stream().forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(enabled = false, groups = {"main"}, dependsOnMethods = {"checkCurrency", "initXXX", "checkCurrencyRate", "checkCurrencyRate", "checkAccountClient"})
    public void checkOpers() {
        String inputDateStr = "2006-01-01T10:00:00+0000";
        String outputDateStr = "2006-01-01T10:00:00+0000";
        Timestamp inputDate = Utils.dateTimeToTimestamp(inputDateStr);
        Timestamp outputDate = Utils.dateTimeToTimestamp(outputDateStr);

        ComplexOper complexOper = new ComplexOper();
        OutputOperation output = new OutputOperation();
        output.setOperType(OperTypeEnum.OUTPUT);
        output.setAccount("ACC00001");
        output.setAmount(new BigDecimal("99.00"));
        output.setCurrency("RUB");
        output.setOperDate(outputDate);
        complexOper.getOperations().add(output);
        InputOperation input = new InputOperation();
        input.setOperType(OperTypeEnum.INPUT);
        input.setAmount(new BigDecimal("100.00"));
        input.setAccount("ACC00001");
        input.setCurrency("RUB");
        input.setOperDate(inputDate);
        complexOper.getOperations().add(input);

        assertEquals(2, httpPost("addOperations", Extracts.class, complexOper)
                .getExtracts().size());

        ExtractRoot er = httpGet("/extracts/{acc_num}/{start_date}/{stop_date}", ExtractRoot.class,
                "ACC00001", inputDateStr, outputDateStr);

        assertEquals(balanceFrom(er.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
        assertEquals(balanceFrom(er.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);

        inputDateStr = "2006-01-01T10:00:00+0000";
        outputDateStr = "2006-01-01T10:00:01+0000";
        inputDate = Utils.dateTimeToTimestamp(inputDateStr);
        outputDate = Utils.dateTimeToTimestamp(outputDateStr);
        input.setOperDate(inputDate);
        output.setOperDate(outputDate);

        assertEquals(2, httpPost("addOperations", Extracts.class, complexOper)
                .getExtracts().size());

        ExtractRoot er1 = httpGet("/extracts/{acc_num}/{start_date}/{stop_date}", ExtractRoot.class,
                "ACC00001", inputDateStr, outputDateStr);
        assertEquals(balanceFrom(er1.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
        assertEquals(balanceFrom(er1.getOutput(), "RUB").compareTo(new BigDecimal("2")), 0);

        String transferDateStr = "2006-01-01T10:00:00+0000";
        Timestamp transferDate = Utils.dateTimeToTimestamp(transferDateStr);
        complexOper.getOperations().clear();

        TransferOperation transfer = new TransferOperation();
        transfer.setOperType(OperTypeEnum.TRANSFER);
        transfer.setAccount("ACC00001");
        transfer.setCurrency("RUB");
        transfer.setOperDate(transferDate);
        transfer.setAmount(BigDecimal.ONE);
        transfer.setDestAccount("ACC00002");
        transfer.setDestCurrency("RUB");
        complexOper.getOperations().add(transfer);

        assertEquals(1, httpPost("addOperations", Extracts.class, complexOper)
                .getExtracts().size());

        ExtractRoot er2 = httpGet("/extracts/{acc_num}/{start_date}/{stop_date}", ExtractRoot.class,
                "ACC00001", inputDateStr, outputDateStr);

        assertEquals(balanceFrom(er2.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
        assertEquals(balanceFrom(er2.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);

        ExtractRoot er3 = httpGet("/extracts/{acc_num}/{start_date}/{stop_date}", ExtractRoot.class,
                "ACC00002", inputDateStr, outputDateStr);
        assertEquals(balanceFrom(er3.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
        assertEquals(balanceFrom(er3.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);


        transferDateStr = "2006-01-01T10:00:02+0000";
        transferDate = Utils.dateTimeToTimestamp(transferDateStr);
        transfer.setOperDate(transferDate);

        assertEquals(1, httpPost("addOperations", Extracts.class, complexOper)
                .getExtracts().size());

        ExtractRoot er4 = httpGet("/extracts/{acc_num}/{start_date}/{stop_date}", ExtractRoot.class,
                "ACC00001", inputDateStr, outputDateStr);
        assertEquals(balanceFrom(er4.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
        assertEquals(balanceFrom(er4.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);


        ExtractRoot er5 = httpGet("/extracts/{acc_num}/{start_date}/{stop_date}", ExtractRoot.class,
                "ACC00001", inputDateStr, transferDateStr);
        assertEquals(balanceFrom(er5.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
        assertEquals(balanceFrom(er5.getOutput(), "RUB").compareTo(BigDecimal.ZERO), 0);

        ExtractRoot er6 = httpGet("/extracts/{acc_num}/{start_date}/{stop_date}", ExtractRoot.class,
                "ACC00002", inputDateStr, outputDateStr);
        assertEquals(balanceFrom(er6.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
        assertEquals(balanceFrom(er6.getOutput(), "RUB").compareTo(BigDecimal.ONE), 0);

        ExtractRoot er7 = httpGet("/extracts/{acc_num}/{start_date}/{stop_date}", ExtractRoot.class,
                "ACC00002", inputDateStr, transferDateStr);
        assertEquals(balanceFrom(er7.getInput(), "RUB").compareTo(BigDecimal.ZERO), 0);
        assertEquals(balanceFrom(er7.getOutput(), "RUB").compareTo(new BigDecimal("2")), 0);
    }


}
