-- =========================
-- V2.4 seed: данные для 2.37..2.51 (SAFE / НЕ ПАДАЕТ ОТ NULL)
-- =========================
-- Идея: всё делаем через DO-блок и проверки наличия зависимостей.
-- Если каких-то базовых данных нет (товары/локации/сотрудники/контракт) — блоки пропускаются с NOTICE.

DO $$
DECLARE
  v_sl_store          uuid;
  v_sl_wh             uuid;

  v_p_jager           uuid;
  v_p_cola            uuid;

  v_e_director        uuid;
  v_e_manager         uuid;
  v_e_keeper          uuid;

  v_c_main            uuid;

  v_cat_alcohol       uuid;
  v_cat_soft          uuid;
  v_cat_herbal        uuid;

  v_zone_wh_cold      uuid;
  v_zone_wh_dry       uuid;
  v_zone_store_sale   uuid;

  v_supply_invoice    uuid;
  v_sii1              uuid;
  v_sii2              uuid;

  v_dr                uuid;

  v_batch_jager       uuid;
  v_batch_cola        uuid;

  v_pli_store_jager   uuid;

  v_shift             uuid;

  v_pct               uuid;

  v_coupon            uuid;
  v_coupon_inst       uuid;

  v_receipt           uuid;
BEGIN
  -- ---------- base lookups ----------
  SELECT id INTO v_sl_store FROM storage_locations WHERE name='Магазин №1 Краснодар' LIMIT 1;
  SELECT id INTO v_sl_wh    FROM storage_locations WHERE name='Склад центральный' LIMIT 1;

  SELECT id INTO v_p_jager  FROM product WHERE barcode='4600000000011' LIMIT 1;
  SELECT id INTO v_p_cola   FROM product WHERE barcode='4600000000012' LIMIT 1;

  SELECT id INTO v_e_director FROM employees WHERE full_name='Иванов Иван Иванович' LIMIT 1;
  SELECT id INTO v_e_manager  FROM employees WHERE full_name='Петров Пётр Петрович' LIMIT 1;
  SELECT id INTO v_e_keeper   FROM employees WHERE full_name='Сидоров Сергей Сергеевич' LIMIT 1;

  SELECT id INTO v_c_main FROM contracts WHERE contract_number='CNT-2026-0001' LIMIT 1;

  -- Прайс-лист айтем для магазина по Jäger (если нет — некоторые блоки просто пропустятся)
  IF v_sl_store IS NOT NULL AND v_p_jager IS NOT NULL THEN
    SELECT pli.id
    INTO v_pli_store_jager
    FROM price_list_items pli
    JOIN price_lists pl ON pl.id = pli.price_list_id
    WHERE pl.type='DAILY'
      AND pl.effective_date='2026-01-04'
      AND pli.storage_location_id = v_sl_store
      AND pli.product_id = v_p_jager
    LIMIT 1;
  END IF;

  -- ---------- 1) categories ----------
  -- Алкоголь
  IF NOT EXISTS (SELECT 1 FROM product_categories WHERE name='Алкоголь') THEN
    INSERT INTO product_categories (name, parent_id)
    VALUES ('Алкоголь', NULL);
  END IF;
  SELECT id INTO v_cat_alcohol FROM product_categories WHERE name='Алкоголь' LIMIT 1;

  -- Безалкогольные напитки
  IF NOT EXISTS (SELECT 1 FROM product_categories WHERE name='Безалкогольные напитки') THEN
    INSERT INTO product_categories (name, parent_id)
    VALUES ('Безалкогольные напитки', NULL);
  END IF;
  SELECT id INTO v_cat_soft FROM product_categories WHERE name='Безалкогольные напитки' LIMIT 1;

  -- Травяные ликёры (дочерняя от Алкоголь)
  IF v_cat_alcohol IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM product_categories WHERE name='Травяные ликёры') THEN
      INSERT INTO product_categories (name, parent_id)
      VALUES ('Травяные ликёры', v_cat_alcohol);
    END IF;
  END IF;
  SELECT id INTO v_cat_herbal FROM product_categories WHERE name='Травяные ликёры' LIMIT 1;

  -- product_category_links (НЕ вставляем, если не нашли product/category)
  IF v_p_jager IS NOT NULL AND v_cat_herbal IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM product_category_links
      WHERE product_id=v_p_jager AND category_id=v_cat_herbal
    ) THEN
      INSERT INTO product_category_links (product_id, category_id)
      VALUES (v_p_jager, v_cat_herbal);
    END IF;
  ELSE
    RAISE NOTICE 'Skip link JAGER->herbal: product or category not found (p_jager=%, cat_herbal=%).', v_p_jager, v_cat_herbal;
  END IF;

  IF v_p_cola IS NOT NULL AND v_cat_soft IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM product_category_links
      WHERE product_id=v_p_cola AND category_id=v_cat_soft
    ) THEN
      INSERT INTO product_category_links (product_id, category_id)
      VALUES (v_p_cola, v_cat_soft);
    END IF;
  ELSE
    RAISE NOTICE 'Skip link COLA->soft: product or category not found (p_cola=%, cat_soft=%).', v_p_cola, v_cat_soft;
  END IF;

  -- ---------- 2) storage_zones ----------
  IF v_sl_wh IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM storage_zones z
      WHERE z.storage_location_id=v_sl_wh AND z.name='Склад/Холодильник'
    ) THEN
      INSERT INTO storage_zones (storage_location_id, name, zone_type, temperature_mode, capacity, is_active)
      VALUES (v_sl_wh, 'Склад/Холодильник', 'COLD', '0..+4', 5000, TRUE);
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM storage_zones z
      WHERE z.storage_location_id=v_sl_wh AND z.name='Склад/Сухая зона'
    ) THEN
      INSERT INTO storage_zones (storage_location_id, name, zone_type, temperature_mode, capacity, is_active)
      VALUES (v_sl_wh, 'Склад/Сухая зона', 'DRY', '+10..+25', 20000, TRUE);
    END IF;
  ELSE
    RAISE NOTICE 'Skip warehouse zones: storage_location "Склад центральный" not found.';
  END IF;

  IF v_sl_store IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM storage_zones z
      WHERE z.storage_location_id=v_sl_store AND z.name='Торговый зал'
    ) THEN
      INSERT INTO storage_zones (storage_location_id, name, zone_type, temperature_mode, capacity, is_active)
      VALUES (v_sl_store, 'Торговый зал', 'SALE', '+18..+25', 3000, TRUE);
    END IF;
  ELSE
    RAISE NOTICE 'Skip store zone: storage_location "Магазин №1 Краснодар" not found.';
  END IF;

  SELECT id INTO v_zone_wh_cold
  FROM storage_zones
  WHERE v_sl_wh IS NOT NULL AND storage_location_id=v_sl_wh AND name='Склад/Холодильник'
  LIMIT 1;

  SELECT id INTO v_zone_wh_dry
  FROM storage_zones
  WHERE v_sl_wh IS NOT NULL AND storage_location_id=v_sl_wh AND name='Склад/Сухая зона'
  LIMIT 1;

  SELECT id INTO v_zone_store_sale
  FROM storage_zones
  WHERE v_sl_store IS NOT NULL AND storage_location_id=v_sl_store AND name='Торговый зал'
  LIMIT 1;

  -- ---------- 3) supply_invoices + items + discrepancy + batches ----------
  IF v_c_main IS NOT NULL AND v_sl_wh IS NOT NULL AND v_zone_wh_dry IS NOT NULL
     AND v_e_keeper IS NOT NULL AND v_e_manager IS NOT NULL THEN

    IF NOT EXISTS (SELECT 1 FROM supply_invoices si WHERE si.invoice_number='SI-2026-0001') THEN
      INSERT INTO supply_invoices (
        contract_id, storage_location_id,
        invoice_number, expected_date, actual_date, status,
        received_zone_id, storekeeper_id, merchandiser_id
      )
      VALUES (
        v_c_main, v_sl_wh,
        'SI-2026-0001', '2026-01-04', '2026-01-04', 'received',
        v_zone_wh_dry, v_e_keeper, v_e_manager
      );
    END IF;

    SELECT id INTO v_supply_invoice FROM supply_invoices WHERE invoice_number='SI-2026-0001' LIMIT 1;

    -- items
    IF v_supply_invoice IS NOT NULL AND v_p_jager IS NOT NULL THEN
      IF NOT EXISTS (
        SELECT 1 FROM supply_invoice_items sii
        WHERE sii.supply_invoice_id=v_supply_invoice AND sii.line_no=1
      ) THEN
        INSERT INTO supply_invoice_items (
          supply_invoice_id, line_no, product_id,
          quantity_expected, purchase_price,
          quantity_actual, line_status,
          fact_entered_by, fact_entered_at
        )
        VALUES (
          v_supply_invoice, 1, v_p_jager,
          10.00, 800.00,
          10.00, 'received',
          v_e_keeper, now()
        );
      END IF;
    ELSE
      RAISE NOTICE 'Skip supply_invoice_item line 1 (Jager): invoice or product not found (invoice=%, p_jager=%).', v_supply_invoice, v_p_jager;
    END IF;

    IF v_supply_invoice IS NOT NULL AND v_p_cola IS NOT NULL THEN
      IF NOT EXISTS (
        SELECT 1 FROM supply_invoice_items sii
        WHERE sii.supply_invoice_id=v_supply_invoice AND sii.line_no=2
      ) THEN
        INSERT INTO supply_invoice_items (
          supply_invoice_id, line_no, product_id,
          quantity_expected, purchase_price,
          quantity_actual, line_status,
          fact_entered_by, fact_entered_at
        )
        VALUES (
          v_supply_invoice, 2, v_p_cola,
          30.00, 60.00,
          29.00, 'received',
          v_e_keeper, now()
        );
      END IF;
    ELSE
      RAISE NOTICE 'Skip supply_invoice_item line 2 (Cola): invoice or product not found (invoice=%, p_cola=%).', v_supply_invoice, v_p_cola;
    END IF;

    SELECT id INTO v_sii1 FROM supply_invoice_items WHERE v_supply_invoice IS NOT NULL AND supply_invoice_id=v_supply_invoice AND line_no=1 LIMIT 1;
    SELECT id INTO v_sii2 FROM supply_invoice_items WHERE v_supply_invoice IS NOT NULL AND supply_invoice_id=v_supply_invoice AND line_no=2 LIMIT 1;

    -- discrepancy (по коле)
    IF v_supply_invoice IS NOT NULL AND v_sii2 IS NOT NULL AND v_p_cola IS NOT NULL THEN
      IF NOT EXISTS (SELECT 1 FROM discrepancy_requests dr WHERE dr.description='Недостача при приемке (1 шт.)') THEN
        INSERT INTO discrepancy_requests (
          supply_invoice_id, supply_invoice_item_id,
          product_id, discrepancy_type, description,
          quantity_expected, quantity_actual, status,
          created_by_employee_id
        )
        VALUES (
          v_supply_invoice, v_sii2,
          v_p_cola, 'SHORTAGE', 'Недостача при приемке (1 шт.)',
          30.00, 29.00, 'OPEN',
          v_e_keeper
        );
      END IF;

      SELECT id INTO v_dr FROM discrepancy_requests WHERE description='Недостача при приемке (1 шт.)' LIMIT 1;
    END IF;

    -- batches
    IF v_supply_invoice IS NOT NULL AND v_p_jager IS NOT NULL THEN
      IF NOT EXISTS (
        SELECT 1 FROM batches b
        WHERE b.supply_invoice_id=v_supply_invoice AND b.product_id=v_p_jager
      ) THEN
        INSERT INTO batches (
          product_id, supply_invoice_id,
          manufacture_date, expiration_date,
          purchase_price, initial_quantity
        )
        VALUES (
          v_p_jager, v_supply_invoice,
          '2025-12-15', '2027-12-15',
          800.00, 10.00
        );
      END IF;

      SELECT id INTO v_batch_jager
      FROM batches
      WHERE supply_invoice_id=v_supply_invoice AND product_id=v_p_jager
      ORDER BY created_at DESC
      LIMIT 1;
    END IF;

    IF v_supply_invoice IS NOT NULL AND v_p_cola IS NOT NULL THEN
      IF NOT EXISTS (
        SELECT 1 FROM batches b
        WHERE b.supply_invoice_id=v_supply_invoice AND b.product_id=v_p_cola
      ) THEN
        INSERT INTO batches (
          product_id, supply_invoice_id,
          manufacture_date, expiration_date,
          purchase_price, initial_quantity
        )
        VALUES (
          v_p_cola, v_supply_invoice,
          '2025-12-20', '2026-06-20',
          60.00, 29.00
        );
      END IF;

      SELECT id INTO v_batch_cola
      FROM batches
      WHERE supply_invoice_id=v_supply_invoice AND product_id=v_p_cola
      ORDER BY created_at DESC
      LIMIT 1;
    END IF;

    -- link items -> batch_id + discrepancy_request_id
    IF v_supply_invoice IS NOT NULL THEN
      UPDATE supply_invoice_items sii
      SET
        batch_id = CASE
          WHEN sii.line_no=1 THEN v_batch_jager
          WHEN sii.line_no=2 THEN v_batch_cola
          ELSE sii.batch_id
        END,
        discrepancy_request_id = CASE
          WHEN sii.line_no=2 THEN v_dr
          ELSE NULL
        END
      WHERE sii.supply_invoice_id=v_supply_invoice;
    END IF;

  ELSE
    RAISE NOTICE 'Skip supply block: missing prereqs (contract=%, sl_wh=%, zone_wh_dry=%, e_keeper=%, e_manager=%).',
      v_c_main, v_sl_wh, v_zone_wh_dry, v_e_keeper, v_e_manager;
  END IF;

  -- ---------- 4) batch_locations ----------
  IF v_batch_jager IS NOT NULL AND v_zone_wh_dry IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM batch_locations bl WHERE bl.batch_id=v_batch_jager AND bl.storage_zone_id=v_zone_wh_dry) THEN
      INSERT INTO batch_locations (batch_id, storage_zone_id, quantity)
      VALUES (v_batch_jager, v_zone_wh_dry, 10.00);
    END IF;
  END IF;

  IF v_batch_cola IS NOT NULL AND v_zone_wh_dry IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM batch_locations bl WHERE bl.batch_id=v_batch_cola AND bl.storage_zone_id=v_zone_wh_dry) THEN
      INSERT INTO batch_locations (batch_id, storage_zone_id, quantity)
      VALUES (v_batch_cola, v_zone_wh_dry, 29.00);
    END IF;
  END IF;

  -- ---------- 5) shifts + shift_operations ----------
  IF v_e_manager IS NOT NULL AND v_e_director IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM shifts s
      WHERE s.employee_id=v_e_manager AND s.date='2026-01-04'
    ) THEN
      INSERT INTO shifts (employee_id, date, start_time, end_time, status, responsible_id)
      VALUES (v_e_manager, '2026-01-04', '12:00', '16:00', 'CLOSED', v_e_director);
    END IF;

    SELECT id INTO v_shift
    FROM shifts
    WHERE employee_id=v_e_manager AND date='2026-01-04'
    LIMIT 1;

    IF v_shift IS NOT NULL AND v_supply_invoice IS NOT NULL THEN
      IF NOT EXISTS (
        SELECT 1 FROM shift_operations so
        WHERE so.reference_table='supply_invoices' AND so.reference_id=v_supply_invoice
      ) THEN
        INSERT INTO shift_operations (shift_id, operation_type, reference_id, reference_table)
        VALUES (v_shift, 'RECEIVE_SUPPLY', v_supply_invoice, 'supply_invoices');
      END IF;
    END IF;
  END IF;

  -- ---------- 6) price_change_tasks + items ----------
  IF v_e_manager IS NOT NULL AND v_sl_store IS NOT NULL THEN
    IF NOT EXISTS (
      SELECT 1 FROM price_change_tasks pct
      WHERE pct.created_date='2026-01-04' AND pct.storage_location_id=v_sl_store
    ) THEN
      INSERT INTO price_change_tasks (
        created_date, created_by_employee_id, storage_location_id,
        status, started_at, completed_at,
        total_products, prices_updated, prices_restricted
      )
      VALUES (
        '2026-01-04', v_e_manager, v_sl_store,
        'COMPLETED', now() - interval '10 minutes', now(),
        2, 1, 1
      );
    END IF;

    SELECT id INTO v_pct
    FROM price_change_tasks
    WHERE created_date='2026-01-04' AND storage_location_id=v_sl_store
    LIMIT 1;

    -- task_items: jager (если есть)
    IF v_pct IS NOT NULL AND v_p_jager IS NOT NULL THEN
      IF NOT EXISTS (
        SELECT 1 FROM price_change_task_items pcti
        WHERE pcti.task_id=v_pct AND pcti.product_id=v_p_jager
      ) THEN
        INSERT INTO price_change_task_items (
          task_id, product_id, selected_price_list_item_id,
          selected_final_price, regular_price,
          label_type, restriction_applied, restriction_reason,
          stop_list_id, coupon_id, batch_id
        )
        VALUES (
          v_pct, v_p_jager, v_pli_store_jager,
          920.00, 920.00,
          'PRICE', TRUE, 'LIMIT_DAILY_CHANGE',
          (SELECT id FROM stop_list WHERE product_id=v_p_jager AND effective_date='2026-01-04' LIMIT 1),
          NULL,
          v_batch_jager
        );
      END IF;
    END IF;

    -- (опционально) cola можно добавить по аналогии, если у тебя это нужно
  END IF;

  -- ---------- 7) coupons + instances + label_print_queue ----------
  IF v_p_jager IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM coupons c WHERE c.code='JAGER-10') THEN
      INSERT INTO coupons (
        name, code, description,
        discount_percent, valid_from, valid_until,
        min_purchase, product_id, batch_id, is_active
      )
      VALUES (
        'Скидка на Jägermeister',
        'JAGER-10',
        'Скидка 10% на Jägermeister 0.7L',
        10.00,
        '2026-01-04',
        '2026-01-31',
        500.00,
        v_p_jager,
        v_batch_jager,
        TRUE
      );
    END IF;

    SELECT id INTO v_coupon FROM coupons WHERE code='JAGER-10' LIMIT 1;

    IF v_coupon IS NOT NULL THEN
      IF NOT EXISTS (SELECT 1 FROM coupon_instances ci WHERE ci.instance_code='JAGER-10-000001') THEN
        INSERT INTO coupon_instances (coupon_id, instance_code, printed_date, printed_by)
        VALUES (v_coupon, 'JAGER-10-000001', now(), v_e_manager);
      END IF;

      SELECT id INTO v_coupon_inst FROM coupon_instances WHERE instance_code='JAGER-10-000001' LIMIT 1;
    END IF;

    IF v_pli_store_jager IS NOT NULL AND v_e_manager IS NOT NULL THEN
      IF NOT EXISTS (
        SELECT 1 FROM label_print_queue lpq
        WHERE lpq.product_id=v_p_jager AND lpq.label_type='PRICE'
      ) THEN
        INSERT INTO label_print_queue (product_id, price_list_item_id, label_type, print_status, print_date, printed_by)
        VALUES (v_p_jager, v_pli_store_jager, 'PRICE', 'PRINTED', now(), v_e_manager);
      END IF;
    END IF;
  END IF;

  -- ---------- 8) sales_receipts + sales_items + redeem coupon ----------
  IF v_e_manager IS NOT NULL AND v_sl_store IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM sales_receipts sr WHERE sr.receipt_number='R-2026-0001') THEN
      INSERT INTO sales_receipts (
        receipt_number, date, employee_id, storage_location_id,
        total_amount, payment_method, status, customer_name
      )
      VALUES (
        'R-2026-0001', now(), v_e_manager, v_sl_store,
        920.00, 'CARD', 'PAID', 'Покупатель'
      );
    END IF;

    SELECT id INTO v_receipt FROM sales_receipts WHERE receipt_number='R-2026-0001' LIMIT 1;

    IF v_receipt IS NOT NULL AND v_p_jager IS NOT NULL THEN
      IF NOT EXISTS (
        SELECT 1 FROM sales_items si
        WHERE si.receipt_id=v_receipt
      ) THEN
        INSERT INTO sales_items (receipt_id, product_id, batch_id, quantity, unit_price, total_price)
        VALUES (v_receipt, v_p_jager, v_batch_jager, 1.00, 920.00, 920.00);
      END IF;
    END IF;

    -- redeem coupon instance
    IF v_coupon_inst IS NOT NULL THEN
      UPDATE coupon_instances
      SET redeemed_at = now(),
          redeemed_receipt_id = v_receipt
      WHERE id = v_coupon_inst
        AND redeemed_receipt_id IS NULL;
    END IF;
  END IF;

END $$;
