package com.ofood.voucher.repository;

import com.ofood.voucher.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    Optional<Voucher> findByCode(String code);
    boolean existsByCode(String code);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 WHERE v.id = :id AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    int incrementUsedCountIfAllowed(@Param("id") UUID id);
}
