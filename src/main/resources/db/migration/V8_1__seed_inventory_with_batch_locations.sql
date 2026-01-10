-- V8_1__seed_inventory_with_batch_locations.sql

WITH
sl AS (
  SELECT id
  FROM storage_locations
  WHERE name = 'Магазин №1 Краснодар'
  LIMIT 1
),
emp AS (
  SELECT id
  FROM employees
  WHERE full_name = 'Иванов Иван Иванович'
  LIMIT 1
),
p1 AS (
  SELECT id FROM product WHERE barcode = '4600000000011' LIMIT 1
),
b1 AS (
  SELECT b.id
  FROM batches b
  JOIN product p ON p.id = b.product_id
  WHERE p.barcode = '4600000000011'
  ORDER BY b.created_at DESC
  LIMIT 1
),

inv AS (
  INSERT INTO inventory (inventory_date, employee_id, storage_location_id, status)
  SELECT DATE '2026-01-10', (SELECT id FROM emp), (SELECT id FROM sl), 'IN_PROGRESS'
  WHERE (SELECT id FROM emp) IS NOT NULL
    AND (SELECT id FROM sl) IS NOT NULL
  RETURNING id
)
INSERT INTO inventory_items (inventory_id, product_id, expected_qty, actual_qty, batch_id)
SELECT
  (SELECT id FROM inv),
  (SELECT id FROM p1),
  10.00,
  8.00,
  (SELECT id FROM b1)
WHERE (SELECT id FROM inv) IS NOT NULL
  AND (SELECT id FROM p1) IS NOT NULL;
