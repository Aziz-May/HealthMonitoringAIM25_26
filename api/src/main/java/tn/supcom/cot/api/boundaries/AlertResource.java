package tn.supcom.cot.api.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.nosql.document.DocumentTemplate;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import tn.supcom.cot.api.entities.Alert;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Alert Resource
 *
 * REST endpoints for managing emergency alerts and health warnings.
 * Sensors create alerts, doctors/family view and resolve them.
 *
 * Base URL: /api/alerts
 */
@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertResource {

    @Inject
    private DocumentTemplate template;

    @Inject
    private JsonWebToken jwt;

    /**
     * Create a new alert
     *
     * POST /api/alerts
     * Roles: SENSOR, ADMIN
     *
     * Request body example:
     * {
     *   "patientId": "patient-profile-001",
     *   "type": "HIGH_TEMPERATURE",
     *   "severity": "HIGH",
     *   "message": "Patient temperature elevated: 39.5°C",
     *   "value": 39.5,
     *   "unit": "°C"
     * }
     */
    @POST
    @RolesAllowed({"SENSOR", "ADMIN"})
    public Response createAlert(Alert alert) {
        try {
            // Validate required fields
            if (alert.getPatientId() == null || alert.getPatientId().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("patientId is required"))
                        .build();
            }
            if (alert.getType() == null || alert.getType().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("type is required"))
                        .build();
            }
            if (alert.getSeverity() == null || alert.getSeverity().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("severity is required"))
                        .build();
            }

            // Generate ID and set metadata
            if (alert.getId() == null || alert.getId().isEmpty()) {
                alert.setId("alert-" + UUID.randomUUID());
            }
            alert.setTimestamp(LocalDateTime.now());
            alert.setSensorId(jwt.getSubject());
            alert.setResolved(false);

            // Save to database
            Alert saved = template.insert(alert);

            return Response.status(Response.Status.CREATED)
                    .entity(saved)
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create alert: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get active (unresolved) alerts
     *
     * GET /api/alerts
     * Roles: DOCTOR, FAMILY, ADMIN
     */
    @GET
    @RolesAllowed({"DOCTOR", "FAMILY", "ADMIN"})
    public Response getActiveAlerts(@QueryParam("patientId") String patientId) {
        try {
            List<Alert> alerts;

            if (patientId != null && !patientId.isEmpty()) {
                // Get active alerts for specific patient
                List<Alert> result = template.select(Alert.class)
                        .where("patientId").eq(patientId)
                        .and("resolved").eq(false)
                        .result();

                alerts = result.stream()
                        .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                        .collect(Collectors.toList());
            } else {
                // Get all active alerts
                List<Alert> result = template.select(Alert.class)
                        .where("resolved").eq(false)
                        .result();

                alerts = result.stream()
                        .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                        .collect(Collectors.toList());
            }

            return Response.ok(alerts).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alerts: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get alert by ID
     *
     * GET /api/alerts/{id}
     * Roles: DOCTOR, FAMILY, ADMIN
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({"DOCTOR", "FAMILY", "ADMIN"})
    public Response getAlert(@PathParam("id") String id) {
        try {
            Optional<Alert> alert = template.find(Alert.class, id);

            if (alert.isPresent()) {
                return Response.ok(alert.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Alert not found"))
                        .build();
            }

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alert: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all alerts for a patient (including resolved)
     *
     * GET /api/alerts/patient/{patientId}
     * Roles: DOCTOR, FAMILY, ADMIN
     */
    @GET
    @Path("/patient/{patientId}")
    @RolesAllowed({"DOCTOR", "FAMILY", "ADMIN"})
    public Response getPatientAlerts(
            @PathParam("patientId") String patientId,
            @QueryParam("limit") @DefaultValue("50") int limit) {

        try {
            List<Alert> result = template.select(Alert.class)
                    .where("patientId").eq(patientId)
                    .result();

            List<Alert> alerts = result.stream()
                    .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            return Response.ok(alerts).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve patient alerts: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Resolve an alert
     *
     * PATCH /api/alerts/{id}/resolve
     * Roles: DOCTOR, ADMIN
     *
     * Request body (optional):
     * {
     *   "notes": "Patient condition stabilized"
     * }
     */
    @PATCH
    @Path("/{id}/resolve")
    @RolesAllowed({"DOCTOR", "ADMIN"})
    public Response resolveAlert(@PathParam("id") String id, ResolveRequest request) {
        try {
            Optional<Alert> optionalAlert = template.find(Alert.class, id);

            if (optionalAlert.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Alert not found"))
                        .build();
            }

            Alert alert = optionalAlert.get();

            if (alert.getResolved()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Alert already resolved"))
                        .build();
            }

            // Resolve the alert
            alert.resolve(jwt.getSubject());

            if (request != null && request.getNotes() != null) {
                alert.setNotes(request.getNotes());
            }

            Alert updated = template.update(alert);

            return Response.ok(updated).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to resolve alert: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get alerts by severity
     *
     * GET /api/alerts/severity/{severity}
     * Roles: DOCTOR, FAMILY, ADMIN
     *
     * Severity: LOW, MEDIUM, HIGH, CRITICAL
     */
    @GET
    @Path("/severity/{severity}")
    @RolesAllowed({"DOCTOR", "FAMILY", "ADMIN"})
    public Response getAlertsBySeverity(
            @PathParam("severity") String severity,
            @QueryParam("resolved") @DefaultValue("false") boolean includeResolved) {

        try {
            List<Alert> result;

            if (includeResolved) {
                result = template.select(Alert.class)
                        .where("severity").eq(severity.toUpperCase())
                        .result();
            } else {
                result = template.select(Alert.class)
                        .where("severity").eq(severity.toUpperCase())
                        .and("resolved").eq(false)
                        .result();
            }

            List<Alert> alerts = result.stream()
                    .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                    .collect(Collectors.toList());

            return Response.ok(alerts).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alerts: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Delete an alert (admin only)
     *
     * DELETE /api/alerts/{id}
     * Roles: ADMIN
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response deleteAlert(@PathParam("id") String id) {
        try {
            template.delete(Alert.class, id);

            return Response.noContent().build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete alert: " + e.getMessage()))
                    .build();
        }
    }

    // DTOs
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

    public static class ResolveRequest {
        private String notes;

        public ResolveRequest() {}

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}