package tn.supcom.cot.api.repositories;

import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import tn.supcom.cot.api.entities.Alert;
import java.util.List;

@Repository
public interface AlertRepository extends CrudRepository<Alert, String> {
    @Find
    List<Alert> findByPatientId(@By("patientId") String patientId);

    @Find
    List<Alert> findByResolved(@By("resolved") boolean resolved);
}