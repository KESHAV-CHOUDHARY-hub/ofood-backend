-- V2: Add user profile fields for Phase 2 compatibility
ALTER TABLE users ADD COLUMN first_name VARCHAR(255);
ALTER TABLE users ADD COLUMN last_name VARCHAR(255);
ALTER TABLE users ADD COLUMN mobile VARCHAR(50);
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(1024);

CREATE TABLE IF NOT EXISTS customer_profiles (
    user_id UUID PRIMARY KEY,
    date_of_birth DATE,
    preferences JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
