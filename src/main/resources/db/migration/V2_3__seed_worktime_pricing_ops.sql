-- V2.3 seed: данные для 2.19..2.36 (worktime/pricing/ops/logs/docs/orders etc)
-- Безопасно: если каких-то записей нет (director/manager/employees/price-list) — вставит 0 строк и миграция не упадёт.

-- 1) notifications (recipient_id NOT NULL)
INSERT INTO notifications (recipient_id, title, message)
SELECT ua.id,
       'Подтверждение рабочего времени',
       'Пожалуйста, подтвердите рабочее время за 2026-01-04'
FROM user_accounts ua
WHERE ua.login = 'director'
  AND NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.title = 'Подтверждение рабочего времени'
  );

-- 2) work_schedule_templates (типовой график)

-- директор
INSERT INTO work_schedule_templates (position_id, employee_id, weekday, start_time, end_time, hours, is_day_off)
SELECT pos.id, emp.id, d.weekday,
       CASE WHEN d.weekday BETWEEN 1 AND 5 THEN '09:00'::time ELSE NULL END,
       CASE WHEN d.weekday BETWEEN 1 AND 5 THEN '18:00'::time ELSE NULL END,
       CASE WHEN d.weekday BETWEEN 1 AND 5 THEN 8.00 ELSE 0.00 END,
       CASE WHEN d.weekday BETWEEN 6 AND 7 THEN TRUE ELSE FALSE END
FROM (VALUES (1),(2),(3),(4),(5),(6),(7)) AS d(weekday)
JOIN positions pos ON pos.name='Директор'
JOIN employees emp ON emp.full_name='Иванов Иван Иванович'
WHERE NOT EXISTS (
  SELECT 1 FROM work_schedule_templates wst
  WHERE wst.employee_id=emp.id AND wst.weekday=d.weekday
);

-- manager
INSERT INTO work_schedule_templates (position_id, employee_id, weekday, start_time, end_time, hours, is_day_off)
SELECT pos.id, emp.id, d.weekday,
       CASE WHEN d.weekday BETWEEN 1 AND 5 THEN '10:00'::time ELSE NULL END,
       CASE WHEN d.weekday BETWEEN 1 AND 5 THEN '19:00'::time ELSE NULL END,
       CASE WHEN d.weekday BETWEEN 1 AND 5 THEN 8.00 ELSE 0.00 END,
       CASE WHEN d.weekday BETWEEN 6 AND 7 THEN TRUE ELSE FALSE END
FROM (VALUES (1),(2),(3),(4),(5),(6),(7)) AS d(weekday)
JOIN positions pos ON pos.name='Менеджер'
JOIN employees emp ON emp.full_name='Петров Пётр Петрович'
WHERE NOT EXISTS (
  SELECT 1 FROM work_schedule_templates wst
  WHERE wst.employee_id=emp.id AND wst.weekday=d.weekday
);

-- keeper
INSERT INTO work_schedule_templates (position_id, employee_id, weekday, start_time, end_time, hours, is_day_off)
SELECT pos.id, emp.id, d.weekday,
       CASE WHEN d.weekday BETWEEN 1 AND 5 THEN '08:00'::time ELSE NULL END,
       CASE WHEN d.weekday BETWEEN 1 AND 5 THEN '17:00'::time ELSE NULL END,
       CASE WHEN d.weekday BETWEEN 1 AND 5 THEN 8.00 ELSE 0.00 END,
       CASE WHEN d.weekday BETWEEN 6 AND 7 THEN TRUE ELSE FALSE END
FROM (VALUES (1),(2),(3),(4),(5),(6),(7)) AS d(weekday)
JOIN positions pos ON pos.name='Кладовщик'
JOIN employees emp ON emp.full_name='Сидоров Сергей Сергеевич'
WHERE NOT EXISTS (
  SELECT 1 FROM work_schedule_templates wst
  WHERE wst.employee_id=emp.id AND wst.weekday=d.weekday
);

-- 3) employee_schedule (на 2026-01-04 = воскресенье)

-- директор OFF
INSERT INTO employee_schedule (employee_id, template_id, date, planned_start, planned_end, schedule_type)
SELECT
  ed.id,
  wst.id,
  '2026-01-04'::date, NULL, NULL, 'OFF'
FROM employees ed
JOIN work_schedule_templates wst
  ON wst.employee_id = ed.id AND wst.weekday = 7
WHERE ed.full_name='Иванов Иван Иванович'
  AND NOT EXISTS (
    SELECT 1 FROM employee_schedule es
    WHERE es.employee_id=ed.id AND es.date='2026-01-04'::date
  );

-- менеджеру корректировка 12:00-16:00, corrected_by = директор
INSERT INTO employee_schedule (employee_id, template_id, date, planned_start, planned_end, correction_reason, corrected_by, schedule_type)
SELECT
  em.id,
  wst.id,
  '2026-01-04'::date, '12:00'::time, '16:00'::time,
  'Подмена смены/инвентаризация', ed.id,
  'CORRECTION'
FROM employees em
JOIN employees ed ON ed.full_name='Иванов Иван Иванович'
JOIN work_schedule_templates wst
  ON wst.employee_id = em.id AND wst.weekday = 7
WHERE em.full_name='Петров Пётр Петрович'
  AND NOT EXISTS (
    SELECT 1 FROM employee_schedule es
    WHERE es.employee_id=em.id AND es.date='2026-01-04'::date
  );

-- 4) employee_worktime (факт логин/логаут)
INSERT INTO employee_worktime (
  employee_id, date, scheduled_start, scheduled_end,
  actual_login, actual_logout, status, auto_closed,
  confirmation_sent_at, confirmation_deadline_at, confirmation_result,
  notification_id
)
SELECT
  ed.id,
  '2026-01-04'::date,
  NULL, NULL,
  '2026-01-04 09:05:00'::timestamp,
  '2026-01-04 14:10:00'::timestamp,
  'RECORDED',
  FALSE,
  now(), now() + interval '1 day', 'PENDING',
  n.id
FROM employees ed
LEFT JOIN LATERAL (
  SELECT id
  FROM notifications
  WHERE title='Подтверждение рабочего времени'
  ORDER BY created_at DESC
  LIMIT 1
) n ON TRUE
WHERE ed.full_name='Иванов Иван Иванович'
  AND NOT EXISTS (
    SELECT 1 FROM employee_worktime ewt
    WHERE ewt.employee_id=ed.id AND ewt.date='2026-01-04'::date
  );

-- 5) work_report_headers (недельный отчёт, старт недели 2025-12-29)
INSERT INTO work_report_headers (week_start, storage_location_id, director_id, status)
SELECT '2025-12-29'::date, sl.id, ed.id, 'DRAFT'
FROM storage_locations sl
JOIN employees ed ON ed.full_name='Иванов Иван Иванович'
WHERE sl.name='Магазин №1 Краснодар'
  AND NOT EXISTS (
    SELECT 1 FROM work_report_headers wrh
    WHERE wrh.week_start='2025-12-29'::date AND wrh.storage_location_id=sl.id
  );

-- lines: директор/менеджер/кладовщик
INSERT INTO work_report_lines (report_id, employee_id, computed_hours, norm_hours, delta_hours, director_override_delta, override_reason)
SELECT
  wrh.id, ed.id,
  34.00, 40.00, -6.00, -4.00, 'Учитываем участие в инвентаризации'
FROM work_report_headers wrh
JOIN storage_locations sl ON sl.id=wrh.storage_location_id AND sl.name='Магазин №1 Краснодар'
JOIN employees ed ON ed.full_name='Иванов Иван Иванович'
WHERE wrh.week_start='2025-12-29'::date
  AND NOT EXISTS (
    SELECT 1 FROM work_report_lines wrl
    WHERE wrl.report_id=wrh.id AND wrl.employee_id=ed.id
  );

INSERT INTO work_report_lines (report_id, employee_id, computed_hours, norm_hours, delta_hours)
SELECT
  wrh.id, em.id,
  40.00, 40.00, 0.00
FROM work_report_headers wrh
JOIN storage_locations sl ON sl.id=wrh.storage_location_id AND sl.name='Магазин №1 Краснодар'
JOIN employees em ON em.full_name='Петров Пётр Петрович'
WHERE wrh.week_start='2025-12-29'::date
  AND NOT EXISTS (
    SELECT 1 FROM work_report_lines wrl
    WHERE wrl.report_id=wrh.id AND wrl.employee_id=em.id
  );

INSERT INTO work_report_lines (report_id, employee_id, computed_hours, norm_hours, delta_hours)
SELECT
  wrh.id, ek.id,
  42.00, 40.00, 2.00
FROM work_report_headers wrh
JOIN storage_locations sl ON sl.id=wrh.storage_location_id AND sl.name='Магазин №1 Краснодар'
JOIN employees ek ON ek.full_name='Сидоров Сергей Сергеевич'
WHERE wrh.week_start='2025-12-29'::date
  AND NOT EXISTS (
    SELECT 1 FROM work_report_lines wrl
    WHERE wrl.report_id=wrh.id AND wrl.employee_id=ek.id
  );

-- 6) vacation_balance + vacations + sick_leaves
INSERT INTO vacation_balance (employee_id, year, carried_over, accrued, used)
SELECT em.id, 2026, 2.00, 28.00, 0.00
FROM employees em
WHERE em.full_name='Петров Пётр Петрович'
  AND NOT EXISTS (
    SELECT 1 FROM vacation_balance vb
    WHERE vb.employee_id=em.id AND vb.year=2026
  );

INSERT INTO vacations (employee_id, date_start, date_end, status, approved_by_employee_id, approved_at)
SELECT em.id, '2026-02-10'::date, '2026-02-20'::date, 'APPROVED', ed.id, now()
FROM employees em
JOIN employees ed ON ed.full_name='Иванов Иван Иванович'
WHERE em.full_name='Петров Пётр Петрович'
  AND NOT EXISTS (
    SELECT 1 FROM vacations v
    WHERE v.employee_id=em.id AND v.date_start='2026-02-10'::date
  );

INSERT INTO sick_leaves (employee_id, date_start, date_end, status, doc_required, doc_due_date)
SELECT ek.id, '2026-01-02'::date, '2026-01-06'::date, 'OPEN', TRUE, '2026-01-10'::date
FROM employees ek
WHERE ek.full_name='Сидоров Сергей Сергеевич'
  AND NOT EXISTS (
    SELECT 1 FROM sick_leaves s
    WHERE s.employee_id=ek.id AND s.date_start='2026-01-02'::date
  );

-- 7) price_restrictions
INSERT INTO price_restrictions (
  scope_type, storage_location_id, product_id,
  max_daily_change_percent, max_markup_percent,
  allow_daily_change_exception_for_auto_markdown,
  min_price, max_price
)
SELECT
  'product',
  sl.id,
  p.id,
  5.00, 15.00,
  FALSE,
  700.00, 1500.00
FROM storage_locations sl
JOIN product p ON p.barcode='4600000000011'
WHERE sl.name='Магазин №1 Краснодар'
  AND NOT EXISTS (
    SELECT 1 FROM price_restrictions pr
    WHERE pr.scope_type='product'
      AND pr.storage_location_id=sl.id
      AND pr.product_id=p.id
  );

-- 8) stop_list (нужен price_list_item_id)
INSERT INTO stop_list (
  storage_location_id, effective_date, product_id, price_list_item_id,
  candidate_price, prev_price, input_price, violations,
  reason, status, sent_to_hq_at
)
SELECT
  sl.id,
  '2026-01-04'::date,
  p.id,
  pli.id,
  1300.00,
  900.00,
  800.00,
  '{"daily_change_limit": true, "markup_cap": true}'::jsonb,
  'Авторасчёт превысил ограничения',
  'NEW',
  NULL
FROM storage_locations sl
JOIN product p ON p.barcode='4600000000011'
JOIN price_list_items pli ON pli.product_id=p.id AND pli.storage_location_id=sl.id
JOIN price_lists pl ON pl.id=pli.price_list_id AND pl.type='DAILY' AND pl.effective_date='2026-01-04'::date
WHERE sl.name='Магазин №1 Краснодар'
  AND NOT EXISTS (
    SELECT 1 FROM stop_list s
    WHERE s.storage_location_id=sl.id
      AND s.product_id=p.id
      AND s.effective_date='2026-01-04'::date
  );

-- 9) price_history
INSERT INTO price_history (product_id, old_price, new_price, changed_by, comment)
SELECT p.id, 900.00, 920.00, ed.id, 'Прайс-лист на день'
FROM product p
JOIN employees ed ON ed.full_name='Иванов Иван Иванович'
WHERE p.barcode='4600000000011'
  AND NOT EXISTS (
    SELECT 1 FROM price_history ph
    WHERE ph.product_id=p.id AND ph.old_price=900.00 AND ph.new_price=920.00
  );

-- 10) store_prices
INSERT INTO store_prices (
  storage_location_id, product_id, effective_date,
  price, regular_price, price_type_applied,
  source_price_list_item_id, task_id
)
SELECT
  sl.id,
  p.id,
  '2026-01-04'::date,
  920.00,
  920.00,
  'RETAIL',
  pli.id,
  NULL
FROM storage_locations sl
JOIN product p ON p.barcode='4600000000011'
JOIN price_list_items pli ON pli.product_id=p.id AND pli.storage_location_id=sl.id
JOIN price_lists pl ON pl.id=pli.price_list_id AND pl.type='DAILY' AND pl.effective_date='2026-01-04'::date
WHERE sl.name='Магазин №1 Краснодар'
  AND NOT EXISTS (
    SELECT 1 FROM store_prices sp
    WHERE sp.storage_location_id=sl.id
      AND sp.product_id=p.id
      AND sp.effective_date='2026-01-04'::date
  );

-- 11) returns
INSERT INTO returns (type, product_id, quantity, date_returned, reason, employee_id, contractor_id)
SELECT
  'customer', p.id, 1.00, '2026-01-04'::date,
  'Брак упаковки', em.id, NULL
FROM product p
JOIN employees em ON em.full_name='Петров Пётр Петрович'
WHERE p.barcode='4600000000012'
  AND NOT EXISTS (
    SELECT 1 FROM returns r
    WHERE r.type='customer' AND r.product_id=p.id AND r.date_returned='2026-01-04'::date
  );

-- 12) write_offs
INSERT INTO write_offs (
  product_id, quantity, reason, date_written_off, employee_id,
  batch_id, storage_zone_id, document_number, comment, status,
  approved_by_director_id, approved_at
)
SELECT
  p.id,
  2.00,
  'EXPIRED',
  '2026-01-04'::date,
  ek.id,
  NULL, NULL,
  'WO-2026-0001',
  'Списание тестовое',
  'APPROVED',
  ed.id,
  now()
FROM product p
JOIN employees ek ON ek.full_name='Сидоров Сергей Сергеевич'
JOIN employees ed ON ed.full_name='Иванов Иван Иванович'
WHERE p.barcode='4600000000012'
  AND NOT EXISTS (
    SELECT 1 FROM write_offs w
    WHERE w.document_number='WO-2026-0001'
  );

-- 13) orders + order_items
INSERT INTO orders (customer_name, phone, status, total_sum, created_by)
SELECT 'Иван Покупатель', '+7-999-000-00-00', 'NEW', 920.00, em.id
FROM employees em
WHERE em.full_name='Петров Пётр Петрович'
  AND NOT EXISTS (
    SELECT 1 FROM orders o
    WHERE o.customer_name='Иван Покупатель' AND o.order_date::date = now()::date
  );

INSERT INTO order_items (order_id, product_id, quantity, price)
SELECT
  o.id,
  p.id,
  1.00,
  920.00
FROM product p
JOIN LATERAL (
  SELECT id FROM orders
  WHERE customer_name='Иван Покупатель'
  ORDER BY order_date DESC
  LIMIT 1
) o ON TRUE
WHERE p.barcode='4600000000011'
  AND NOT EXISTS (
    SELECT 1 FROM order_items oi
    WHERE oi.order_id=o.id AND oi.product_id=p.id
  );

-- 14) exchange_log
INSERT INTO exchange_log (entity_name, entity_id, operation, direction, status, message)
SELECT 'price_lists',
       pl.id,
       'insert', 'upload', 'success', 'Отправлено в ГК'
FROM price_lists pl
WHERE pl.effective_date='2026-01-04'::date
  AND NOT EXISTS (
    SELECT 1 FROM exchange_log el
    WHERE el.entity_name='price_lists' AND el.status='success' AND el.message='Отправлено в ГК'
  )
LIMIT 1;

-- 15) access_logs (только если director существует)
INSERT INTO access_logs (user_id, action, entity, entity_id, ip_address)
SELECT ua.id, 'LOGIN', 'user_accounts', ua.id, '127.0.0.1'
FROM user_accounts ua
WHERE ua.login='director'
  AND NOT EXISTS (
    SELECT 1 FROM access_logs al
    WHERE al.action='LOGIN' AND al.user_id=ua.id
  );

-- 16) documents + document_files
INSERT INTO documents (doc_type, related_entity, related_id, created_by, status)
SELECT
  'WORK_REPORT',
  'work_report_headers',
  wrh.id,
  ed.id,
  'CREATED'
FROM work_report_headers wrh
JOIN storage_locations sl ON sl.id=wrh.storage_location_id AND sl.name='Магазин №1 Краснодар'
JOIN employees ed ON ed.full_name='Иванов Иван Иванович'
WHERE wrh.week_start='2025-12-29'::date
  AND NOT EXISTS (
    SELECT 1 FROM documents d
    WHERE d.doc_type='WORK_REPORT'
  )
LIMIT 1;

INSERT INTO document_files (document_id, file_name, file_path)
SELECT d.id, 'work_report_2025-12-29.pdf', '/files/docs/work_report_2025-12-29.pdf'
FROM (
  SELECT id
  FROM documents
  WHERE doc_type='WORK_REPORT'
  ORDER BY created_at DESC
  LIMIT 1
) d
WHERE NOT EXISTS (
  SELECT 1 FROM document_files df
  WHERE df.file_name='work_report_2025-12-29.pdf'
);
