package tn.supcom.cot.api.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.nosql.document.DocumentTemplate;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import tn.supcom.cot.api.entities.HealthRecord;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Monitoring Resource
 *
 * REST endpoints for submitting and retrieving health monitoring data.
 * Sensors submit readings, doctors/family view historical data.
 *
 * Base URL: /api/monitor
 */
@Path("/monitor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MonitoringResource {

    @Inject
    private DocumentTemplate template;

    @Inject
    private JsonWebToken jwt;

    /**
     * Submit sensor data
     *
     * POST /api/monitor
     * Roles: SENSOR, ADMIN
     *
     * Request body example:
     * {
     *   "patientId": "patient-profile-001",
     *   "type": "HEART_RATE",
     *   "value": 75.0,
     *   "unit": "bpm",
     *   "notes": "Normal reading"
     * }
     */
    @POST
    @RolesAllowed({"SENSOR", "ADMIN"})
    public Response submitHealthData(HealthRecord record) {
        try {
            // Validate required fields
            if (record.getPatientId() == null || record.getPatientId().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("patientId is required"))
                        .build();
            }
            if (record.getType() == null || record.getType().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("type is required"))
                        .build();
            }
            if (record.getValue() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("value is required"))
                        .build();
            }

            // Generate ID and set metadata
            if (record.getId() == null || record.getId().isEmpty()) {
                record.setId("health-record-" + UUID.randomUUID().toString());
            }
            record.setTimestamp(LocalDateTime.now());
            record.setSensorId(jwt.getSubject());

            // Save to database
            HealthRecord saved = template.insert(record);

            return Response.status(Response.Status.CREATED)
                    .entity(saved)
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to submit health data: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get health history for a patient by type
     *
     * GET /api/monitor/{patientId}/{type}
     * Roles: DOCTOR, FAMILY
     *
     * Example: GET /api/monitor/patient-profile-001/HEART_RATE
     */
    @GET
    @Path("/{patientId}/{type}")
    @RolesAllowed({"DOCTOR", "FAMILY", "ADMIN"})
    public Response getHealthHistory(
            @PathParam("patientId") String patientId,
            @PathParam("type") String type,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        try {
            List<HealthRecord> result = template.select(HealthRecord.class)
                    .where("patientId").eq(patientId)
                    .and("type").eq(type)
                    .result();

            List<HealthRecord> records = result.stream()
                    .sorted(Comparator.comparing(HealthRecord::getTimestamp).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            return Response.ok(records).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve health history: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all health data for a patient
     *
     * GET /api/monitor/{patientId}
     * Roles: DOCTOR, FAMILY, ADMIN
     */
    @GET
    @Path("/{patientId}")
    @RolesAllowed({"DOCTOR", "FAMILY", "ADMIN"})
    public Response getAllHealthData(
            @PathParam("patientId") String patientId,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        try {
            List<HealthRecord> result = template.select(HealthRecord.class)
                    .where("patientId").eq(patientId)
                    .result();

            List<HealthRecord> records = result.stream()
                    .sorted(Comparator.comparing(HealthRecord::getTimestamp).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            return Response.ok(records).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve health data: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get latest reading for a patient by type
     *
     * GET /api/monitor/{patientId}/{type}/latest
     * Roles: DOCTOR, FAMILY, PATIENT, ADMIN
     */
    @GET
    @Path("/{patientId}/{type}/latest")
    @RolesAllowed({"DOCTOR", "FAMILY", "PATIENT", "ADMIN"})
    public Response getLatestReading(
            @PathParam("patientId") String patientId,
            @PathParam("type") String type) {

        try {
            List<HealthRecord> result = template.select(HealthRecord.class)
                    .where("patientId").eq(patientId)
                    .and("type").eq(type)
                    .result();

            List<HealthRecord> records = result.stream()
                    .sorted(Comparator.comparing(HealthRecord::getTimestamp).reversed())
                    .limit(1)
                    .collect(Collectors.toList());

            if (records.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("No readings found"))
                        .build();
            }

            return Response.ok(records.get(0)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve latest reading: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get statistics for a health metric
     *
     * GET /api/monitor/{patientId}/{type}/stats
     * Roles: DOCTOR, FAMILY, ADMIN
     */
    @GET
    @Path("/{patientId}/{type}/stats")
    @RolesAllowed({"DOCTOR", "FAMILY", "ADMIN"})
    public Response getStatistics(
            @PathParam("patientId") String patientId,
            @PathParam("type") String type) {

        try {
            List<HealthRecord> records = template.select(HealthRecord.class)
                    .where("patientId").eq(patientId)
                    .and("type").eq(type)
                    .result();

            if (records.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("No data available for statistics"))
                        .build();
            }

            // Calculate statistics
            double avg = records.stream()
                    .mapToDouble(HealthRecord::getValue)
                    .average()
                    .orElse(0.0);

            double min = records.stream()
                    .mapToDouble(HealthRecord::getValue)
                    .min()
                    .orElse(0.0);

            double max = records.stream()
                    .mapToDouble(HealthRecord::getValue)
                    .max()
                    .orElse(0.0);

            HealthStats stats = new HealthStats(
                    type,
                    records.size(),
                    avg,
                    min,
                    max,
                    records.get(0).getUnit()
            );

            return Response.ok(stats).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to calculate statistics: " + e.getMessage()))
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

    public static class HealthStats {
        public String type;
        public int count;
        public double average;
        public double min;
        public double max;
        public String unit;

        public HealthStats() {}

        public HealthStats(String type, int count, double average, double min, double max, String unit) {
            this.type = type;
            this.count = count;
            this.average = average;
            this.min = min;
            this.max = max;
            this.unit = unit;
        }

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getAverage() { return average; }
        public void setAverage(double average) { this.average = average; }
        public double getMin() { return min; }
        public void setMin(double min) { this.min = min; }
        public double getMax() { return max; }
        public void setMax(double max) { this.max = max; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }
}