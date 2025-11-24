package tn.supcom.cot.api.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tn.supcom.cot.api.controllers.PatientController;
import tn.supcom.cot.api.entities.Patient;

@Path("/patients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    @Inject
    PatientController controller;

    @POST
    @RolesAllowed({"ADMIN", "DOCTOR", "FAMILY"})
    public Response createProfile(Patient patient) {
        Patient created = controller.create(patient);
        return Response.ok(created).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed({"PATIENT", "FAMILY"})
    public Response getMyProfile() {
        return controller.getMyProfile()
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }
}
