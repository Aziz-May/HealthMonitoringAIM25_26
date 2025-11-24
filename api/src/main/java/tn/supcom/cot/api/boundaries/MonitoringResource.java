package tn.supcom.cot.api.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.supcom.cot.api.entities.HealthRecord;
import tn.supcom.cot.api.entities.SensorType;
import tn.supcom.cot.api.entities.Alert;
import tn.supcom.cot.api.repositories.HealthRecordRepository;
import tn.supcom.cot.api.repositories.AlertRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Path("/monitor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MonitoringResource {

    @Inject
    private HealthRecordRepository recordRepository;

    @Inject
    private AlertRepository alertRepository;

    // 1. Ingest Data (Called by Devices or MQTT Worker)
    @POST
    @RolesAllowed({"SENSOR", "ADMIN"})
    public Response addRecord(HealthRecord record) {
        record.setId(UUID.randomUUID().toString());
        record.setTimestamp(LocalDateTime.now());

        // Simple Threshold Logic for Immediate Alerts
        checkThresholds(record);

        recordRepository.save(record);
        return Response.status(Response.Status.CREATED).build();
    }

    // 2. Read Data (Called by Frontend PWA)
    @GET
    @Path("/{patientId}/{type}")
    @RolesAllowed({"DOCTOR", "FAMILY"})
    public Response getHistory(@PathParam("patientId") String patientId,
                               @PathParam("type") String typeStr) {
        try {
            SensorType type = SensorType.valueOf(typeStr.toUpperCase());
            var data = recordRepository.findByPatientIdAndType(patientId, type);
            return Response.ok(data).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Sensor Type").build();
        }
    }

    private void checkThresholds(HealthRecord record) {
        if (record.getType() == SensorType.TEMPERATURE && record.getValue() > 39.0) {
            createAlert(record.getPatientId(), "High Fever Detected: " + record.getValue());
        }
        if (record.getType() == SensorType.HEART_RATE && record.getValue() > 130.0) {
            createAlert(record.getPatientId(), "Tachycardia Detected: " + record.getValue());
        }
    }

    private void createAlert(String patientId, String message) {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID().toString());
        alert.setPatientId(patientId);
        alert.setMessage(message);
        alert.setSeverity("HIGH");
        alert.setResolved(false);
        alert.setTimestamp(LocalDateTime.now());
        alertRepository.save(alert);
    }
}