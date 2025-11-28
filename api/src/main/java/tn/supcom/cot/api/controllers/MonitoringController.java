package tn.supcom.cot.api.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.supcom.cot.api.entities.HealthRecord;
import tn.supcom.cot.api.entities.SensorType;
import tn.supcom.cot.api.repositories.HealthRecordRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MonitoringController {

    @Inject
    HealthRecordRepository recordRepository;

    @Inject
    AlertController alertController;

    /*
     * Add new health record (HR data from sensors)
     */
    public void addRecord(HealthRecord record) {
        record.setId(UUID.randomUUID().toString());
        record.setTimestamp(LocalDateTime.now());

        checkThresholds(record);

        recordRepository.save(record);
    }

    /*
     * Apply threshold rules to detect critical events.
     */
    private void checkThresholds(HealthRecord record) {
        // Option 1: Compare strings directly
        if ("TEMPERATURE".equals(record.getType()) && record.getValue() > 39.0) {
            alertController.createAutomaticAlert(
                    record.getPatientId(),
                    "High Fever Detected: " + record.getValue() + "°C"
            );
        }

        if ("HEART_RATE".equals(record.getType()) && record.getValue() > 130.0) {
            alertController.createAutomaticAlert(
                    record.getPatientId(),
                    "Tachycardia Detected: " + record.getValue() + " bpm"
            );
        }

        // Additional thresholds
        if ("SPO2".equals(record.getType()) && record.getValue() < 90.0) {
            alertController.createAutomaticAlert(
                    record.getPatientId(),
                    "Low Oxygen Saturation: " + record.getValue() + "%"
            );
        }

        if ("BLOOD_PRESSURE".equals(record.getType()) && record.getValue() > 180.0) {
            alertController.createAutomaticAlert(
                    record.getPatientId(),
                    "High Blood Pressure: " + record.getValue() + " mmHg"
            );
        }
    }

    /*
     * Historical monitoring data for UI dashboard.
     */
    public List<HealthRecord> getHistory(String patientId, SensorType type) {
        return recordRepository.findByPatientIdAndType(patientId, type);
    }
}