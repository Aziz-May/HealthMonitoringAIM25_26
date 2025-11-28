package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.time.LocalDateTime;

/**
 * Alert Entity
 *
 * Represents an emergency alert or health warning.
 * Created when sensor readings exceed safe thresholds.
 */
@Entity
public class Alert {

    @Id
    private String id;

    @Column
    private String patientId;

    @Column
    private String type; // HIGH_TEMPERATURE, LOW_SPO2, IRREGULAR_HEARTBEAT, FALL_DETECTED, etc.

    @Column
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column
    private String message;

    @Column
    private Double value;

    @Column
    private String unit;

    @Column
    private LocalDateTime timestamp;

    @Column
    private Boolean resolved;

    @Column
    private LocalDateTime resolvedAt;

    @Column
    private String resolvedBy; // Identity ID of person who resolved it

    @Column
    private String sensorId;

    @Column
    private String notes;

    // Constructors
    public Alert() {
        this.timestamp = LocalDateTime.now();
        this.resolved = false;
    }

    public Alert(String patientId, String type, String severity, String message) {
        this();
        this.patientId = patientId;
        this.type = type;
        this.severity = severity;
        this.message = message;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void resolve(String resolvedBy) {
        this.resolved = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
    }
}