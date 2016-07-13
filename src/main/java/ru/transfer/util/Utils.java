package ru.transfer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.transfer.conf.Config;
import ru.transfer.expt.TransferAppException;
import ru.transfer.model.Error;
import ru.transfer.model.ErrorType;

import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 */
@SuppressWarnings("unchecked")
public class Utils {
    private final static Logger log = LoggerFactory.getLogger(Utils.class);

    public static String arrayToString(Object[] array) {
        if (array != null) {
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            printWriter.write("{");
            printWriter.write(
                    Arrays.stream(array)
                            .map(it -> Objects.toString(it) ).collect(Collectors.joining(", ")));
            printWriter.write("}");
            return result.toString();
        } else
            return null;
    }

    private static String stackTraceToString(Exception e) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        return result.toString();
    }

    private static <T> boolean isFilled(T o) {
        if (o == null) {
            return false;
        } else if (o instanceof CharSequence) {
            return ((CharSequence) o).length() != 0;
        } else if (o instanceof Collection) {
            return !((Collection) o).isEmpty();
        } else if (o instanceof Map) {
            return !((Map) o).isEmpty();
        } else if (o.getClass().isArray()) {
            return Array.getLength(o) != 0;
        }
        return true;
    }

    public static <T> T first(Collection<T> collection) {
        if (!collection.isEmpty()) {
            return collection.iterator().next();
        } else
            throw new IllegalStateException(
                    String.format("Invalid result : found %d elements", collection.size()));
    }

    /**
     * instead of Objects.requireNonNull(T obj) and Objects.requireNonNull(T obj, String message)
     */
    public static <T> T NNE(Object obj, String message) {
        if (obj != null && isFilled(obj)) {
            return (T) obj;
        } else
            throw new IllegalStateException(message);
    }

    public static <T> T NNE(Object obj) {
        return NNE(obj, "Object must to be not null or not empty");
    }

    /*using in bindind.xjb*/
    public static Timestamp dateTimeToTimestamp(String date) {
        try {
            return new Timestamp(Config.dateFmt().parse(date).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /*
    public static <T> T valueFrom(T obj, Set<T> set) {
        return set.stream().filter(it -> it.equals(obj)).findAny()
                .orElseThrow(() -> new RuntimeException(String.format("Object '%s' not found", obj)));
    }
    */

    /*using in bindind.xjb*/
    public static String timestampToDateTime(Timestamp date) {
        return Config.dateFmt().format(date);
    }

    public static String timestampToDateTime(long date) {
        return Config.dateFmt().format(date);
    }

    public static Response ok(Object obj) {
        return Response.status(200).header("content-type", "application/json; charset=utf-8")
                .entity(obj).build();
    }

    public static Response error(Exception e) {
        Error error = new Error();
        error.setError(e.getMessage());
        if (e instanceof TransferAppException) {
            error.setType(ErrorType.APPLICATION);
        } else {
            error.setType(ErrorType.SYSTEM);
        }
        error.setStackTrace(stackTraceToString(e));
        e.printStackTrace();
        return Response.status(500).header("content-type", "application/json; charset=utf-8")
                .entity(error).build();
    }

    public static void traceSql(String sql, Object[] params) {
        log.trace("\tSQL : --->\n\t{}", sql);
        if (params != null) {
            log.trace("\tWITH PARAMS : {}", arrayToString(params), sql);
        }
    }
}
