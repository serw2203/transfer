package ru.transfer;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.transfer.face.Face;


/**
 *
 */
public class App {
    private static Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        Face osi = new Face();
        sf.setServiceBean(osi);
        sf.setAddress("http://localhost:9000/");
        sf.setProvider(new JacksonJsonProvider ());
        sf.create();
    }
}
