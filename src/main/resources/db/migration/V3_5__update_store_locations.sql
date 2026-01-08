-- V3_5__update_store_locations.sql

UPDATE store_prices
SET storage_location_id = '11111111-1111-1111-1111-111111111111'
WHERE product_id IN (
    '25c55fe0-dd9e-45de-9817-6ddfaf8bf18b',  -- Jägermeister 0.7L
    '2855c128-3058-47fa-afc7-594189d08cd4'  -- Coca-Cola 1L
)
AND storage_location_id <> '11111111-1111-1111-1111-111111111111';