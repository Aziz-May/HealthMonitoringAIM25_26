package tn.supcom.cot.api.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import tn.supcom.cot.api.entities.Patient;
import tn.supcom.cot.api.repositories.PatientRepository;
import java.util.UUID;

@Path("/patients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    @Inject
    private PatientRepository repository;

    @Inject
    private JsonWebToken jwt;

    @POST
    @RolesAllowed({"ADMIN", "DOCTOR", "FAMILY"})
    public Response createProfile(Patient patient) {
        if (patient.getIdentityId() == null) {
            patient.setIdentityId(jwt.getSubject());
        }
        if (patient.getId() == null) {
            patient.setId(UUID.randomUUID().toString());
        }
        repository.save(patient);
        return Response.ok(patient).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed({"PATIENT", "FAMILY"})
    public Response getMyProfile() {
        var profile = repository.findByIdentityId(jwt.getSubject());
        if (profile.isPresent()) {
            return Response.ok(profile.get()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}