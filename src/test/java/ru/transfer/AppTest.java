package ru.transfer;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.transfer.helper.Jdbc;
import ru.transfer.init.DdlBatchQueries;
import ru.transfer.model.Account;
import ru.transfer.model.Client;
import ru.transfer.model.Rate;
import ru.transfer.query.CommonQuery;
import ru.transfer.query.DataQuery;
import ru.transfer.service.AnalyticalService;
import ru.transfer.service.AnalyticalServiceImpl;
import ru.transfer.service.OperationService;
import ru.transfer.service.OperationServiceImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AppTest {

    private static Logger log = LoggerFactory.getLogger(AppTest.class);
    private static boolean initOnce;
    private AnalyticalService analytical = new AnalyticalServiceImpl();
    private OperationService operation = new OperationServiceImpl();

    @Before
    public void init() {
        if (!initOnce) {
            initOnce = true;
            Jdbc jdbc = new Jdbc();
            try {
                jdbc.executeBatch(new DdlBatchQueries());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class CurrencyPairDataQuery implements DataQuery<Rate> {
        @Override
        public List<Rate> handle(ResultSet resultSet) throws Exception {
            List<Rate> result = new ArrayList<>();
            while (resultSet.next()) {
                Rate rate = new Rate();
                rate.setScurCode(resultSet.getString("SCUR_CODE"));
                rate.setTcurCode(resultSet.getString("TCUR_CODE"));
                result.add(rate);
            }
            return result;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            return CommonQuery.instance("select c1.cur_code as tcur_code, c2.cur_code as scur_code from aaa_currency c1\n" +
                    "join aaa_currency c2 on c1.cur_code != c2.cur_code").createPreparedStatement(connection);
        }
    }

    @Test
    public void currTest() {
        try {
            CurrencyPairDataQuery query = new CurrencyPairDataQuery();
            List<Rate> pairRates = new Jdbc().executeQuery(query);
//            for (CurrencyPair pair : pairRates) {
//                log.info("{}", pair);
//            }

//            List<Map<String, ?>> rows = new Jdbc().executeQuery(
//                    "select c1.cur_code, c2.cur_code, avg(cr.rate) as rate from aaa_currency c1\n" +
//                            "join aaa_currency c2 on c1.cur_code != c2.cur_code\n" +
//                            "left join aaa_cross_rate cr on cr.scur_code = c1.cur_code and cr.tcur_code = c2.cur_code\n" +
//                            "group by c1.cur_code, c2.cur_code");
//            for ( Map<String, ?> row : rows) {
//                assert row.get("RATE") != null;
//            }
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test
    public void addTest() {
        try {
            Client client = new Client();
            client.setLastName("Lastname1");
            client.setFirstName("Firstname1");
            client = operation.addClient(client);
            assert analytical.client(client.getClientId()) != null;

            Account account = new Account();
            account.setAccNum("ACC00001");
            account.setClientId(client.getClientId());
            account = operation.addAccount(account);
            assert analytical.accounts(client.getClientId()) != null;
            assert analytical.account(account.getAccNum()) != null;

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }
}
