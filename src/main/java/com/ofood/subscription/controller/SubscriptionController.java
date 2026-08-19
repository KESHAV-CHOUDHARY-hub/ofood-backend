package com.ofood.subscription.controller;

import com.ofood.auth.model.User;
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
    public List<Subscription> getSubscriptions() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        User customer = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            UUID customerId = UUID.fromString(jwt.getSubject());
            customer = new User();
            customer.setId(customerId);
        }
        final User finalCustomer = customer;
        return subscriptionRepository.findAll().stream()
                .filter(s -> s.getCustomer().getId().equals(finalCustomer.getId()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subscription by ID")
    public Subscription getSubscription(@PathVariable UUID id) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        User customer = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            UUID customerId = UUID.fromString(jwt.getSubject());
            customer = new User();
            customer.setId(customerId);
        }
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (!subscription.getCustomer().getId().equals(customer.getId())) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Subscription does not belong to the authenticated user");
        }

        return subscription;
    }
}
