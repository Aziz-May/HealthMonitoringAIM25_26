package tn.supcom.cot.api.repositories;

import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import tn.supcom.cot.api.entities.HealthRecord;
import tn.supcom.cot.api.entities.SensorType;
import java.util.List;

@Repository
public interface HealthRecordRepository extends CrudRepository<HealthRecord, String> {
    @Find
    List<HealthRecord> findByPatientIdAndType(@By("patientId") String patientId,
                                              @By("type") SensorType type);
}