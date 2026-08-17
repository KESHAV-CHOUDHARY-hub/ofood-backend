package com.ofood.voucher.dto;

import com.ofood.voucher.model.DiscountType;
import com.ofood.voucher.model.VoucherStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class VoucherResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private BigDecimal minimumOrderValue;
    private Instant startDate;
    private Instant expiryDate;
    private Integer usageLimit;
    private Integer usagePerCustomer;
    private Integer usedCount;
    private VoucherStatus status;
    private Set<UUID> applicablePlanIds;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }
    public BigDecimal getMinimumOrderValue() { return minimumOrderValue; }
    public void setMinimumOrderValue(BigDecimal minimumOrderValue) { this.minimumOrderValue = minimumOrderValue; }
    public Instant getStartDate() { return startDate; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }
    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public Integer getUsagePerCustomer() { return usagePerCustomer; }
    public void setUsagePerCustomer(Integer usagePerCustomer) { this.usagePerCustomer = usagePerCustomer; }
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    public VoucherStatus getStatus() { return status; }
    public void setStatus(VoucherStatus status) { this.status = status; }
    public Set<UUID> getApplicablePlanIds() { return applicablePlanIds; }
    public void setApplicablePlanIds(Set<UUID> applicablePlanIds) { this.applicablePlanIds = applicablePlanIds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
