package tn.supcom.cot.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Identity implements RootEntity<String>, Principal {
    @Id
    private String id;

    @Column
    private long version = 0L;

    @Column
    private String username;

    @Column
    private String password;

    @Column
    private String email;

    @Column
    private String phoneNumber;

    @Column
    private LocalDate birthDate;

    @Column
    private long roles;

    @Column
    private String providedScopes;

    // --- NOUVEAUX CHAMPS POUR L'ACTIVATION ---
    @Column
    private boolean accountActivated = false;

    @Column
    private String activationCode;

    @Column
    private LocalDateTime activationCodeExpiresAt;
    // -----------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        if (this.version != version) {
            throw new IllegalStateException();
        }
        ++this.version;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getName() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public long getRoles() {
        return roles;
    }

    public void setRoles(long roles) {
        this.roles = roles;
    }

    public String getProvidedScopes() {
        return providedScopes;
    }

    public void setProvidedScopes(String providedScopes) {
        this.providedScopes = providedScopes;
    }

    // --- Getters/Setters pour Activation ---
    public boolean isAccountActivated() {
        return accountActivated;
    }

    public void setAccountActivated(boolean accountActivated) {
        this.accountActivated = accountActivated;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public LocalDateTime getActivationCodeExpiresAt() {
        return activationCodeExpiresAt;
    }

    public void setActivationCodeExpiresAt(LocalDateTime activationCodeExpiresAt) {
        this.activationCodeExpiresAt = activationCodeExpiresAt;
    }
}