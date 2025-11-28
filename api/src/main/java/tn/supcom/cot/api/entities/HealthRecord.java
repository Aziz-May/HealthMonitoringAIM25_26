package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.time.LocalDateTime;

/**
 * HealthRecord Entity
 *
 * Represents a single health measurement/reading from a sensor.
 * Examples: heart rate, blood pressure, temperature, SpO2, etc.
 */
@Entity
public class HealthRecord {

    @Id
    private String id;

    @Column
    private String patientId;

    @Column
    private String type; // HEART_RATE, BLOOD_PRESSURE, TEMPERATURE, SPO2, STEPS, SLEEP, etc.

    @Column
    private Double value;

    @Column
    private String unit; // bpm, mmHg, °C, %, steps, hours, etc.

    @Column
    private LocalDateTime timestamp;

    @Column
    private String sensorId; // Which sensor/device recorded this

    @Column
    private String notes;

    // For blood pressure (systolic/diastolic)
    @Column
    private Double secondaryValue;

    @Column
    private String secondaryUnit;

    // Constructors
    public HealthRecord() {
        this.timestamp = LocalDateTime.now();
    }

    public HealthRecord(String patientId, String type, Double value, String unit) {
        this();
        this.patientId = patientId;
        this.type = type;
        this.value = value;
        this.unit = unit;
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

    public Double getSecondaryValue() {
        return secondaryValue;
    }

    public void setSecondaryValue(Double secondaryValue) {
        this.secondaryValue = secondaryValue;
    }

    public String getSecondaryUnit() {
        return secondaryUnit;
    }

    public void setSecondaryUnit(String secondaryUnit) {
        this.secondaryUnit = secondaryUnit;
    }
}