package com.ofood.location.controller;

import com.ofood.location.service.ServiceabilityService;
import com.ofood.location.service.ServiceabilityService.ServiceabilityResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/serviceability")
public class ServiceabilityController {
    private final ServiceabilityService serviceabilityService;

    public ServiceabilityController(ServiceabilityService serviceabilityService) {
        this.serviceabilityService = serviceabilityService;
    }

    @GetMapping
    public ResponseEntity<ServiceabilityResponse> checkServiceability(@RequestParam String pincode) {
        return ResponseEntity.ok(serviceabilityService.checkServiceability(pincode));
    }
}
