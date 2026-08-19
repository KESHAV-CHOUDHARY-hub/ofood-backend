package com.ofood.checkout.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CheckoutPreviewRequest {
    @NotNull(message = "Plan ID is required")
    private UUID planId;

    @NotNull(message = "Address ID is required")
    private UUID addressId;

    private String voucherCode;

    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public UUID getAddressId() { return addressId; }
    public void setAddressId(UUID addressId) { this.addressId = addressId; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
}
