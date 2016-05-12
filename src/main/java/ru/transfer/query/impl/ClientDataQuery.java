package ru.transfer.query.impl;

import ru.transfer.model.Client;
import ru.transfer.query.DataQuery;
import ru.transfer.util.Helper;

import javax.xml.datatype.DatatypeConfigurationException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ClientDataQuery implements DataQuery<Client> {

    private static final String SQL = "select c.client_id, hc.last_name, hc.first_name, hc.middle_name, hc.modify_date from aaa_client c\n" +
            "join aaa_h_client hc on c.client_id = hc.client_id and hc.cli_version = 0\n" +
            "where c.client_id != 0 and c.client_id = ?";

    private BigInteger clientId;

    public BigInteger getClientId() {
        return clientId;
    }

    public void setClientId(BigInteger clientId) {
        this.clientId = clientId;
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        if (getClientId() != null) {
            Object[] params = new Long[]{this.getClientId().longValue()};
            return CommonUpdateQuery.instance(SQL, params).createPreparedStatement(connection);
        } else throw new RuntimeException("ClientDataQuery.createPreparedStatement : clientId must to be not null");
    }

    @Override
    public List<Client> handle(ResultSet resultSet) throws SQLException, DatatypeConfigurationException {
        List<Client> result = new ArrayList<>();
        while (resultSet.next()) {
            Client client = new Client();
            client.setClientId(BigInteger.valueOf(resultSet.getLong("CLIENT_ID"))  );
            client.setFirstName(resultSet.getString("FIRST_NAME"));
            client.setLastName(resultSet.getString("LAST_NAME"));
            client.setMiddleName(resultSet.getString("MIDDLE_NAME"));
            client.setModifyDate(Helper.convertDate(resultSet.getDate("MODIFY_DATE")));
            result.add(client);
        }
        return result;
    }

}
