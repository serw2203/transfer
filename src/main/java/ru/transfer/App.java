package ru.transfer;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import ru.transfer.conf.Config;


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
