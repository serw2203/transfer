package ru.transfer.query.impl;

import ru.transfer.model.Client;
import ru.transfer.query.AbstractExecute;
import ru.transfer.query.Query;
import ru.transfer.util.Utils;

import javax.xml.datatype.DatatypeConfigurationException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ClientQuery extends AbstractExecute implements Query<Client> {
    @Override
    public List<Client> handle(ResultSet resultSet) throws SQLException, DatatypeConfigurationException {
        List<Client> result = new ArrayList<>();
        while (resultSet.next()) {
            Client client = new Client();
            client.setClientId(resultSet.getInt("CLIENT_ID"));
            client.setFirstName(resultSet.getString("FIRST_NAME"));
            client.setLastName(resultSet.getString("LAST_NAME"));
            client.setMiddleName(resultSet.getString("MIDDLE_NAME"));
            client.setModifyDate(Utils.convertDate(resultSet.getDate("MODIFY_DATE")));
            result.add(client);
        }
        return result;
    }

    @Override
    public String sql() {
        return "select c.client_id, hc.last_name, hc.first_name, hc.middle_name, hc.modify_date from aaa_client c\n" +
                "join aaa_h_client hc on c.client_id = hc.client_id and hc.cli_version = 0\n" +
                "where c.client_id != 0 and c.client_id = ?";
    }
}
