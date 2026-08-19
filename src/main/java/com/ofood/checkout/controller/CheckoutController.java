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

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview checkout pricing")
    public CheckoutPreviewResponse preview(@Valid @RequestBody CheckoutPreviewRequest request,
                                           @AuthenticationPrincipal User customer) {
        return checkoutService.previewCheckout(request, customer);
    }

    @PostMapping
    @Operation(summary = "Process checkout and create payment/subscription")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutPreviewRequest request,
                                     @AuthenticationPrincipal User customer) {
        return checkoutService.checkout(request, customer);
    }
}
