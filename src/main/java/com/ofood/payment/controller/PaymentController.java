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
                                                      @Valid @RequestBody PaymentConfirmationRequest request,
                                                      @AuthenticationPrincipal User customer) {
        return paymentService.confirmPayment(id, request, customer);
    }
}
