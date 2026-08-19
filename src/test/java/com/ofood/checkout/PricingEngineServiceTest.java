package com.ofood.checkout;

import com.ofood.catalog.model.Plan;
import com.ofood.checkout.dto.CheckoutPreviewResponse;
import com.ofood.checkout.service.PricingEngineService;
import com.ofood.voucher.dto.VoucherValidationRequest;
import com.ofood.voucher.dto.VoucherValidationResponse;
import com.ofood.voucher.model.DiscountType;
import com.ofood.voucher.model.Voucher;
import com.ofood.voucher.service.VoucherValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PricingEngineServiceTest {

    @Mock
    private VoucherValidationService voucherValidationService;

    @InjectMocks
    private PricingEngineService pricingEngineService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(pricingEngineService, "taxRatePercentage", new BigDecimal("5.0"));
        ReflectionTestUtils.setField(pricingEngineService, "deliveryFeeConst", new BigDecimal("50.00"));
    }

    @Test
    void calculatePricing_NoVoucher() {
        Plan plan = new Plan();
        plan.setPrice(new BigDecimal("1000"));
        plan.setCompareAtPrice(new BigDecimal("1200"));

        CheckoutPreviewResponse response = pricingEngineService.calculatePricing(plan, null);

        assertEquals(new BigDecimal("1000.00"), response.getPlanPrice());
        assertEquals(new BigDecimal("200.00"), response.getPlanDiscount());
        assertEquals(new BigDecimal("0.00"), response.getVoucherDiscount());
        assertEquals(new BigDecimal("1000.00"), response.getTaxableAmount());
        assertEquals(new BigDecimal("50.00"), response.getTax()); // 5% of 1000
        assertEquals(new BigDecimal("50.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("1100.00"), response.getFinalAmount()); // 1000 + 50 + 50
    }

    @Test
    void calculatePricing_WithVoucher() {
        Plan plan = new Plan();
        plan.setId(UUID.randomUUID());
        plan.setPrice(new BigDecimal("1000"));
        plan.setCompareAtPrice(new BigDecimal("1000")); // No plan discount

        Voucher voucher = new Voucher();
        voucher.setCode("DISCOUNT200");

        VoucherValidationResponse mockResp = new VoucherValidationResponse();
        mockResp.setValid(true);
        mockResp.setDiscountAmount(new BigDecimal("200.00"));

        when(voucherValidationService.validateAndCalculate(eq(voucher), any(VoucherValidationRequest.class)))
                .thenReturn(mockResp);

        CheckoutPreviewResponse response = pricingEngineService.calculatePricing(plan, voucher);

        assertEquals(new BigDecimal("1000.00"), response.getPlanPrice());
        assertEquals(new BigDecimal("0.00"), response.getPlanDiscount());
        assertEquals(new BigDecimal("200.00"), response.getVoucherDiscount());
        assertEquals(new BigDecimal("800.00"), response.getTaxableAmount()); // 1000 - 200
        assertEquals(new BigDecimal("40.00"), response.getTax()); // 5% of 800
        assertEquals(new BigDecimal("50.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("890.00"), response.getFinalAmount()); // 800 + 40 + 50
    }
}
