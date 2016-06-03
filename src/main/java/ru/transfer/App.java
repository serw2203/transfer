package ru.transfer;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.transfer.conf.Config;
import ru.transfer.helper.ConnectionHelper;
import ru.transfer.helper.Jdbc;
import ru.transfer.init.ClientAccountBatchQueries;
import ru.transfer.init.CrossRateBatchQueries;
import ru.transfer.init.OperationBatchQueries;


/**
 *
 */
public class App {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        JAXRSServerFactoryBean serverFactory = new JAXRSServerFactoryBean();
        Config.config(serverFactory);
        Server server = serverFactory.create();
        Config.init(server);
    }
}
