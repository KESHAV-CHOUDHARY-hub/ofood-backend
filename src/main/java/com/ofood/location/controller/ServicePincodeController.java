package com.ofood.location.controller;

import com.ofood.location.dto.ServicePincodeRequest;
import com.ofood.location.dto.ServicePincodeResponse;
import com.ofood.location.service.ServicePincodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pincodes")
public class ServicePincodeController {
    private final ServicePincodeService pincodeService;

    public ServicePincodeController(ServicePincodeService pincodeService) {
        this.pincodeService = pincodeService;
    }

    @GetMapping
    public ResponseEntity<List<ServicePincodeResponse>> getPincodes() {
        return ResponseEntity.ok(pincodeService.getActivePincodes());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ServicePincodeResponse>> getAllPincodes() {
        return ResponseEntity.ok(pincodeService.getAllPincodes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ServicePincodeResponse> getPincode(@PathVariable UUID id) {
        return ResponseEntity.ok(pincodeService.getPincodeById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ServicePincodeResponse> createPincode(@Valid @RequestBody ServicePincodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pincodeService.createPincode(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ServicePincodeResponse> updatePincode(@PathVariable UUID id, @Valid @RequestBody ServicePincodeRequest request) {
        return ResponseEntity.ok(pincodeService.updatePincode(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePincode(@PathVariable UUID id) {
        pincodeService.deletePincode(id);
        return ResponseEntity.noContent().build();
    }
}
