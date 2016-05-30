package ru.transfer;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.transfer.conf.Config;
import ru.transfer.helper.Jdbc;
import ru.transfer.init.ClientAccountBatchQueries;
import ru.transfer.init.CrossRateBatchQueries;
import ru.transfer.init.OperationBatchQueries;


/**
 *
 */
public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        JAXRSServerFactoryBean serverFactory = new JAXRSServerFactoryBean();
        Config.config(serverFactory);
        Server server = serverFactory.create();
        Config.init(server);
        Jdbc jdbc = new Jdbc();
        try {
            long st = System.currentTimeMillis();
            jdbc.executeBatch(new ClientAccountBatchQueries());
            logger.info("CLIENTS & ACCOUNTS - init!");
            jdbc.executeBatch(new CrossRateBatchQueries());
            logger.info("CROSS RATE - init!");
            jdbc.executeBatch(new OperationBatchQueries());
            logger.info("OPERATION & REST - init!");
            logger.info("ALL INIT's - done!!! Elapsed time - {} s", (System.currentTimeMillis() - st) / 1000.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
