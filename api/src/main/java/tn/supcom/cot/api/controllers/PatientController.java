package tn.supcom.cot.api.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import tn.supcom.cot.api.entities.Patient;
import tn.supcom.cot.api.repositories.PatientRepository;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PatientController {

    @Inject
    PatientRepository repository;

    @Inject
    JsonWebToken jwt;

    public Patient create(Patient p) {

        // Assign identityId from JWT if not provided by frontend
        if (p.getIdentityId() == null) {
            p.setIdentityId(jwt.getSubject());
        }

        // Assign generated UUID if no ID given
        if (p.getId() == null) {
            p.setId(UUID.randomUUID().toString());
        }

        repository.save(p);
        return p;
    }

     // Return the profile of the logged-in patient or family member.
    public Optional<Patient> getMyProfile() {
        String identityId = jwt.getSubject();
        return repository.findByIdentityId(identityId);
    }
}
