package tn.supcom.cot.api.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.supcom.cot.api.entities.Alert;
import tn.supcom.cot.api.repositories.AlertRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AlertController {

    @Inject
    AlertRepository repository;

    public void report(Alert alert) {
        alert.setId(UUID.randomUUID().toString());
        alert.setTimestamp(LocalDateTime.now());
        alert.setResolved(false);
        alert.setSeverity("HIGH");

        repository.save(alert);
    }

    public void createAutomaticAlert(String patientId, String message) {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID().toString());
        alert.setPatientId(patientId);
        alert.setMessage(message);
        alert.setTimestamp(LocalDateTime.now());
        alert.setSeverity("HIGH");
        alert.setResolved(false);

        repository.save(alert);
    }

    public List<Alert> getActive() {
        return repository.findByResolved(false);
    }
}
