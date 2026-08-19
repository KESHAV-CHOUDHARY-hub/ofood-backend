package com.ofood.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class PaymentConfirmationRequest {
    @NotBlank(message = "Provider payment ID is required")
    private String providerPaymentId;

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }
}
