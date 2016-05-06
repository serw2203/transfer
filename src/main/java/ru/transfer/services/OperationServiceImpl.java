package ru.transfer.services;

import ru.transfer.model.Operation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/")
public class OperationServiceImpl {

    @GET
    @Path("/operation/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes ({MediaType.APPLICATION_JSON})
    public Response operation(Operation operation) {
        return Response.status(200).header("content-type", "text/plain; charset=utf-8")
                .entity(operation).build();
    }
}
