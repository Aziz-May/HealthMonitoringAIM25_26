package tn.supcom.cot.iam.controllers.repositories;

import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.GrantPK;


import java.util.List;
import java.util.Optional;

@Repository
public interface GrantRepository extends CrudRepository<Grant, GrantPK> {

}
