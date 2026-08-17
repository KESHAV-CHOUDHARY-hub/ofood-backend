package com.ofood.catalog.repository;

import com.ofood.catalog.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findBySlug(String slug);
    List<Plan> findByStatusOrderByDisplayOrderAsc(String status);
    List<Plan> findAllByOrderByDisplayOrderAsc();
    boolean existsBySlug(String slug);
}
