package ru.transfer.services;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
    public Response operation() {
        return Response.status(200).header("content-type", "text/plain; charset=utf-8")
                .entity("DONE !!!").build();
    }
}
