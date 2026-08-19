package com.ofood.voucher.repository;

import com.ofood.voucher.model.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, UUID> {
    long countByVoucherIdAndCustomerId(UUID voucherId, UUID customerId);
}
