package com.ofood.voucher.repository;

import com.ofood.voucher.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    Optional<Voucher> findByCode(String code);
    boolean existsByCode(String code);
}
