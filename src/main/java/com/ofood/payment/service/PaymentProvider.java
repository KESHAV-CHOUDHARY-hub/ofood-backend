package com.ofood.payment.service;

import java.math.BigDecimal;

public interface PaymentProvider {
    
    /**
     * Creates a payment intent with the provider.
     * @param amount the total final amount to charge
     * @param currency the currency
     * @param internalPaymentId our system's internal payment ID
     * @return the provider's payment ID
     */
    String createPaymentIntent(BigDecimal amount, String currency, String internalPaymentId);
    
    /**
     * Verifies the payment with the provider.
     * @param providerPaymentId the provider's payment ID
     * @return true if the payment was successful, false otherwise
     */
    boolean verifyPayment(String providerPaymentId);
}
