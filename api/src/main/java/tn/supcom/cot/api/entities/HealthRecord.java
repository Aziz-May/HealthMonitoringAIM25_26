package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
public class HealthRecord implements Serializable {
    @Id
    private String id;

    @Column
    private String patientId;

    @Column
    private SensorType type;

    @Column
    private double value;

    @Column
    private String unit;

    @Column
    private LocalDateTime timestamp;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public SensorType getType() { return type; }
    public void setType(SensorType type) { this.type = type; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}