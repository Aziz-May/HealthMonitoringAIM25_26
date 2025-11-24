package tn.supcom.cot.api.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.supcom.cot.api.controllers.AlertController;
import tn.supcom.cot.api.entities.Alert;

@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertResource {

    @Inject
    AlertController controller;

    @POST
    @RolesAllowed({"SENSOR", "ADMIN"})
    public Response reportEmergency(Alert alert) {
        controller.report(alert);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @RolesAllowed({"DOCTOR", "FAMILY"})
    public Response getActiveAlerts() {
        return Response.ok(controller.getActive()).build();
    }
}
