ALTER TABLE price_restrictions
ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_price_restrictions_storage_location_priority
ON price_restrictions(storage_location_id, priority DESC);

CREATE INDEX IF NOT EXISTS idx_price_restrictions_priority
ON price_restrictions(priority DESC);
