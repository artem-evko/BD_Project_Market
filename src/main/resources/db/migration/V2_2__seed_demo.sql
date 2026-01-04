-- =========================
-- V2.2 seed: demo/ops data (FIXED, idempotent)
-- =========================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Контрагент + контакт (без "cont_1"!)
WITH cont AS (
  INSERT INTO contractors (name, inn, kpp, address, phone, email)
  SELECT 'ООО Поставщик Юг', '1234567890', '123401001',
         'г. Краснодар, ул. Поставщиков, 5', '+7-861-111-11-11', 'info@supplier.local'
  WHERE NOT EXISTS (SELECT 1 FROM contractors WHERE name='ООО Поставщик Юг')
  RETURNING id
),
cont_id AS (
  SELECT id FROM cont
  UNION ALL
  SELECT id FROM contractors WHERE name='ООО Поставщик Юг'
  LIMIT 1
)
INSERT INTO contract_contacts (contractor_id, full_name, phone, email, position)
SELECT (SELECT id FROM cont_id),
       'Кузнецов Алексей', '+7-861-555-55-55', 'kuznetsov@example.ru', 'Менеджер'
WHERE NOT EXISTS (
  SELECT 1
  FROM contract_contacts cc
  WHERE cc.contractor_id = (SELECT id FROM cont_id)
    AND cc.full_name='Кузнецов Алексей'
);

-- 2) Storage zones (если нет)
WITH loc AS (
  SELECT id FROM storage_locations WHERE name='Склад центральный' LIMIT 1
)
INSERT INTO storage_zones (storage_location_id, name, zone_type, temperature_mode, capacity, is_active)
SELECT (SELECT id FROM loc), 'Приемка', 'receiving', '+10..+25', 1000, TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM storage_zones
  WHERE storage_location_id=(SELECT id FROM loc) AND name='Приемка'
);

WITH loc AS (
  SELECT id FROM storage_locations WHERE name='Склад центральный' LIMIT 1
)
INSERT INTO storage_zones (storage_location_id, name, zone_type, temperature_mode, capacity, is_active)
SELECT (SELECT id FROM loc), 'Основная зона', 'storage', '+10..+25', 5000, TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM storage_zones
  WHERE storage_location_id=(SELECT id FROM loc) AND name='Основная зона'
);

-- 3) Пример партии + привязка к зоне (без created_at)

WITH p AS (
  SELECT id FROM product WHERE barcode='4600000000011' LIMIT 1
),
ins AS (
  INSERT INTO batches (product_id, manufacture_date, expiration_date, purchase_price, initial_quantity)
  SELECT (SELECT id FROM p), DATE '2025-12-01', DATE '2027-12-01', 800.00, 50
  WHERE (SELECT id FROM p) IS NOT NULL
    AND NOT EXISTS (
      SELECT 1
      FROM batches b
      WHERE b.product_id = (SELECT id FROM p)
        AND b.manufacture_date = DATE '2025-12-01'
        AND b.expiration_date  = DATE '2027-12-01'
        AND b.initial_quantity = 50
    )
  RETURNING id
),
bid AS (
  -- если вставили новую — берём её
  SELECT id FROM ins
  UNION ALL
  -- иначе берём уже существующую, совпадающую по тем же признакам
  SELECT b.id
  FROM batches b
  WHERE b.product_id = (SELECT id FROM p)
    AND b.manufacture_date = DATE '2025-12-01'
    AND b.expiration_date  = DATE '2027-12-01'
    AND b.initial_quantity = 50
  LIMIT 1
),
z AS (
  SELECT id FROM storage_zones WHERE name='Основная зона' LIMIT 1
)
INSERT INTO batch_locations (batch_id, storage_zone_id, quantity)
SELECT (SELECT id FROM bid), (SELECT id FROM z), 50
WHERE (SELECT id FROM bid) IS NOT NULL
  AND (SELECT id FROM z) IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM batch_locations
    WHERE batch_id=(SELECT id FROM bid)
      AND storage_zone_id=(SELECT id FROM z)
  );
