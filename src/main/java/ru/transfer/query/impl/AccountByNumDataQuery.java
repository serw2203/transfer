package ru.transfer.query.impl;

import ru.transfer.model.Client;
import ru.transfer.query.DataQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *
 */
public class AccountByNumDataQuery implements DataQuery<Client> {
    @Override
    public List<Client> handle(ResultSet resultSet) throws Exception {
        return null;
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return null;
    }
}
