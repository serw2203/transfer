package ru.transfer.face;

import ru.transfer.model.Error;
import ru.transfer.service.AnalyticalService;
import ru.transfer.service.IAnalyticalService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigInteger;

/**
 *
 */
@Path("/service")
public class Face {
    private final IAnalyticalService analytical = new AnalyticalService();

    private Response responseError (Exception e) {
        Error error = new Error();
        error.setError(e.getMessage());
        return Response.status(500).header("content-type", "application/json; charset=utf-8")
                .entity(error).build();
    }

    @GET
    @Path("/input")
    @Produces({MediaType.APPLICATION_JSON})
    public Response input(@QueryParam("account") String account) {
        return Response.status(200).header("content-type", "application/json; charset=utf-8")
                .entity("DONE !!!").build();
    }

    @GET
    @Path("/currency")
    @Produces({MediaType.APPLICATION_JSON})
    public Response currency() {
        try {
            return Response.status(200).header("content-type", "application/json; charset=utf-8")
                    .entity(analytical.currencies()).build();
        } catch (Exception e) {
            return responseError(e);
        }
    }

    @GET
    @Path("/client/{client_id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response client(@PathParam("client_id") BigInteger client_id) {
        try {
            return Response.status(200).header("content-type", "application/json; charset=utf-8")
                    .entity(analytical.client(client_id)).build();
        } catch (Exception e) {
            return responseError(e);
        }
    }

    @GET
    @Path("/accounts/{client_id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response accounts(@PathParam("client_id") BigInteger client_id) {
        try {
            return Response.status(200).header("content-type", "application/json; charset=utf-8")
                    .entity(analytical.accounts(client_id)).build();
        } catch (Exception e) {
            return responseError(e);
        }
    }
}
