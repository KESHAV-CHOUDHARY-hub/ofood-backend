package com.ofood.customer.controller;

import com.ofood.customer.dto.AddressRequest;
import com.ofood.customer.dto.AddressResponse;
import com.ofood.customer.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    private UUID extractCustomerId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        return ResponseEntity.ok(addressService.getCustomerAddresses(customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(@PathVariable UUID id, Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        return ResponseEntity.ok(addressService.getAddressById(id, customerId));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createAddress(customerId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        return ResponseEntity.ok(addressService.updateAddress(id, customerId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID id, Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        addressService.deleteAddress(id, customerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<AddressResponse> markAsDefault(@PathVariable UUID id, Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        return ResponseEntity.ok(addressService.markAsDefault(id, customerId));
    }
}
