package com.ofood.voucher.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class VoucherValidationRequest {

    @NotBlank(message = "Code is required")
    private String code;

    private UUID planId;

    @NotNull(message = "Order value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Order value must be positive")
    private BigDecimal orderValue;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public BigDecimal getOrderValue() { return orderValue; }
    public void setOrderValue(BigDecimal orderValue) { this.orderValue = orderValue; }
}
