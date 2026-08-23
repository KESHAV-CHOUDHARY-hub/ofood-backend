-- V10: Make plan fields optional and add extended metadata

-- Make configuration fields optional to support draft saving
ALTER TABLE plans ALTER COLUMN price DROP NOT NULL;
ALTER TABLE plans ALTER COLUMN currency DROP NOT NULL;
ALTER TABLE plans ALTER COLUMN duration DROP NOT NULL;
ALTER TABLE plans ALTER COLUMN duration_unit DROP NOT NULL;
ALTER TABLE plans ALTER COLUMN meal_count DROP NOT NULL;
ALTER TABLE plans ALTER COLUMN meals_per_day DROP NOT NULL;
ALTER TABLE plans ALTER COLUMN servings_per_meal DROP NOT NULL;

-- Add new extended metadata columns
ALTER TABLE plans ADD COLUMN calories_label VARCHAR(255);
ALTER TABLE plans ADD COLUMN delivery_information TEXT;
ALTER TABLE plans ADD COLUMN terms TEXT;
ALTER TABLE plans ADD COLUMN seo_title VARCHAR(255);
ALTER TABLE plans ADD COLUMN seo_description TEXT;
