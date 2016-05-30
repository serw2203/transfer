package ru.transfer.query;

import java.sql.ResultSet;
import java.util.List;

/**
 *
 */
public interface DataQuery<T> extends Query{
    List<T> handle (ResultSet resultSet) throws Exception;
}
