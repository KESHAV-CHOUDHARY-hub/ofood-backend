package com.ofood.checkout.service;

import com.ofood.catalog.model.Plan;
import com.ofood.checkout.dto.CheckoutPreviewResponse;
import com.ofood.voucher.dto.VoucherValidationRequest;
import com.ofood.voucher.dto.VoucherValidationResponse;
import com.ofood.voucher.model.Voucher;
import com.ofood.voucher.service.VoucherValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingEngineService {

    private final VoucherValidationService voucherValidationService;

    @Value("${ofood.pricing.tax-rate-percentage:5.0}")
    private BigDecimal taxRatePercentage;

    @Value("${ofood.pricing.delivery-fee:50.00}")
    private BigDecimal deliveryFeeConst;

    public PricingEngineService(VoucherValidationService voucherValidationService) {
        this.voucherValidationService = voucherValidationService;
    }

    public CheckoutPreviewResponse calculatePricing(Plan plan, Voucher voucher) {
        CheckoutPreviewResponse response = new CheckoutPreviewResponse();

        BigDecimal planPrice = plan.getPrice().setScale(2, RoundingMode.HALF_UP);
        response.setPlanPrice(planPrice);

        BigDecimal planDiscount = BigDecimal.ZERO;
        if (plan.getCompareAtPrice() != null && plan.getCompareAtPrice().compareTo(planPrice) > 0) {
            planDiscount = plan.getCompareAtPrice().subtract(planPrice).setScale(2, RoundingMode.HALF_UP);
        }
        response.setPlanDiscount(planDiscount);

        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (voucher != null) {
            VoucherValidationRequest req = new VoucherValidationRequest();
            req.setCode(voucher.getCode());
            req.setOrderValue(planPrice);
            req.setPlanId(plan.getId());
            
            VoucherValidationResponse voucherResp = voucherValidationService.validateAndCalculate(voucher, req);
            if (voucherResp.isValid()) {
                voucherDiscount = voucherResp.getDiscountAmount();
            }
        }
        response.setVoucherDiscount(voucherDiscount.setScale(2, RoundingMode.HALF_UP));

        BigDecimal taxableAmount = planPrice.subtract(voucherDiscount).setScale(2, RoundingMode.HALF_UP);
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }
        response.setTaxableAmount(taxableAmount);

        BigDecimal tax = taxableAmount.multiply(taxRatePercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        response.setTax(tax);

        BigDecimal deliveryFee = deliveryFeeConst.setScale(2, RoundingMode.HALF_UP);
        response.setDeliveryFee(deliveryFee);

        BigDecimal finalAmount = taxableAmount.add(tax).add(deliveryFee).setScale(2, RoundingMode.HALF_UP);
        response.setFinalAmount(finalAmount);

        return response;
    }
}
