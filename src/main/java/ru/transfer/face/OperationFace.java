package ru.transfer.face;

import ru.transfer.model.Account;
import ru.transfer.model.Client;
import ru.transfer.model.ComplexOper;
import ru.transfer.model.Operation;
import ru.transfer.service.OperationService;
import ru.transfer.service.OperationServiceImpl;
import ru.transfer.util.Utils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/operation")
public class OperationFace {
    private final OperationService operation = new OperationServiceImpl();

    @POST
    @Path("/addClient")
    @Produces({MediaType.APPLICATION_JSON})
    public Response addClient(Client client) {
        try {
            return Utils.ok(operation.addClient(client));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @POST
    @Path("/addAccount")
    @Produces({MediaType.APPLICATION_JSON})
    public Response addAccount(Account account) {
        try {
            return Utils.ok(operation.addAccount(account));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @POST
    @Path("/addOperation")
    @Produces({MediaType.APPLICATION_JSON})
    public Response addOperation (Operation oper) {
        try {
            return Utils.ok(operation.call(oper));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @POST
    @Path("/addOperations")
    @Produces({MediaType.APPLICATION_JSON})
    public Response addOperations (ComplexOper oper) {
        try {
            return Utils.ok(operation.call(oper));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

}
