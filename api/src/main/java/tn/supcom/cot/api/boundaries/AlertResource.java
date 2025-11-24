package tn.supcom.cot.api.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.supcom.cot.api.entities.Alert;
import tn.supcom.cot.api.repositories.AlertRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertResource {

    @Inject
    private AlertRepository repository;

    // Report a Fall/Emergency (Called by AI/Device)
    @POST
    @RolesAllowed({"SENSOR", "ADMIN"})
    public Response reportEmergency(Alert alert) {
        alert.setId(UUID.randomUUID().toString());
        alert.setTimestamp(LocalDateTime.now());
        alert.setResolved(false);
        alert.setSeverity("HIGH");

        repository.save(alert);

        System.out.println("!!! EMERGENCY REPORTED: " + alert.getMessage());
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @RolesAllowed({"DOCTOR", "FAMILY"})
    public Response getActiveAlerts() {
        return Response.ok(repository.findByResolved(false)).build();
    }
}