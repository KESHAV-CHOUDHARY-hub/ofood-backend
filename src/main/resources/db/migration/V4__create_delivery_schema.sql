-- V4: Create Delivery Schema
CREATE TABLE IF NOT EXISTS delivery_persons (
    id UUID PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    mobile VARCHAR(50) NOT NULL UNIQUE,
    vehicle_type VARCHAR(50) NOT NULL,
    vehicle_number VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS delivery_person_pincodes (
    delivery_person_id UUID NOT NULL,
    pincode_id UUID NOT NULL,
    PRIMARY KEY (delivery_person_id, pincode_id),
    FOREIGN KEY (delivery_person_id) REFERENCES delivery_persons(id) ON DELETE CASCADE,
    FOREIGN KEY (pincode_id) REFERENCES service_pincodes(id) ON DELETE CASCADE
);

CREATE INDEX idx_delivery_person_pincodes_pincode_id ON delivery_person_pincodes(pincode_id);
