-- V10_2__seed_goods_receipt_real_data.sql
-- Реальные данные для модуля приёмки товара (Goods Receipt):
-- 1) Контракт + товары в контракте
-- 2) 3 накладные:
--    - ожидаемая (expected)
--    - в процессе (in_progress) + несоответствие (pending)
--    - принятая (received) + партии (batches) + размещение (batch_locations) + операции (warehouse_operations)

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
  v_sl_store        uuid;
  v_zone_cold       uuid;
  v_zone_display    uuid;

  v_contractor      uuid;
  v_truck           uuid;
  v_contract        uuid;

  v_storekeeper     uuid;
  v_merchandiser    uuid;

  v_p_jager         uuid;
  v_p_cola          uuid;
  v_p_danone        uuid;

  v_si_expected     uuid;
  v_si_progress     uuid;
  v_si_received     uuid;

  v_sii_prog_1      uuid;
  v_sii_prog_2      uuid;
  v_sii_prog_3      uuid;

  v_dr_prog_2       uuid;

  v_sii_rec_1       uuid;
  v_sii_rec_2       uuid;

  v_batch_rec_1     uuid;
  v_batch_rec_2     uuid;
BEGIN
  ----------------------------------------------------------------------
  -- 0) Базовые сущности: тестовая ТТ и зоны (используем те же id, что в V3_6)
  ----------------------------------------------------------------------
  INSERT INTO storage_locations (id, name, type, address)
  VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Магазин №1 (тестовая ТТ)',
    'STORE',
    'г. Тестоград, ул. Тестовая, д. 1'
  )
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO storage_zones (id, storage_location_id, name, zone_type, temperature_mode, capacity, is_active)
  VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7',
    '11111111-1111-1111-1111-111111111111',
    'Холодильник №1',
    'refrigerator',
    'cool',
    100.00,
    true
  )
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO storage_zones (id, storage_location_id, name, zone_type, temperature_mode, capacity, is_active)
  VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8',
    '11111111-1111-1111-1111-111111111111',
    'Витрина напитков',
    'display',
    'normal',
    200.00,
    true
  )
  ON CONFLICT (id) DO NOTHING;

  SELECT id INTO v_sl_store FROM storage_locations WHERE id='11111111-1111-1111-1111-111111111111' LIMIT 1;
  SELECT id INTO v_zone_cold FROM storage_zones WHERE id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7' LIMIT 1;
  SELECT id INTO v_zone_display FROM storage_zones WHERE id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8' LIMIT 1;

  ----------------------------------------------------------------------
  -- 1) Товары (если товаров нет — часть блоков пропустится)
  ----------------------------------------------------------------------
  SELECT id INTO v_p_jager  FROM product WHERE barcode='400000000001' LIMIT 1;
  SELECT id INTO v_p_cola   FROM product WHERE barcode='400000000002' LIMIT 1;
  SELECT id INTO v_p_danone FROM product WHERE barcode='400000000003' LIMIT 1;

  ----------------------------------------------------------------------
  -- 2) Сотрудники (кладовщик/товаровед). Берём то, что уже сидилось (V10_1, V3_2)
  ----------------------------------------------------------------------
  SELECT id INTO v_storekeeper
  FROM employees
  WHERE email='storekeeper_active@example.com'
  LIMIT 1;

  SELECT id INTO v_merchandiser
  FROM employees
  WHERE email='petrov@example.com'
  LIMIT 1;

  IF v_merchandiser IS NULL THEN
    v_merchandiser := v_storekeeper;
  END IF;

  ----------------------------------------------------------------------
  -- 3) Контрагент + транспорт + контракт
  ----------------------------------------------------------------------
  IF NOT EXISTS (SELECT 1 FROM contractors WHERE inn='2300000099') THEN
    INSERT INTO contractors (name, inn, kpp, address, phone, email)
    VALUES (
      'ООО "Поставщик Реальные Данные"',
      '2300000099',
      '230099001',
      'г. Краснодар, ул. Реальная, 10',
      '+7-861-900-00-99',
      'real-supplier@example.ru'
    );
  END IF;

  SELECT id INTO v_contractor FROM contractors WHERE inn='2300000099' LIMIT 1;

  IF NOT EXISTS (SELECT 1 FROM trucks WHERE plate_number='R999RR23') THEN
    INSERT INTO trucks (plate_number, model, capacity_kg, status, note)
    VALUES ('R999RR23', 'ГАЗель Next', 1500, 'ACTIVE', 'Поставка (реальные данные)');
  END IF;

  SELECT id INTO v_truck FROM trucks WHERE plate_number='R999RR23' LIMIT 1;

  IF v_contractor IS NOT NULL AND v_sl_store IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM contracts WHERE contract_number='CNT-REAL-2026-0001') THEN
      INSERT INTO contracts (
        contractor_id, storage_location_id, contract_number,
        date_start, date_end, delivery_type, truck_id, employee_id
      )
      VALUES (
        v_contractor, v_sl_store, 'CNT-REAL-2026-0001',
        DATE '2026-01-01', DATE '2026-12-31', 'TRUCK', v_truck, v_merchandiser
      );
    END IF;
  END IF;

  SELECT id INTO v_contract FROM contracts WHERE contract_number='CNT-REAL-2026-0001' LIMIT 1;

  -- contract_products (по желанию, но полезно для “реальности”)
  IF v_contract IS NOT NULL AND v_p_jager IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM contract_products WHERE contract_id=v_contract AND product_id=v_p_jager) THEN
      INSERT INTO contract_products (contract_id, product_id, quantity, purchase_price)
      VALUES (v_contract, v_p_jager, 200.00, 3100.00);
    END IF;
  END IF;

  IF v_contract IS NOT NULL AND v_p_cola IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM contract_products WHERE contract_id=v_contract AND product_id=v_p_cola) THEN
      INSERT INTO contract_products (contract_id, product_id, quantity, purchase_price)
      VALUES (v_contract, v_p_cola, 500.00, 90.00);
    END IF;
  END IF;

  IF v_contract IS NOT NULL AND v_p_danone IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM contract_products WHERE contract_id=v_contract AND product_id=v_p_danone) THEN
      INSERT INTO contract_products (contract_id, product_id, quantity, purchase_price)
      VALUES (v_contract, v_p_danone, 300.00, 55.00);
    END IF;
  END IF;

  ----------------------------------------------------------------------
  -- 4) Накладная 1: expected (для поиска/открытия приёмки)
  ----------------------------------------------------------------------
  IF v_contract IS NOT NULL AND v_sl_store IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM supply_invoices WHERE invoice_number='SI-REAL-2026-0001') THEN
      INSERT INTO supply_invoices (
        contract_id, storage_location_id, invoice_number,
        expected_date, status
      )
      VALUES (
        v_contract, v_sl_store, 'SI-REAL-2026-0001',
        DATE '2026-01-12', 'expected'
      );
    END IF;
  END IF;

  SELECT id INTO v_si_expected FROM supply_invoices WHERE invoice_number='SI-REAL-2026-0001' LIMIT 1;

  IF v_si_expected IS NOT NULL AND v_p_jager IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM supply_invoice_items WHERE supply_invoice_id=v_si_expected AND line_no=1
    ) THEN
      INSERT INTO supply_invoice_items (supply_invoice_id, line_no, product_id, quantity_expected, purchase_price, line_status)
      VALUES (v_si_expected, 1, v_p_jager, 12.00, 3100.00, 'pending');
    END IF;
  END IF;

  IF v_si_expected IS NOT NULL AND v_p_cola IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM supply_invoice_items WHERE supply_invoice_id=v_si_expected AND line_no=2
    ) THEN
      INSERT INTO supply_invoice_items (supply_invoice_id, line_no, product_id, quantity_expected, purchase_price, line_status)
      VALUES (v_si_expected, 2, v_p_cola, 24.00, 90.00, 'pending');
    END IF;
  END IF;

  ----------------------------------------------------------------------
  -- 5) Накладная 2: in_progress + discrepancy_pending
  ----------------------------------------------------------------------
  IF v_contract IS NOT NULL AND v_sl_store IS NOT NULL AND v_zone_display IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM supply_invoices WHERE invoice_number='SI-REAL-2026-0002') THEN
      INSERT INTO supply_invoices (
        contract_id, storage_location_id, invoice_number,
        expected_date, actual_date, status,
        received_zone_id, storekeeper_id, merchandiser_id
      )
      VALUES (
        v_contract, v_sl_store, 'SI-REAL-2026-0002',
        DATE '2026-01-09', DATE '2026-01-10', 'in_progress',
        v_zone_display, v_storekeeper, v_merchandiser
      );
    END IF;
  END IF;

  SELECT id INTO v_si_progress FROM supply_invoices WHERE invoice_number='SI-REAL-2026-0002' LIMIT 1;

  -- line 1 (ok)
  IF v_si_progress IS NOT NULL AND v_p_jager IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM supply_invoice_items WHERE supply_invoice_id=v_si_progress AND line_no=1
    ) THEN
      INSERT INTO supply_invoice_items (
        supply_invoice_id, line_no, product_id,
        quantity_expected, purchase_price,
        quantity_actual, manufacture_date, expiration_date,
        line_status, fact_entered_by, fact_entered_at
      )
      VALUES (
        v_si_progress, 1, v_p_jager,
        12.00, 3100.00,
        12.00, DATE '2025-12-01', DATE '2030-12-01',
        'ok', v_storekeeper, now()
      );
    END IF;
  END IF;

  -- line 2 (discrepancy_pending)
  IF v_si_progress IS NOT NULL AND v_p_cola IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM supply_invoice_items WHERE supply_invoice_id=v_si_progress AND line_no=2
    ) THEN
      INSERT INTO supply_invoice_items (
        supply_invoice_id, line_no, product_id,
        quantity_expected, purchase_price,
        quantity_actual, manufacture_date, expiration_date,
        line_status, fact_entered_by, fact_entered_at
      )
      VALUES (
        v_si_progress, 2, v_p_cola,
        24.00, 90.00,
        23.00, DATE '2026-01-01', DATE '2027-01-01',
        'discrepancy_pending', v_storekeeper, now()
      );
    END IF;
  END IF;

  -- line 3 (ok)
  IF v_si_progress IS NOT NULL AND v_p_danone IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM supply_invoice_items WHERE supply_invoice_id=v_si_progress AND line_no=3
    ) THEN
      INSERT INTO supply_invoice_items (
        supply_invoice_id, line_no, product_id,
        quantity_expected, purchase_price,
        quantity_actual, manufacture_date, expiration_date,
        line_status, fact_entered_by, fact_entered_at
      )
      VALUES (
        v_si_progress, 3, v_p_danone,
        30.00, 55.00,
        30.00, DATE '2026-01-06', DATE '2026-02-20',
        'ok', v_storekeeper, now()
      );
    END IF;
  END IF;

  SELECT id INTO v_sii_prog_2
  FROM supply_invoice_items
  WHERE v_si_progress IS NOT NULL AND supply_invoice_id=v_si_progress AND line_no=2
  LIMIT 1;

  -- discrepancy для line 2
  IF v_si_progress IS NOT NULL AND v_sii_prog_2 IS NOT NULL AND v_p_cola IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM discrepancy_requests WHERE supply_invoice_item_id=v_sii_prog_2) THEN
      INSERT INTO discrepancy_requests (
        supply_invoice_id, supply_invoice_item_id,
        product_id, discrepancy_type, description,
        quantity_expected, quantity_actual,
        status, created_by_employee_id, created_at
      )
      VALUES (
        v_si_progress, v_sii_prog_2,
        v_p_cola, 'shortage', 'Недостача при приемке (1 шт.)',
        24.00, 23.00,
        'pending', v_storekeeper, now()
      );
    END IF;

    SELECT id INTO v_dr_prog_2 FROM discrepancy_requests WHERE supply_invoice_item_id=v_sii_prog_2 LIMIT 1;

    -- проставляем ссылку на discrepancy_request в item
    IF v_dr_prog_2 IS NOT NULL THEN
      UPDATE supply_invoice_items
      SET discrepancy_request_id = v_dr_prog_2
      WHERE id = v_sii_prog_2
        AND (discrepancy_request_id IS DISTINCT FROM v_dr_prog_2);
    END IF;
  END IF;

  ----------------------------------------------------------------------
  -- 6) Накладная 3: received + batches + locations + operations
  ----------------------------------------------------------------------
  IF v_contract IS NOT NULL AND v_sl_store IS NOT NULL AND v_zone_cold IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM supply_invoices WHERE invoice_number='SI-REAL-2026-0003') THEN
      INSERT INTO supply_invoices (
        contract_id, storage_location_id, invoice_number,
        expected_date, actual_date, status,
        received_zone_id, storekeeper_id, merchandiser_id
      )
      VALUES (
        v_contract, v_sl_store, 'SI-REAL-2026-0003',
        DATE '2026-01-05', DATE '2026-01-05', 'received',
        v_zone_cold, v_storekeeper, v_merchandiser
      );
    END IF;
  END IF;

  SELECT id INTO v_si_received FROM supply_invoices WHERE invoice_number='SI-REAL-2026-0003' LIMIT 1;

  IF v_si_received IS NOT NULL AND v_p_jager IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM supply_invoice_items WHERE supply_invoice_id=v_si_received AND line_no=1) THEN
      INSERT INTO supply_invoice_items (
        supply_invoice_id, line_no, product_id,
        quantity_expected, purchase_price,
        quantity_actual, manufacture_date, expiration_date,
        line_status, fact_entered_by, fact_entered_at
      )
      VALUES (
        v_si_received, 1, v_p_jager,
        6.00, 3100.00,
        6.00, DATE '2025-12-01', DATE '2030-12-01',
        'accepted', v_storekeeper, now()
      );
    END IF;
  END IF;

  IF v_si_received IS NOT NULL AND v_p_cola IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM supply_invoice_items WHERE supply_invoice_id=v_si_received AND line_no=2) THEN
      INSERT INTO supply_invoice_items (
        supply_invoice_id, line_no, product_id,
        quantity_expected, purchase_price,
        quantity_actual, manufacture_date, expiration_date,
        line_status, fact_entered_by, fact_entered_at
      )
      VALUES (
        v_si_received, 2, v_p_cola,
        10.00, 90.00,
        10.00, DATE '2026-01-01', DATE '2027-01-01',
        'accepted', v_storekeeper, now()
      );
    END IF;
  END IF;

  SELECT id INTO v_sii_rec_1
  FROM supply_invoice_items
  WHERE v_si_received IS NOT NULL AND supply_invoice_id=v_si_received AND line_no=1
  LIMIT 1;

  SELECT id INTO v_sii_rec_2
  FROM supply_invoice_items
  WHERE v_si_received IS NOT NULL AND supply_invoice_id=v_si_received AND line_no=2
  LIMIT 1;

  -- batches для received invoice
  IF v_si_received IS NOT NULL AND v_p_jager IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM batches
      WHERE supply_invoice_id=v_si_received AND product_id=v_p_jager
        AND manufacture_date=DATE '2025-12-01' AND expiration_date=DATE '2030-12-01'
        AND purchase_price=3100.00 AND initial_quantity=6.00
    ) THEN
      INSERT INTO batches (
        product_id, supply_invoice_id, manufacture_date, expiration_date, purchase_price, initial_quantity, created_at
      )
      VALUES (
        v_p_jager, v_si_received, DATE '2025-12-01', DATE '2030-12-01', 3100.00, 6.00, now()
      );
    END IF;

    SELECT id INTO v_batch_rec_1
    FROM batches
    WHERE supply_invoice_id=v_si_received AND product_id=v_p_jager
      AND manufacture_date=DATE '2025-12-01' AND expiration_date=DATE '2030-12-01'
      AND purchase_price=3100.00 AND initial_quantity=6.00
    ORDER BY created_at DESC
    LIMIT 1;
  END IF;

  IF v_si_received IS NOT NULL AND v_p_cola IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM batches
      WHERE supply_invoice_id=v_si_received AND product_id=v_p_cola
        AND manufacture_date=DATE '2026-01-01' AND expiration_date=DATE '2027-01-01'
        AND purchase_price=90.00 AND initial_quantity=10.00
    ) THEN
      INSERT INTO batches (
        product_id, supply_invoice_id, manufacture_date, expiration_date, purchase_price, initial_quantity, created_at
      )
      VALUES (
        v_p_cola, v_si_received, DATE '2026-01-01', DATE '2027-01-01', 90.00, 10.00, now()
      );
    END IF;

    SELECT id INTO v_batch_rec_2
    FROM batches
    WHERE supply_invoice_id=v_si_received AND product_id=v_p_cola
      AND manufacture_date=DATE '2026-01-01' AND expiration_date=DATE '2027-01-01'
      AND purchase_price=90.00 AND initial_quantity=10.00
    ORDER BY created_at DESC
    LIMIT 1;
  END IF;

  -- проставляем batch_id в items
  IF v_sii_rec_1 IS NOT NULL AND v_batch_rec_1 IS NOT NULL THEN
    UPDATE supply_invoice_items
    SET batch_id = v_batch_rec_1
    WHERE id = v_sii_rec_1
      AND (batch_id IS DISTINCT FROM v_batch_rec_1);
  END IF;

  IF v_sii_rec_2 IS NOT NULL AND v_batch_rec_2 IS NOT NULL THEN
    UPDATE supply_invoice_items
    SET batch_id = v_batch_rec_2
    WHERE id = v_sii_rec_2
      AND (batch_id IS DISTINCT FROM v_batch_rec_2);
  END IF;

  -- batch_locations (кладём всё в cold zone)
  IF v_batch_rec_1 IS NOT NULL AND v_zone_cold IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM batch_locations WHERE batch_id=v_batch_rec_1 AND storage_zone_id=v_zone_cold) THEN
      INSERT INTO batch_locations (batch_id, storage_zone_id, quantity, updated_at)
      VALUES (v_batch_rec_1, v_zone_cold, 6.00, now());
    END IF;
  END IF;

  IF v_batch_rec_2 IS NOT NULL AND v_zone_cold IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM batch_locations WHERE batch_id=v_batch_rec_2 AND storage_zone_id=v_zone_cold) THEN
      INSERT INTO batch_locations (batch_id, storage_zone_id, quantity, updated_at)
      VALUES (v_batch_rec_2, v_zone_cold, 10.00, now());
    END IF;
  END IF;

  -- warehouse_operations (RECEIPT) с привязкой к документу
  IF v_batch_rec_1 IS NOT NULL AND v_storekeeper IS NOT NULL AND v_zone_cold IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM warehouse_operations
      WHERE type='RECEIPT' AND batch_id=v_batch_rec_1 AND source_document_id=v_si_received
    ) THEN
      INSERT INTO warehouse_operations (
        type, product_id, from_zone_id, to_zone_id, quantity, operation_date,
        employee_id, reason, batch_id, source_document_type, source_document_id
      )
      SELECT
        'RECEIPT',
        b.product_id,
        NULL,
        v_zone_cold,
        6.00,
        now() - interval '5 days',
        v_storekeeper,
        'Приёмка поставки (реальные данные)',
        b.id,
        'SUPPLY_INVOICE',
        v_si_received
      FROM batches b
      WHERE b.id = v_batch_rec_1;
    END IF;
  END IF;

  IF v_batch_rec_2 IS NOT NULL AND v_storekeeper IS NOT NULL AND v_zone_cold IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM warehouse_operations
      WHERE type='RECEIPT' AND batch_id=v_batch_rec_2 AND source_document_id=v_si_received
    ) THEN
      INSERT INTO warehouse_operations (
        type, product_id, from_zone_id, to_zone_id, quantity, operation_date,
        employee_id, reason, batch_id, source_document_type, source_document_id
      )
      SELECT
        'RECEIPT',
        b.product_id,
        NULL,
        v_zone_cold,
        10.00,
        now() - interval '5 days',
        v_storekeeper,
        'Приёмка поставки (реальные данные)',
        b.id,
        'SUPPLY_INVOICE',
        v_si_received
      FROM batches b
      WHERE b.id = v_batch_rec_2;
    END IF;
  END IF;

END $$;
