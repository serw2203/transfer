package ru.transfer.conf;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
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
import java.util.*;

/**
 *
 */
@SuppressWarnings("unchecked")
public class Config {
    private final static String SFMT = "yyyy-MM-dd'T'HH:mm:ssZ";

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

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

    public static List<Object> providers (){
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
        return Collections.singletonList( Arrays.asList(provider, new ParamConverterProviderImpl()));
    }

    public static void config (JAXRSServerFactoryBean serverFactory) {
        serverFactory.setAddress("http://0.0.0.0:9000/");
        serverFactory.setServiceBeans( Collections.singletonList( Arrays.asList(new AnalyticalFace(), new OperationFace()) ));
        serverFactory.setProviders(providers());
    }

    public static void init (Server server) {
        if ( server.isStarted() ) {
            Jdbc jdbc = new Jdbc();
            try {
                jdbc.executeBatch(new DdlBatchQueries());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else
            throw new IllegalStateException("Invalid init system. Server not started");
    }
}
