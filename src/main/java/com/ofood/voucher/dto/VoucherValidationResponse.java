package com.ofood.voucher.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class VoucherValidationResponse {
    private boolean isValid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private UUID voucherId;
    private String voucherCode;

    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public UUID getVoucherId() { return voucherId; }
    public void setVoucherId(UUID voucherId) { this.voucherId = voucherId; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
}
