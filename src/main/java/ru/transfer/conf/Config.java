package ru.transfer.conf;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.transfer.face.AnalyticalFace;
import ru.transfer.face.OperationFace;
import ru.transfer.helper.Jdbc;
import ru.transfer.init.DdlBatchQueries;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 *
 */
@SuppressWarnings("unchecked")
public class Config {
    private static Logger log = LoggerFactory.getLogger(Config.class);

    public final static String SFMT = "yyyy-MM-dd'T'HH:mm:ssZ";

    private static class TimestampParamConverter implements ParamConverter<Timestamp> {
        @Override
        public Timestamp fromString(String string) {
            try {
                return new Timestamp( Config.dateFmt().parse(string).getTime() );
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }
        @Override
        public String toString(Timestamp t) {
            return Config.dateFmt().format(t);
        }
    }

    private static class ParamConverterProviderImpl implements ParamConverterProvider {
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
            if (Timestamp.class.equals(type)) {
                return (ParamConverter<T>) new TimestampParamConverter();
            }
            return null;
        }
    }

    private Config() {
    }

    public static SimpleDateFormat dateFmt () {
        return new SimpleDateFormat(SFMT);
    }

    public static void config (JAXRSServerFactoryBean serverFactory) {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        serverFactory.setAddress("http://0.0.0.0:9000/");
        serverFactory.setServiceBeans(Arrays.asList(new Object[]{new AnalyticalFace(), new OperationFace()}));
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
        mapper.configure(SerializationConfig.Feature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        SerializationConfig serializationConfig = mapper.getSerializationConfig();
        mapper.setSerializationConfig(serializationConfig.withDateFormat(Config.dateFmt()));
        DeserializationConfig deserializationConfig = mapper.getDeserializationConfig();
        mapper.setDeserializationConfig(deserializationConfig.withDateFormat(Config.dateFmt()));
        JacksonJsonProvider provider = new JacksonJsonProvider();
        provider.setMapper(mapper);
        serverFactory.setProviders(Arrays.asList(new Object[]{provider, new ParamConverterProviderImpl()}));
        Map maps = new HashMap();
        maps.put("json", "application/json");
        serverFactory.setExtensionMappings(maps);
    }

    public static void init (Server server) {
        if ( server.isStarted() ) {
            Jdbc jdbc = new Jdbc();
            try {
                long st = System.currentTimeMillis();
                jdbc.executeBatch(new DdlBatchQueries());
                log.trace("DDL INIT's - done!!! Elapsed time - {} s", (System.currentTimeMillis() - st) / 1000.0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else
            throw new IllegalStateException("Invalid init system. Server not started");
    }
}
