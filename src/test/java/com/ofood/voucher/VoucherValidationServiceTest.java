package com.ofood.voucher;

import com.ofood.catalog.model.Plan;
import com.ofood.voucher.dto.VoucherValidationRequest;
import com.ofood.voucher.dto.VoucherValidationResponse;
import com.ofood.voucher.model.DiscountType;
import com.ofood.voucher.model.Voucher;
import com.ofood.voucher.model.VoucherStatus;
import com.ofood.voucher.service.VoucherValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VoucherValidationServiceTest {

    private VoucherValidationService service;

    @BeforeEach
    void setup() {
        service = new VoucherValidationService();
    }

    @Test
    void testPercentageDiscount() {
        Voucher v = createVoucher(DiscountType.PERCENTAGE, new BigDecimal("10.0"));
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertTrue(res.isValid());
        assertEquals(new BigDecimal("100.00"), res.getDiscountAmount());
        assertEquals(new BigDecimal("900.00"), res.getFinalAmount());
    }

    @Test
    void testPercentageDiscountWithMaxDiscount() {
        Voucher v = createVoucher(DiscountType.PERCENTAGE, new BigDecimal("10.0"));
        v.setMaxDiscount(new BigDecimal("50.00"));
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertTrue(res.isValid());
        assertEquals(new BigDecimal("50.00"), res.getDiscountAmount());
        assertEquals(new BigDecimal("950.00"), res.getFinalAmount());
    }

    @Test
    void testFixedDiscount() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("200.0"));
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertTrue(res.isValid());
        assertEquals(new BigDecimal("200.00"), res.getDiscountAmount());
        assertEquals(new BigDecimal("800.00"), res.getFinalAmount());
    }

    @Test
    void testFixedDiscountCappedAtOrderValue() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("1500.0"));
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertTrue(res.isValid());
        assertEquals(new BigDecimal("1000.00"), res.getDiscountAmount());
        assertEquals(new BigDecimal("0.00"), res.getFinalAmount());
    }

    @Test
    void testMinimumOrderEqualThreshold() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("100.0"));
        v.setMinimumOrderValue(new BigDecimal("500.0"));
        VoucherValidationRequest req = createReq(new BigDecimal("500.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertTrue(res.isValid());
    }

    @Test
    void testMinimumOrderBelowThreshold() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("100.0"));
        v.setMinimumOrderValue(new BigDecimal("500.0"));
        VoucherValidationRequest req = createReq(new BigDecimal("499.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertFalse(res.isValid());
        assertEquals("Minimum order value not met", res.getMessage());
    }

    @Test
    void testZeroPercentageFails() {
        Voucher v = createVoucher(DiscountType.PERCENTAGE, new BigDecimal("0.0"));
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertFalse(res.isValid());
        assertEquals("Invalid discount value on voucher", res.getMessage());
    }

    @Test
    void testNegativePercentageFails() {
        Voucher v = createVoucher(DiscountType.PERCENTAGE, new BigDecimal("-10.0"));
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertFalse(res.isValid());
        assertEquals("Invalid discount value on voucher", res.getMessage());
    }

    @Test
    void testPercentageGreaterThan100Fails() {
        Voucher v = createVoucher(DiscountType.PERCENTAGE, new BigDecimal("101.0"));
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertFalse(res.isValid());
        assertEquals("Percentage discount cannot exceed 100%", res.getMessage());
    }

    @Test
    void testFutureStartDateFails() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("100.0"));
        v.setStartDate(Instant.now().plus(1, ChronoUnit.DAYS));
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertFalse(res.isValid());
        assertEquals("Voucher is not yet active", res.getMessage());
    }

    @Test
    void testExpiredVoucherFails() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("100.0"));
        v.setExpiryDate(Instant.now().minus(1, ChronoUnit.DAYS));
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertFalse(res.isValid());
        assertEquals("Voucher has expired", res.getMessage());
    }

    @Test
    void testInactiveVoucherFails() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("100.0"));
        v.setStatus(VoucherStatus.INACTIVE);
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertFalse(res.isValid());
        assertEquals("Voucher is not active", res.getMessage());
    }

    @Test
    void testApplicablePlanSucceeds() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("100.0"));
        Plan p = new Plan();
        p.setId(UUID.randomUUID());
        v.getApplicablePlans().add(p);
        
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));
        req.setPlanId(p.getId());

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertTrue(res.isValid());
    }

    @Test
    void testNonApplicablePlanFails() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("100.0"));
        Plan p1 = new Plan();
        p1.setId(UUID.randomUUID());
        v.getApplicablePlans().add(p1);
        
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));
        req.setPlanId(UUID.randomUUID()); // Different plan

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertFalse(res.isValid());
        assertEquals("Voucher is not applicable to this plan", res.getMessage());
    }

    @Test
    void testGlobalVoucherWorksWithoutPlanRestrictions() {
        Voucher v = createVoucher(DiscountType.FIXED_AMOUNT, new BigDecimal("100.0"));
        // No applicable plans
        VoucherValidationRequest req = createReq(new BigDecimal("1000.0"));
        req.setPlanId(UUID.randomUUID());

        VoucherValidationResponse res = service.validateAndCalculate(v, req);
        assertTrue(res.isValid());
    }

    private Voucher createVoucher(DiscountType type, BigDecimal value) {
        Voucher v = new Voucher();
        v.setId(UUID.randomUUID());
        v.setCode("TEST");
        v.setStatus(VoucherStatus.ACTIVE);
        v.setDiscountType(type);
        v.setDiscountValue(value);
        return v;
    }

    private VoucherValidationRequest createReq(BigDecimal orderValue) {
        VoucherValidationRequest req = new VoucherValidationRequest();
        req.setCode("TEST");
        req.setOrderValue(orderValue);
        return req;
    }
}
