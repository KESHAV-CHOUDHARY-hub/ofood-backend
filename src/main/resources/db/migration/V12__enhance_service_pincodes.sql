-- V12: Enhance service_pincodes to support serviceable areas

-- 1. Add new columns for area name and service area (geographic polygon)
ALTER TABLE service_pincodes ADD COLUMN area_name VARCHAR(100);
ALTER TABLE service_pincodes ADD COLUMN service_area JSONB;

-- 2. Backfill existing records to have a default area name so they don't violate constraints later
UPDATE service_pincodes SET area_name = 'Default Area' WHERE area_name IS NULL;

-- 3. Make area_name required
ALTER TABLE service_pincodes ALTER COLUMN area_name SET NOT NULL;

-- 4. Drop the existing UNIQUE constraint on pincode to allow multiple areas per pincode
ALTER TABLE service_pincodes DROP CONSTRAINT IF EXISTS service_pincodes_pincode_key;

-- 5. Add the new composite UNIQUE constraint as approved
ALTER TABLE service_pincodes ADD CONSTRAINT service_pincodes_city_pincode_area_key UNIQUE (city_id, pincode, area_name);
