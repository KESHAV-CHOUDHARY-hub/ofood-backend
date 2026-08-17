package com.ofood.catalog.controller;

import com.ofood.catalog.dto.PlanRequest;
import com.ofood.catalog.dto.PlanResponse;
import com.ofood.catalog.dto.ReorderPlansRequest;
import com.ofood.catalog.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<PlanResponse>> getActivePlans() {
        return ResponseEntity.ok(planService.getActivePlans());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<PlanResponse>> getAllPlansForAdmin() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getActivePlan(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.getActivePlanById(id));
    }

    @GetMapping("/{id}/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PlanResponse> getPlanForAdmin(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.getPlanByIdForAdmin(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<PlanResponse> getActivePlanBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(planService.getActivePlanBySlug(slug));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.createPlan(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PlanResponse> duplicatePlan(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.duplicatePlan(id));
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> reorderPlans(@Valid @RequestBody ReorderPlansRequest request) {
        planService.reorderPlans(request);
        return ResponseEntity.ok().build();
    }
}
