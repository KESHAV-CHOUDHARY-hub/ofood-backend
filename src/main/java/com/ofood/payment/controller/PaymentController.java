package com.ofood.payment.controller;

import com.ofood.auth.model.User;
import com.ofood.payment.dto.PaymentConfirmationRequest;
import com.ofood.payment.dto.PaymentConfirmationResponse;
import com.ofood.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment operations")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm a payment")
    public PaymentConfirmationResponse confirmPayment(@PathVariable UUID id,
                                                      @Valid @RequestBody PaymentConfirmationRequest request) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        User customer = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            UUID customerId = UUID.fromString(jwt.getSubject());
            customer = new User();
            customer.setId(customerId);
        }
        return paymentService.confirmPayment(id, request, customer);
    }
}
