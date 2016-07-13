package ru.transfer.conf;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.codehaus.jackson.*;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.module.SimpleModule;
import ru.transfer.face.AnalyticalFace;
import ru.transfer.face.OperationFace;
import ru.transfer.helper.Jdbc;
import ru.transfer.init.DdlBatchQueries;
import ru.transfer.model.*;
import ru.transfer.util.Utils;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

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

    public static class Scheme4Desirializer extends JsonDeserializer<Operation> {

        @Override
        public Operation deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            ObjectCodec oc = jp.getCodec();
            JsonNode node = oc.readTree(jp);
            OperTypeEnum nv = OperTypeEnum.fromValue(node.get("operType").asText());
            Operation oper;
            if (OperTypeEnum.INPUT == nv){
                oper = new InputOperation();
            } else
            if (OperTypeEnum.OUTPUT == nv) {
                oper = new OutputOperation();
            }
            else
            if (OperTypeEnum.TRANSFER == nv) {
                oper = new TransferOperation();
                ((TransferOperation) oper).setDestAccount(Utils.NNE(node.get("destAccount").asText(), "Destination account number must to be not null")  );
                ((TransferOperation) oper).setDestCurrency(Utils.NNE(node.get("destCurrency").asText(), "Destination currency number must to be not null ") );
            }
            else {
               throw new RuntimeException("Unknown operation type");
            }
            oper.setOperType(nv);
            oper.setOperDate(Utils.dateTimeToTimestamp( node.get("operDate").asText()) );
            oper.setAccount(node.get("account").asText());
            oper.setCurrency(node.get("currency").asText());
            oper.setAmount( new BigDecimal(node.get("amount").asText()) );
            return oper;
        }
    }

    private Config() {
    }

    public static SimpleDateFormat dateFmt () {
        return new SimpleDateFormat(SFMT);
    }

    public static List<Object> providers (){
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("scheme4", Version.unknownVersion());
        module.addDeserializer(Operation.class, new Scheme4Desirializer());
        mapper.registerModule(module);
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
