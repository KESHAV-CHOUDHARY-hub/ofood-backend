package com.ofood.voucher.service;

import com.ofood.catalog.model.Plan;
import com.ofood.catalog.repository.PlanRepository;
import com.ofood.voucher.dto.VoucherRequest;
import com.ofood.voucher.dto.VoucherResponse;
import com.ofood.voucher.dto.VoucherValidationRequest;
import com.ofood.voucher.dto.VoucherValidationResponse;
import com.ofood.voucher.model.Voucher;
import com.ofood.voucher.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final PlanRepository planRepository;
    private final VoucherValidationService validationService;

    public VoucherService(VoucherRepository voucherRepository, PlanRepository planRepository, VoucherValidationService validationService) {
        this.voucherRepository = voucherRepository;
        this.planRepository = planRepository;
        this.validationService = validationService;
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> getAllVouchers() {
        return voucherRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(UUID id) {
        return voucherRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
    }

    @Transactional
    public VoucherResponse createVoucher(VoucherRequest request) {
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Voucher code already exists");
        }
        Voucher voucher = new Voucher();
        updateVoucherFromRequest(voucher, request);
        voucher = voucherRepository.save(voucher);
        return mapToResponse(voucher);
    }

    @Transactional
    public VoucherResponse updateVoucher(UUID id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));

        if (!voucher.getCode().equals(request.getCode()) && voucherRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Voucher code already exists");
        }

        updateVoucherFromRequest(voucher, request);
        voucher.setUpdatedAt(Instant.now());
        voucher = voucherRepository.save(voucher);
        return mapToResponse(voucher);
    }

    @Transactional
    public void deleteVoucher(UUID id) {
        if (!voucherRepository.existsById(id)) {
            throw new IllegalArgumentException("Voucher not found");
        }
        voucherRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public VoucherResponse getVoucherByCodeForCustomer(String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        
        // Hide administrative fields for customer
        VoucherResponse response = mapToResponse(voucher);
        response.setUsedCount(null);
        response.setUsageLimit(null);
        response.setUsagePerCustomer(null);
        response.setCreatedAt(null);
        response.setUpdatedAt(null);
        
        return response;
    }

    @Transactional(readOnly = true)
    public VoucherValidationResponse validateVoucher(VoucherValidationRequest request) {
        Voucher voucher = voucherRepository.findByCode(request.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
                
        return validationService.validateAndCalculate(voucher, request);
    }

    private void updateVoucherFromRequest(Voucher voucher, VoucherRequest request) {
        voucher.setCode(request.getCode());
        voucher.setName(request.getName());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setMinimumOrderValue(request.getMinimumOrderValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setExpiryDate(request.getExpiryDate());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setUsagePerCustomer(request.getUsagePerCustomer());
        voucher.setStatus(request.getStatus());

        if (request.getApplicablePlanIds() != null && !request.getApplicablePlanIds().isEmpty()) {
            List<Plan> plans = planRepository.findAllById(request.getApplicablePlanIds());
            if (plans.size() != request.getApplicablePlanIds().size()) {
                throw new IllegalArgumentException("One or more applicable plans not found");
            }
            voucher.setApplicablePlans(new HashSet<>(plans));
        } else {
            voucher.setApplicablePlans(new HashSet<>());
        }
    }

    private VoucherResponse mapToResponse(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setName(voucher.getName());
        response.setDescription(voucher.getDescription());
        response.setDiscountType(voucher.getDiscountType());
        response.setDiscountValue(voucher.getDiscountValue());
        response.setMaxDiscount(voucher.getMaxDiscount());
        response.setMinimumOrderValue(voucher.getMinimumOrderValue());
        response.setStartDate(voucher.getStartDate());
        response.setExpiryDate(voucher.getExpiryDate());
        response.setUsageLimit(voucher.getUsageLimit());
        response.setUsagePerCustomer(voucher.getUsagePerCustomer());
        response.setUsedCount(voucher.getUsedCount());
        response.setStatus(voucher.getStatus());
        
        if (voucher.getApplicablePlans() != null) {
            response.setApplicablePlanIds(voucher.getApplicablePlans().stream()
                    .map(Plan::getId).collect(Collectors.toSet()));
        }
        
        response.setCreatedAt(voucher.getCreatedAt());
        response.setUpdatedAt(voucher.getUpdatedAt());
        return response;
    }
}
