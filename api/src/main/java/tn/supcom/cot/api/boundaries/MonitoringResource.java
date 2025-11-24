package tn.supcom.cot.api.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tn.supcom.cot.api.controllers.MonitoringController;
import tn.supcom.cot.api.entities.HealthRecord;
import tn.supcom.cot.api.entities.SensorType;

@Path("/monitor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MonitoringResource {

    @Inject
    MonitoringController controller;

    @POST
    @RolesAllowed({"SENSOR", "ADMIN"})
    public Response addRecord(HealthRecord record) {
        controller.addRecord(record);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/{patientId}/{type}")
    @RolesAllowed({"DOCTOR", "FAMILY"})
    public Response getHistory(
            @PathParam("patientId") String patientId,
            @PathParam("type") String typeStr
    ) {
        try {
            SensorType type = SensorType.valueOf(typeStr.toUpperCase());
            return Response.ok(controller.getHistory(patientId, type)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid Sensor Type")
                    .build();
        }
    }
}
