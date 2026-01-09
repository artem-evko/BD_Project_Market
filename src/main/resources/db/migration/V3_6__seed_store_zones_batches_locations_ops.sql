-- V3_6__seed_store_zones_batches_locations_ops.sql
-- Seed для тестовой ТТ: store location -> zones -> batches -> batch_locations -> warehouse_operations
-- Без падений: все FK берём через SELECT, вставки делаем только если prereqs существуют.

------------------------------------------------------------
-- 1) storage location
------------------------------------------------------------
INSERT INTO storage_locations (id, name, type, address)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Магазин №1 (тестовая ТТ)',
    'STORE',
    'г. Тестоград, ул. Тестовая, д. 1'
)
ON CONFLICT (id) DO NOTHING;

------------------------------------------------------------
-- 2) привязка сотрудника к ТТ (если сотрудник есть)
------------------------------------------------------------
UPDATE employees
SET storage_location_id = '11111111-1111-1111-1111-111111111111'
WHERE id = '22222222-2222-2222-2222-222222222222';

------------------------------------------------------------
-- 3) zones (используем только эти id дальше)
------------------------------------------------------------
-- Холодильник
INSERT INTO storage_zones (
    id, storage_location_id, name, zone_type, temperature_mode, capacity, is_active
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7',
    '11111111-1111-1111-1111-111111111111',
    'Холодильник №1',
    'refrigerator',
    'cool',
    100.00,
    true
)
ON CONFLICT (id) DO NOTHING;

-- Витрина напитков
INSERT INTO storage_zones (
    id, storage_location_id, name, zone_type, temperature_mode, capacity, is_active
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8',
    '11111111-1111-1111-1111-111111111111',
    'Витрина напитков',
    'display',
    'normal',
    200.00,
    true
)
ON CONFLICT (id) DO NOTHING;

------------------------------------------------------------
-- 4) batches (product_id берём по barcode из таблицы product)
------------------------------------------------------------

-- Coca-Cola 1L (barcode 400000000002)
WITH p AS (
    SELECT id FROM product WHERE barcode = '400000000002' LIMIT 1
)
INSERT INTO batches (
    id, product_id, supply_invoice_id, manufacture_date, expiration_date, purchase_price, initial_quantity, created_at
)
SELECT
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    p.id,
    NULL,
    DATE '2025-01-01',
    DATE '2026-01-01',
    80.00,
    100.00,
    NOW()
FROM p
ON CONFLICT (id) DO NOTHING;

-- Jägermeister 0.7L (barcode 400000000001)
WITH p AS (
    SELECT id FROM product WHERE barcode = '400000000001' LIMIT 1
)
INSERT INTO batches (
    id, product_id, supply_invoice_id, manufacture_date, expiration_date, purchase_price, initial_quantity, created_at
)
SELECT
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
    p.id,
    NULL,
    DATE '2024-06-01',
    DATE '2030-06-01',
    3000.00,
    50.00,
    NOW()
FROM p
ON CONFLICT (id) DO NOTHING;

-- Danone Йогурт 125г (barcode 400000000003)
WITH p AS (
    SELECT id FROM product WHERE barcode = '400000000003' LIMIT 1
)
INSERT INTO batches (
    id, product_id, supply_invoice_id, manufacture_date, expiration_date, purchase_price, initial_quantity, created_at
)
SELECT
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
    p.id,
    NULL,
    DATE '2025-01-01',
    DATE '2025-02-15',
    50.00,
    40.00,
    NOW()
FROM p
ON CONFLICT (id) DO NOTHING;

------------------------------------------------------------
-- 5) batch_locations (делаем только если batch + zone существуют)
------------------------------------------------------------

-- Coca-Cola: 60 на витрине
INSERT INTO batch_locations (id, batch_id, storage_zone_id, quantity, updated_at)
SELECT
    'cccccccc-cccc-cccc-cccc-ccccccccccc1',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8',
    60.00,
    NOW()
WHERE EXISTS (SELECT 1 FROM batches WHERE id='bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1')
  AND EXISTS (SELECT 1 FROM storage_zones WHERE id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8')
ON CONFLICT (id) DO NOTHING;

-- Coca-Cola: 40 в холодильнике (условно как "подсобка")
INSERT INTO batch_locations (id, batch_id, storage_zone_id, quantity, updated_at)
SELECT
    'cccccccc-cccc-cccc-cccc-ccccccccccc2',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7',
    40.00,
    NOW()
WHERE EXISTS (SELECT 1 FROM batches WHERE id='bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1')
  AND EXISTS (SELECT 1 FROM storage_zones WHERE id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7')
ON CONFLICT (id) DO NOTHING;

-- Jägermeister: 20 на витрине
INSERT INTO batch_locations (id, batch_id, storage_zone_id, quantity, updated_at)
SELECT
    'cccccccc-cccc-cccc-cccc-ccccccccccc3',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8',
    20.00,
    NOW()
WHERE EXISTS (SELECT 1 FROM batches WHERE id='bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2')
  AND EXISTS (SELECT 1 FROM storage_zones WHERE id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8')
ON CONFLICT (id) DO NOTHING;

-- Danone: 35 в холодильнике
INSERT INTO batch_locations (id, batch_id, storage_zone_id, quantity, updated_at)
SELECT
    'cccccccc-cccc-cccc-cccc-ccccccccccc4',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7',
    35.00,
    NOW()
WHERE EXISTS (SELECT 1 FROM batches WHERE id='bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3')
  AND EXISTS (SELECT 1 FROM storage_zones WHERE id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7')
ON CONFLICT (id) DO NOTHING;

-- Danone: 20 на витрине (твой V3_7)
INSERT INTO batch_locations (id, batch_id, storage_zone_id, quantity, updated_at)
SELECT
    'cccccccc-cccc-cccc-cccc-ccccccccccc5',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8',
    20.00,
    NOW()
WHERE EXISTS (SELECT 1 FROM batches WHERE id='bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3')
  AND EXISTS (SELECT 1 FROM storage_zones WHERE id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8')
ON CONFLICT (id) DO NOTHING;

------------------------------------------------------------
-- 6) warehouse_operations (product_id берём из batches)
------------------------------------------------------------

-- RECEIPT: Coca-Cola -> витрина
INSERT INTO warehouse_operations (
    id, type, product_id, from_zone_id, to_zone_id, quantity, operation_date,
    employee_id, reason, batch_id, source_document_type, source_document_id
)
SELECT
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
    'RECEIPT',
    b.product_id,
    NULL,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8',
    60.00,
    NOW() - INTERVAL '5 days',
    '22222222-2222-2222-2222-222222222222',
    'Приёмка поставки',
    b.id,
    'SUPPLY_INVOICE',
    NULL
FROM batches b
WHERE b.id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1'
  AND EXISTS (SELECT 1 FROM employees e WHERE e.id='22222222-2222-2222-2222-222222222222')
  AND EXISTS (SELECT 1 FROM storage_zones z WHERE z.id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8')
ON CONFLICT (id) DO NOTHING;

-- TRANSFER: Danone витрина -> холодильник
INSERT INTO warehouse_operations (
    id, type, product_id, from_zone_id, to_zone_id, quantity, operation_date,
    employee_id, reason, batch_id, source_document_type, source_document_id
)
SELECT
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2',
    'TRANSFER',
    b.product_id,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7',
    10.00,
    NOW() - INTERVAL '2 days',
    '22222222-2222-2222-2222-222222222222',
    'Перемещение в холодильник',
    b.id,
    'INTERNAL_OPERATION',
    NULL
FROM batches b
WHERE b.id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3'
  AND EXISTS (SELECT 1 FROM employees e WHERE e.id='22222222-2222-2222-2222-222222222222')
  AND EXISTS (SELECT 1 FROM storage_zones z1 WHERE z1.id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8')
  AND EXISTS (SELECT 1 FROM storage_zones z2 WHERE z2.id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7')
ON CONFLICT (id) DO NOTHING;


