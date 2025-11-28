package tn.supcom.cot.api.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.nosql.document.DocumentTemplate;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import tn.supcom.cot.api.entities.Patient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Patient Resource
 *
 * REST endpoints for managing patient profiles.
 *
 * Base URL: /api/patients
 */
@Path("/patients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    @Inject
    private DocumentTemplate template;

    @Inject
    private JsonWebToken jwt;

    /**
     * Create a new patient profile
     *
     * POST /api/patients
     * Roles: ADMIN, DOCTOR, FAMILY
     */
    @POST
    @RolesAllowed({"ADMIN", "DOCTOR", "FAMILY"})
    public Response createPatient(Patient patient) {
        try {
            // Generate ID if not provided
            if (patient.getId() == null || patient.getId().isEmpty()) {
                patient.setId("patient-" + UUID.randomUUID().toString());
            }

            // Set timestamps
            patient.setCreatedAt(LocalDateTime.now());
            patient.setUpdatedAt(LocalDateTime.now());

            // Save to database
            Patient saved = template.insert(patient);

            return Response.status(Response.Status.CREATED)
                    .entity(saved)
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create patient: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get my patient profile (for logged-in patient)
     *
     * GET /api/patients/me
     * Roles: PATIENT, FAMILY
     */
    @GET
    @Path("/me")
    @RolesAllowed({"PATIENT", "FAMILY"})
    public Response getMyProfile(@Context SecurityContext securityContext) {
        try {
            String identityId = jwt.getSubject();

            // Find patient by identityId
            Optional<Patient> patient = template.select(Patient.class)
                    .where("identityId")
                    .eq(identityId)
                    .singleResult();

            if (patient.isPresent()) {
                return Response.ok(patient.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Patient profile not found"))
                        .build();
            }

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve profile: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get patient by ID
     *
     * GET /api/patients/{id}
     * Roles: DOCTOR, FAMILY, ADMIN
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({"DOCTOR", "FAMILY", "ADMIN"})
    public Response getPatient(@PathParam("id") String id) {
        try {
            Optional<Patient> patient = template.find(Patient.class, id);

            if (patient.isPresent()) {
                return Response.ok(patient.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Patient not found"))
                        .build();
            }

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve patient: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all patients
     *
     * GET /api/patients
     * Roles: DOCTOR, ADMIN
     */
    @GET
    @RolesAllowed({"DOCTOR", "ADMIN"})
    public Response getAllPatients() {
        try {
            List<Patient> patients = template.select(Patient.class).result();

            return Response.ok(patients).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve patients: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Update patient profile
     *
     * PUT /api/patients/{id}
     * Roles: DOCTOR, ADMIN
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed({"DOCTOR", "ADMIN"})
    public Response updatePatient(@PathParam("id") String id, Patient updatedPatient) {
        try {
            Optional<Patient> existing = template.find(Patient.class, id);

            if (existing.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Patient not found"))
                        .build();
            }

            Patient patient = existing.get();

            // Update fields
            if (updatedPatient.getFirstName() != null) {
                patient.setFirstName(updatedPatient.getFirstName());
            }
            if (updatedPatient.getLastName() != null) {
                patient.setLastName(updatedPatient.getLastName());
            }
            if (updatedPatient.getDateOfBirth() != null) {
                patient.setDateOfBirth(updatedPatient.getDateOfBirth());
            }
            if (updatedPatient.getAge() != null) {
                patient.setAge(updatedPatient.getAge());
            }
            if (updatedPatient.getGender() != null) {
                patient.setGender(updatedPatient.getGender());
            }
            if (updatedPatient.getBloodType() != null) {
                patient.setBloodType(updatedPatient.getBloodType());
            }
            if (updatedPatient.getEmergencyPhone() != null) {
                patient.setEmergencyPhone(updatedPatient.getEmergencyPhone());
            }
            if (updatedPatient.getMedicalHistory() != null) {
                patient.setMedicalHistory(updatedPatient.getMedicalHistory());
            }
            if (updatedPatient.getCurrentMedications() != null) {
                patient.setCurrentMedications(updatedPatient.getCurrentMedications());
            }
            if (updatedPatient.getAllergies() != null) {
                patient.setAllergies(updatedPatient.getAllergies());
            }

            patient.setUpdatedAt(LocalDateTime.now());

            Patient saved = template.update(patient);

            return Response.ok(saved).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to update patient: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Delete patient
     *
     * DELETE /api/patients/{id}
     * Roles: ADMIN only
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response deletePatient(@PathParam("id") String id) {
        try {
            template.delete(Patient.class, id);

            return Response.noContent().build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete patient: " + e.getMessage()))
                    .build();
        }
    }

    // Error response DTO
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}