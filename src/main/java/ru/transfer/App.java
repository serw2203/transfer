package ru.transfer;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import ru.transfer.services.OperationServiceImpl;

/**
 *
 */
public class App {
    public static void main(String[] args) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        OperationServiceImpl osi = new OperationServiceImpl();
        sf.setServiceBean(osi);
        sf.setAddress("http://localhost:9000/");
        sf.create();
    }
}
