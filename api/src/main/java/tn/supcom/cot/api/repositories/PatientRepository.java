package tn.supcom.cot.api.repositories;

import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import tn.supcom.cot.api.entities.Patient;
import java.util.Optional;

@Repository
public interface PatientRepository extends CrudRepository<Patient, String> {
    @Find
    Optional<Patient> findByIdentityId(@By("identityId") String identityId);
}