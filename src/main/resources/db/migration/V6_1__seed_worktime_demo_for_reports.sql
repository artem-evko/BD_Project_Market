-- V6_1__seed_worktime_demo_for_reports.sql
-- Неделя: 2026-01-06 .. 2026-01-12

-- ---------- CASHIER ----------
WITH
cashier_emp AS (
  SELECT ua.employee_id
  FROM user_accounts ua
  WHERE ua.login = 'cashier1' AND ua.is_active = true
  LIMIT 1
)
INSERT INTO employee_worktime (
  employee_id,
  date,
  scheduled_start, scheduled_end,
  actual_login, actual_logout,
  status,
  auto_closed, auto_close_reason,
  confirmation_sent_at, confirmation_deadline_at,
  confirmation_result, confirmation_response_at
)
SELECT
  (SELECT employee_id FROM cashier_emp),
  d::date,
  (d::date + time '09:00')::timestamp, (d::date + time '18:00')::timestamp,
  (d::date + time '09:05')::timestamp,
  CASE WHEN d::date = DATE '2026-01-10' THEN NULL
       ELSE (d::date + time '18:10')::timestamp
  END,
  'draft',
  CASE WHEN d::date = DATE '2026-01-10' THEN true ELSE false END,
  CASE WHEN d::date = DATE '2026-01-10' THEN 'no response after 24:00 prompt' ELSE NULL END,
  CASE WHEN d::date = DATE '2026-01-10' THEN (d::date + time '23:55')::timestamp ELSE NULL END,
  CASE WHEN d::date = DATE '2026-01-10' THEN (d::date + time '24:00')::timestamp ELSE NULL END,
  CASE WHEN d::date = DATE '2026-01-10' THEN 'no_response' ELSE 'stop' END,
  CASE WHEN d::date = DATE '2026-01-10' THEN NULL ELSE (d::date + time '18:11')::timestamp END
FROM generate_series('2026-01-06'::date, '2026-01-12'::date, interval '1 day') d
WHERE (SELECT employee_id FROM cashier_emp) IS NOT NULL;

-- ---------- MANAGER ----------
WITH
manager_emp AS (
  SELECT ua.employee_id
  FROM user_accounts ua
  WHERE ua.login = 'manager1' AND ua.is_active = true
  LIMIT 1
)
INSERT INTO employee_worktime (
  employee_id, date,
  scheduled_start, scheduled_end,
  actual_login, actual_logout,
  status, auto_closed, confirmation_result
)
SELECT
  (SELECT employee_id FROM manager_emp),
  d::date,
  (d::date + time '10:00')::timestamp, (d::date + time '19:00')::timestamp,
  (d::date + time '10:00')::timestamp, (d::date + time '19:05')::timestamp,
  'draft', false, 'stop'
FROM generate_series('2026-01-06'::date, '2026-01-08'::date, interval '1 day') d
WHERE (SELECT employee_id FROM manager_emp) IS NOT NULL;
