package com.ofood.voucher.service;

import com.ofood.catalog.model.Plan;
import com.ofood.voucher.dto.VoucherValidationRequest;
import com.ofood.voucher.dto.VoucherValidationResponse;
import com.ofood.voucher.model.DiscountType;
import com.ofood.voucher.model.Voucher;
import com.ofood.voucher.model.VoucherStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VoucherValidationService {

    public VoucherValidationResponse validateAndCalculate(Voucher voucher, VoucherValidationRequest request) {
        VoucherValidationResponse response = new VoucherValidationResponse();
        response.setVoucherId(voucher.getId());
        response.setVoucherCode(voucher.getCode());

        try {
            validateVoucherRules(voucher, request);
            
            BigDecimal discount = calculateDiscount(voucher, request.getOrderValue());
            BigDecimal finalAmount = request.getOrderValue().subtract(discount);
            
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }

            response.setValid(true);
            response.setMessage("Voucher applied successfully");
            response.setDiscountAmount(discount);
            response.setFinalAmount(finalAmount);

        } catch (IllegalArgumentException e) {
            response.setValid(false);
            response.setMessage(e.getMessage());
            response.setDiscountAmount(BigDecimal.ZERO);
            response.setFinalAmount(request.getOrderValue());
        }

        return response;
    }

    private void validateVoucherRules(Voucher voucher, VoucherValidationRequest request) {
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            throw new IllegalArgumentException("Voucher is not active");
        }

        Instant now = Instant.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new IllegalArgumentException("Voucher is not yet active");
        }

        if (voucher.getExpiryDate() != null && now.isAfter(voucher.getExpiryDate())) {
            throw new IllegalArgumentException("Voucher has expired");
        }

        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new IllegalArgumentException("Voucher usage limit exceeded");
        }

        if (voucher.getMinimumOrderValue() != null && request.getOrderValue().compareTo(voucher.getMinimumOrderValue()) < 0) {
            throw new IllegalArgumentException("Minimum order value not met");
        }

        Set<Plan> plans = voucher.getApplicablePlans();
        if (plans != null && !plans.isEmpty()) {
            if (request.getPlanId() == null) {
                throw new IllegalArgumentException("Plan ID is required for this voucher");
            }
            Set<UUID> planIds = plans.stream().map(Plan::getId).collect(Collectors.toSet());
            if (!planIds.contains(request.getPlanId())) {
                throw new IllegalArgumentException("Voucher is not applicable to this plan");
            }
        }
        
        // Note: Per-customer usage check is not fully implemented here because actual usage tracking 
        // requires a normalized voucher_usages table, which is dependent on the Checkout/Payments phase.
        // We defer full per-customer tracking until checkout is introduced.
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderValue) {
        BigDecimal discount = BigDecimal.ZERO;

        if (voucher.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid discount value on voucher");
        }

        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            if (voucher.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
            discount = orderValue.multiply(voucher.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    
            if (voucher.getMaxDiscount() != null && discount.compareTo(voucher.getMaxDiscount()) > 0) {
                discount = voucher.getMaxDiscount();
            }
        } else if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = voucher.getDiscountValue();
            if (discount.compareTo(orderValue) > 0) {
                discount = orderValue;
            }
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }
}
