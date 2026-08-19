package com.ofood.checkout.controller;

import com.ofood.auth.model.User;
import com.ofood.checkout.dto.CheckoutPreviewRequest;
import com.ofood.checkout.dto.CheckoutPreviewResponse;
import com.ofood.checkout.dto.CheckoutResponse;
import com.ofood.checkout.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkout")
@Tag(name = "Checkout", description = "Checkout operations")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final com.ofood.auth.repository.UserRepository userRepository;

    public CheckoutController(CheckoutService checkoutService, com.ofood.auth.repository.UserRepository userRepository) {
        this.checkoutService = checkoutService;
        this.userRepository = userRepository;
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview checkout pricing")
    public CheckoutPreviewResponse preview(@Valid @RequestBody CheckoutPreviewRequest request,
                                           org.springframework.security.core.Authentication authentication) {
        User customer = resolveCustomer(authentication);
        return checkoutService.previewCheckout(request, customer);
    }

    @PostMapping
    @Operation(summary = "Process checkout and create payment/subscription")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutPreviewRequest request,
                                     org.springframework.security.core.Authentication authentication) {
        User customer = resolveCustomer(authentication);
        return checkoutService.checkout(request, customer);
    }

    private User resolveCustomer(org.springframework.security.core.Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return userRepository.findById(java.util.UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
