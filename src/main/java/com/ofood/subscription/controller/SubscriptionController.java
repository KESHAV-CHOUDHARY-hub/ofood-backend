package com.ofood.subscription.controller;

import com.ofood.auth.model.User;
import com.ofood.subscription.dto.SubscriptionResponse;
import com.ofood.subscription.model.Subscription;
import com.ofood.subscription.repository.SubscriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscription operations")
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionController(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping
    @Operation(summary = "Get all subscriptions for the authenticated customer")
    public List<SubscriptionResponse> getSubscriptions() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        UUID customerId = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            customerId = UUID.fromString(jwt.getSubject());
        }
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return subscriptionRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subscription by ID")
    public SubscriptionResponse getSubscription(@PathVariable UUID id) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        UUID customerId = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            customerId = UUID.fromString(jwt.getSubject());
        }
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (!subscription.getCustomer().getId().equals(customerId)) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Subscription does not belong to the authenticated user");
        }

        return toResponse(subscription);
    }
    
    private SubscriptionResponse toResponse(Subscription subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(subscription.getId());
        response.setStatus(subscription.getStatus());
        response.setPlanId(subscription.getPlan() != null ? subscription.getPlan().getId() : null);
        response.setAddressId(subscription.getAddress() != null ? subscription.getAddress().getId() : null);
        response.setStartDate(subscription.getStartDate());
        response.setEndDate(subscription.getEndDate());
        response.setPrice(subscription.getPrice());
        response.setPlanDiscount(subscription.getPlanDiscount());
        response.setVoucherDiscount(subscription.getVoucherDiscount());
        response.setTax(subscription.getTax());
        response.setDeliveryFee(subscription.getDeliveryFee());
        response.setFinalAmount(subscription.getFinalAmount());
        response.setCreatedAt(subscription.getCreatedAt());
        return response;
    }
}
