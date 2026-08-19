CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users(id),
    plan_id UUID NOT NULL REFERENCES plans(id),
    address_id UUID NOT NULL REFERENCES addresses(id),
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(30) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    plan_discount DECIMAL(10,2) NOT NULL DEFAULT 0,
    voucher_discount DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0,
    delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    voucher_id UUID REFERENCES vouchers(id),
    paused_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS voucher_usages (
    id UUID PRIMARY KEY,
    voucher_id UUID NOT NULL REFERENCES vouchers(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (voucher_id, customer_id, subscription_id)
);

CREATE INDEX idx_subscriptions_customer_id ON subscriptions(customer_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_voucher_usages_customer_id ON voucher_usages(customer_id);
