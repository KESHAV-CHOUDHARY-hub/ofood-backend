package com.ofood.voucher.controller;

import com.ofood.voucher.dto.VoucherRequest;
import com.ofood.voucher.dto.VoucherResponse;
import com.ofood.voucher.dto.VoucherValidationRequest;
import com.ofood.voucher.dto.VoucherValidationResponse;
import com.ofood.voucher.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<VoucherResponse>> getAllVouchers() {
        return ResponseEntity.ok(voucherService.getAllVouchers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VoucherResponse> getVoucherById(@PathVariable UUID id) {
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.createVoucher(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VoucherResponse> updateVoucher(@PathVariable UUID id, @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteVoucher(@PathVariable UUID id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<VoucherResponse> getVoucherByCodeForCustomer(@PathVariable String code) {
        return ResponseEntity.ok(voucherService.getVoucherByCodeForCustomer(code));
    }

    @PostMapping("/validate")
    public ResponseEntity<VoucherValidationResponse> validateVoucher(@Valid @RequestBody VoucherValidationRequest request) {
        return ResponseEntity.ok(voucherService.validateVoucher(request));
    }
}
