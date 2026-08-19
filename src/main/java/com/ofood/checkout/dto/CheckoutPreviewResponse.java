package com.ofood.checkout.dto;

import java.math.BigDecimal;

public class CheckoutPreviewResponse {
    private BigDecimal planPrice;
    private BigDecimal planDiscount;
    private BigDecimal voucherDiscount;
    private BigDecimal taxableAmount;
    private BigDecimal tax;
    private BigDecimal deliveryFee;
    private BigDecimal finalAmount;

    // Getters and setters
    public BigDecimal getPlanPrice() { return planPrice; }
    public void setPlanPrice(BigDecimal planPrice) { this.planPrice = planPrice; }
    public BigDecimal getPlanDiscount() { return planDiscount; }
    public void setPlanDiscount(BigDecimal planDiscount) { this.planDiscount = planDiscount; }
    public BigDecimal getVoucherDiscount() { return voucherDiscount; }
    public void setVoucherDiscount(BigDecimal voucherDiscount) { this.voucherDiscount = voucherDiscount; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
}
