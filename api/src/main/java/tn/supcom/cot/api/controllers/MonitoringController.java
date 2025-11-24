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

        if (record.getType() == SensorType.TEMPERATURE && record.getValue() > 39.0) {
            alertController.createAutomaticAlert(
                    record.getPatientId(),
                    "High Fever Detected: " + record.getValue()
            );
        }

        if (record.getType() == SensorType.HEART_RATE && record.getValue() > 130.0) {
            alertController.createAutomaticAlert(
                    record.getPatientId(),
                    "Tachycardia Detected: " + record.getValue()
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
