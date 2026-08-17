package com.ofood.customer.repository;

import com.ofood.customer.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    
    List<Address> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    
    Optional<Address> findByIdAndCustomerId(UUID id, UUID customerId);
    
    boolean existsByCustomerId(UUID customerId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customer.id = :customerId AND a.isDefault = true")
    void clearDefaultAddressForCustomer(UUID customerId);
}
