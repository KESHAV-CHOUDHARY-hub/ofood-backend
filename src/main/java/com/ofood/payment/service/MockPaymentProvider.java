package com.ofood.payment.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String createPaymentIntent(BigDecimal amount, String currency, String internalPaymentId) {
        // Return a mock provider payment ID
        return "mock_pi_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public boolean verifyPayment(String providerPaymentId) {
        // In a real provider, this would call out to Stripe/Razorpay API to check status.
        // For the mock, we assume all well-formed mock IDs are successful.
        return providerPaymentId != null && providerPaymentId.startsWith("mock_pi_");
    }
}
