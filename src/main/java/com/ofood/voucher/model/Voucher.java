package com.ofood.voucher.model;

import com.ofood.catalog.model.Plan;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "minimum_order_value", precision = 10, scale = 2)
    private BigDecimal minimumOrderValue;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_per_customer")
    private Integer usagePerCustomer;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VoucherStatus status = VoucherStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "voucher_plans",
            joinColumns = @JoinColumn(name = "voucher_id"),
            inverseJoinColumns = @JoinColumn(name = "plan_id")
    )
    private Set<Plan> applicablePlans = new HashSet<>();

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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Set<Plan> getApplicablePlans() { return applicablePlans; }
    public void setApplicablePlans(Set<Plan> applicablePlans) { this.applicablePlans = applicablePlans; }
}
