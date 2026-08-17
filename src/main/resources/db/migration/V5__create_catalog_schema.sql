-- V5: Create Catalog Schema
CREATE TABLE IF NOT EXISTS plans (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    short_description TEXT,
    description TEXT,
    image VARCHAR(1024),
    gallery JSONB,
    price DECIMAL(10, 2) NOT NULL,
    compare_at_price DECIMAL(10, 2),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    duration INT NOT NULL,
    duration_unit VARCHAR(50) NOT NULL,
    meal_count INT NOT NULL,
    meals_per_day INT NOT NULL,
    servings_per_meal INT NOT NULL,
    meal_types JSONB,
    features JSONB,
    ingredients JSONB,
    nutrition JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    is_featured BOOLEAN NOT NULL DEFAULT false,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS plan_meals (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL,
    meal_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    calories INT,
    serving_size VARCHAR(100),
    ingredients JSONB,
    nutrition JSONB,
    image_url VARCHAR(1024),
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE
);

CREATE INDEX idx_plan_meals_plan_id ON plan_meals(plan_id);
