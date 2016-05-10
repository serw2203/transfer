package ru.transfer.query;

import java.sql.ResultSet;
import java.util.List;

/**
 *
 */
public interface Query<T> extends Execute {
    List<T> handle (ResultSet resultSet) throws Exception;
}
