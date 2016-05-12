package ru.transfer.query.impl;

import ru.transfer.query.DataQuery;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class CommonDataQuery extends CommonUpdateQuery implements DataQuery<Map<String, Object>>{
    @Override
    public List<Map<String, Object>> handle(ResultSet resultSet) throws Exception {
        List<Map<String, Object>> result = new ArrayList();
        while (resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            int colCount = resultSet.getMetaData().getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                row.put(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i));
            }
            result.add(row);
        }
        return result;
    }
}
