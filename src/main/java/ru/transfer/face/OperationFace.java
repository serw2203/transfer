package ru.transfer.face;

import ru.transfer.model.Account;
import ru.transfer.model.Client;
import ru.transfer.model.ComplexOper;
import ru.transfer.model.Operation;
import ru.transfer.service.OperationService;
import ru.transfer.service.OperationServiceImpl;
import ru.transfer.util.Utils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/operation")
public class OperationFace {
    private final OperationService operation = new OperationServiceImpl();

    @GET
    @Path("/add/{client}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response client(@PathParam("client") Client client) {
        try {
            return Utils.ok(operation.addClient(client));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/add/{account}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response account(@PathParam("account") Account account) {
        try {
            return Utils.ok(operation.addAccount(account));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/call/{operation}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response call (@PathParam("operation") Operation oper) {
        try {
            return Utils.ok(operation.call(oper));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/call/{complexOper}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response call (@PathParam("complexOper") ComplexOper oper) {
        try {
            return Utils.ok(operation.call(oper));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

}
