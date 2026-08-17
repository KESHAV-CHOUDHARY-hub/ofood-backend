package com.ofood.location.repository;

import com.ofood.location.model.ServicePincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicePincodeRepository extends JpaRepository<ServicePincode, UUID> {
    Optional<ServicePincode> findByPincode(String pincode);
    List<ServicePincode> findByCityId(UUID cityId);
    boolean existsByPincode(String pincode);
    List<ServicePincode> findByStatus(String status);
}
