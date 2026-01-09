-- V4.0 seed: минимальные данные для проверки price_history / price endpoints
-- Требуется: manufacturer, positions, departments уже есть (они добавляются V2_1__seed_reference.sql)

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) products (используем manufacturer по code)
WITH m_jager AS (
  SELECT id FROM manufacturer WHERE code='M_JAGER' LIMIT 1
),
m_cola AS (
  SELECT id FROM manufacturer WHERE code='M_COCA' LIMIT 1
)
INSERT INTO product (name, manufacturer_id, unit_of_measure, shelf_life_days, barcode, additional_info)
SELECT 'Jägermeister 0.7', (SELECT id FROM m_jager), 'pcs', 3650, '4600000000011', 'seed demo product'
WHERE (SELECT id FROM m_jager) IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM product WHERE barcode='4600000000011');

WITH m_cola AS (
  SELECT id FROM manufacturer WHERE code='M_COCA' LIMIT 1
)
INSERT INTO product (name, manufacturer_id, unit_of_measure, shelf_life_days, barcode, additional_info)
SELECT 'Coca-Cola 0.5', (SELECT id FROM m_cola), 'pcs', 365, '4600000000012', 'seed demo product'
WHERE (SELECT id FROM m_cola) IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM product WHERE barcode='4600000000012');


-- 2) employee (passport_data NOT NULL!)
WITH pos AS (
  SELECT id FROM positions WHERE name='Менеджер' LIMIT 1
),
dep AS (
  SELECT id FROM departments WHERE name='Администрация' LIMIT 1
)
INSERT INTO employees (full_name, passport_data, birth_date, position_id, department_id, employment_date, employment_status, email)
SELECT
  'Петров Пётр Петрович',
  '{"series":"0000","number":"000000","issuedBy":"seed","issuedDate":"2020-01-01"}'::jsonb,
  DATE '1995-01-01',
  (SELECT id FROM pos),
  (SELECT id FROM dep),
  DATE '2025-01-01',
  'active',
  'petrov@example.local'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE full_name='Петров Пётр Петрович');
