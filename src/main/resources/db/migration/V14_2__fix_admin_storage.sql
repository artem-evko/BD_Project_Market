UPDATE employees e
SET storage_location_id = '11111111-1111-1111-1111-111111111111'
FROM user_accounts ua
WHERE ua.employee_id = e.id
  AND ua.login = 'admin';

  INSERT INTO store_prices (id, storage_location_id, product_id, price, effective_date)
SELECT
  gen_random_uuid(),
  '11111111-1111-1111-1111-111111111111',
  p.id,
  100.00,
  CURRENT_DATE
FROM product p
WHERE p.archived = false
  AND NOT EXISTS (
    SELECT 1
    FROM store_prices sp
    WHERE sp.storage_location_id = '11111111-1111-1111-1111-111111111111'
      AND sp.product_id = p.id
      AND sp.effective_date = CURRENT_DATE
  );


DO $$
DECLARE
  v_sl_id uuid := '11111111-1111-1111-1111-111111111111';
  v_zone_id uuid;
BEGIN
  -- 1) Найти любую активную зону хранения для этой ТТ
  SELECT z.id INTO v_zone_id
  FROM storage_zones z
  WHERE z.storage_location_id = v_sl_id
    AND z.is_active = true
  ORDER BY z.name
  LIMIT 1;

  -- 2) Если зоны нет — создать дефолтную
  IF v_zone_id IS NULL THEN
    v_zone_id := gen_random_uuid();

    INSERT INTO storage_zones (
      id, storage_location_id, name, zone_type, temperature_mode, capacity, is_active
    ) VALUES (
      v_zone_id, v_sl_id, 'Основная зона', 'default', 'normal', 999999.00, true
    );
  END IF;

  -- 3) Для каждого товара без остатков в этой ТТ создать партию и остаток в batch_locations
  -- Товар "имеет остатки в ТТ", если существует batch_locations -> storage_zones этой ТТ.
  WITH products_without_stock AS (
    SELECT p.id AS product_id
    FROM product p
    WHERE NOT EXISTS (
      SELECT 1
      FROM batch_locations bl
      JOIN batches b ON b.id = bl.batch_id
      JOIN storage_zones z ON z.id = bl.storage_zone_id
      WHERE b.product_id = p.id
        AND z.storage_location_id = v_sl_id
        AND z.is_active = true
    )
  ),
  created_batches AS (
    INSERT INTO batches (
      id, product_id, supply_invoice_id,
      manufacture_date, expiration_date,
      purchase_price, initial_quantity, created_at
    )
    SELECT
      gen_random_uuid(),
      pws.product_id,
      NULL,
      CURRENT_DATE,
      CURRENT_DATE + INTERVAL '365 days',
      100.00,
      5.00,
      NOW()
    FROM products_without_stock pws
    RETURNING id, product_id
  )
  INSERT INTO batch_locations (
    id, batch_id, storage_zone_id, quantity, updated_at
  )
  SELECT
    gen_random_uuid(),
    cb.id,
    v_zone_id,
    5.00,
    NOW()
  FROM created_batches cb;

END $$;
