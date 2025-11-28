package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Patient Entity
 *
 * Represents a patient profile in the health monitoring system.
 * Linked to an Identity (user account) via identityId.
 */
@Entity
public class Patient {

    @Id
    private String id;

    @Column
    private String identityId;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private Integer age;

    @Column
    private String gender; // MALE, FEMALE, OTHER

    @Column
    private String bloodType; // A+, A-, B+, B-, AB+, AB-, O+, O-

    @Column
    private String emergencyPhone;

    @Column
    private List<String> medicalHistory = new ArrayList<>();

    @Column
    private List<String> currentMedications = new ArrayList<>();

    @Column
    private List<String> allergies = new ArrayList<>();

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Constructors
    public Patient() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Patient(String identityId, String firstName, String lastName) {
        this();
        this.identityId = identityId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }

    public List<String> getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(List<String> medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public List<String> getCurrentMedications() {
        return currentMedications;
    }

    public void setCurrentMedications(List<String> currentMedications) {
        this.currentMedications = currentMedications;
    }

    public List<String> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<String> allergies) {
        this.allergies = allergies;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}