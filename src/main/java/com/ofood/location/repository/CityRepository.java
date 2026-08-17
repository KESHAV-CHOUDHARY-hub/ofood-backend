package com.ofood.location.repository;

import com.ofood.location.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<City, UUID> {
    Optional<City> findBySlug(String slug);
    List<City> findByStatus(String status);
    boolean existsBySlug(String slug);
}
