package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
public class Alert implements Serializable {
    @Id
    private String id;

    @Column
    private String patientId;

    @Column
    private String message;

    @Column
    private String severity; // HIGH, MEDIUM, LOW

    @Column
    private boolean resolved;

    @Column
    private LocalDateTime timestamp;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}