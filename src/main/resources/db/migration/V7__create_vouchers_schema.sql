-- V7: Create Vouchers Schema
CREATE TABLE IF NOT EXISTS vouchers (
    id UUID PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type VARCHAR(30) NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    max_discount DECIMAL(10, 2),
    minimum_order_value DECIMAL(10, 2),
    start_date TIMESTAMP,
    expiry_date TIMESTAMP,
    usage_limit INTEGER,
    usage_per_customer INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS voucher_plans (
    voucher_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    PRIMARY KEY (voucher_id, plan_id),
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
);

CREATE INDEX idx_vouchers_code ON vouchers(code);
CREATE INDEX idx_vouchers_status ON vouchers(status);
CREATE INDEX idx_vouchers_start_date ON vouchers(start_date);
CREATE INDEX idx_vouchers_expiry_date ON vouchers(expiry_date);
