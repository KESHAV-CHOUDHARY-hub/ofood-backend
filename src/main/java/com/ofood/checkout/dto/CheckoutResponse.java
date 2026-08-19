package com.ofood.checkout.dto;

import java.util.UUID;

public class CheckoutResponse {
    private UUID subscriptionId;
    private String subscriptionStatus;
    private UUID paymentId;
    private String paymentStatus;
    private String provider;
    private String providerPaymentId;
    private CheckoutPreviewResponse pricingDetails;

    // Getters and Setters
    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }
    public CheckoutPreviewResponse getPricingDetails() { return pricingDetails; }
    public void setPricingDetails(CheckoutPreviewResponse pricingDetails) { this.pricingDetails = pricingDetails; }
}
