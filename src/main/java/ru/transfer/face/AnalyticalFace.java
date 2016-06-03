package ru.transfer.face;

import ru.transfer.service.AnalyticalService;
import ru.transfer.service.AnalyticalServiceImpl;
import ru.transfer.util.Utils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;

/**
 *
 */
@Path("/analytical")
public class AnalyticalFace {
    private final AnalyticalService analytical = new AnalyticalServiceImpl();

    @GET
    @Path("/currency")
    @Produces({MediaType.APPLICATION_JSON})
    public Response currency() {
        try {
            return Utils.ok(analytical.currencies());
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/client/{client_id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response client(@PathParam("client_id") Long clientId) {
        try {
            return Utils.ok(analytical.client(clientId));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/accounts/{client_id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response accounts(@PathParam("client_id") Long clientId) {
        try {
            return Utils.ok(analytical.accounts(clientId));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/account/{acc_num}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response account(@PathParam("acc_num") String accNum) {
        try {
            return Utils.ok(analytical.account(accNum));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/rate/{scur}/{tcur}/{date}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response rate(@PathParam("scur") String sCur,
                         @PathParam("tcur") String tCur,
                         @PathParam("date") Timestamp date) {
        try {
            return Utils.ok(analytical.rate(sCur, tCur, date));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/rates/{date}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response rates(@PathParam("date") Timestamp date) {
        try {
            return Utils.ok(analytical.rates(date));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/balance/{acc_num}/{date}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response balance(@PathParam("acc_num") String accNum,
                            @PathParam("date") Timestamp date) {
        try {
            return Utils.ok(analytical.balance(accNum, date));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }

    @GET
    @Path("/extracts/{acc_num}/{start_date}/{stop_date}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response extracts(@PathParam("acc_num") String accNum,
                             @PathParam("start_date") Timestamp startDate,
                             @PathParam("stop_date") Timestamp stopDate) {
        try {
            return Utils.ok(analytical.extracts(accNum, startDate, stopDate));
        } catch (Exception e) {
            return Utils.error(e);
        }
    }
}
