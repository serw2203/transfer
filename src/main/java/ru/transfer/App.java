package ru.transfer;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.transfer.face.Face;
import ru.transfer.helper.JdbcHelper;
import ru.transfer.init.ClientAccountBatchQuery;
import ru.transfer.init.CrossRateBatchQuery;
import ru.transfer.init.DdlBatchQuery;


/**
 *
 */
public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        Face osi = new Face();
        sf.setServiceBean(osi);
        sf.setAddress("http://localhost:9000/");
        sf.setProvider(new JacksonJsonProvider());
        sf.create();
        JdbcHelper helper = new JdbcHelper();
        try {
            helper.executeBatch(new DdlBatchQuery());
            helper.executeBatch(new ClientAccountBatchQuery());
            helper.executeBatch(new CrossRateBatchQuery());
            logger.info("DONE !!!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
