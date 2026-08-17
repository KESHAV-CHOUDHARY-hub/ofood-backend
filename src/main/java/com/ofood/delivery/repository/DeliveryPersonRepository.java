package com.ofood.delivery.repository;

import com.ofood.delivery.model.DeliveryPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, UUID> {
    boolean existsByMobile(String mobile);
}
