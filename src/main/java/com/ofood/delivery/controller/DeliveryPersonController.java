package com.ofood.delivery.controller;

import com.ofood.delivery.dto.DeliveryPersonRequest;
import com.ofood.delivery.dto.DeliveryPersonResponse;
import com.ofood.delivery.service.DeliveryPersonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery-persons")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class DeliveryPersonController {
    private final DeliveryPersonService deliveryPersonService;

    public DeliveryPersonController(DeliveryPersonService deliveryPersonService) {
        this.deliveryPersonService = deliveryPersonService;
    }

    @GetMapping
    public ResponseEntity<List<DeliveryPersonResponse>> getDeliveryPersons() {
        return ResponseEntity.ok(deliveryPersonService.getAllDeliveryPersons());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryPersonResponse> getDeliveryPerson(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryPersonService.getDeliveryPersonById(id));
    }

    @PostMapping
    public ResponseEntity<DeliveryPersonResponse> createDeliveryPerson(@Valid @RequestBody DeliveryPersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryPersonService.createDeliveryPerson(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryPersonResponse> updateDeliveryPerson(@PathVariable UUID id, @Valid @RequestBody DeliveryPersonRequest request) {
        return ResponseEntity.ok(deliveryPersonService.updateDeliveryPerson(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeliveryPerson(@PathVariable UUID id) {
        deliveryPersonService.deleteDeliveryPerson(id);
        return ResponseEntity.noContent().build();
    }
}
