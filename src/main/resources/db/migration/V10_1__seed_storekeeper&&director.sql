-- V10_1__seed_storekeeper_and_director.sql

-- 1. Роль STOREKEEPER (если вдруг ещё нет)
INSERT INTO roles (id, code, name, description)
VALUES (
    '8f4e5d33-aaaa-bbbb-cccc-1234567890ab',
    'STOREKEEPER',
    'Кладовщик',
    'Работа со складскими операциями'
)
ON CONFLICT (id) DO NOTHING;

-- 2. Активный кладовщик текущей ТТ
INSERT INTO employees (
    full_name,
    passport_data,
    birth_date,
    position_id,
    department_id,
    storage_location_id,
    employment_date,
    employment_status,
    work_phone,
    personal_phone,
    email,
    created_at,
    updated_at
)
SELECT
    'Иванов Илья Кладовщик',
    '{
      "type": "internal_rf",
      "series": "9999",
      "number": "111222",
      "registrationAddress": "г. Краснодар",
      "issueDate": "2015-01-01"
    }'::jsonb,
    '1990-02-02',
    p.id,
    d.id,
    sl.id,
    '2024-01-15',
    'active',
    '+7-900-400-00-01',
    '+7-900-400-10-01',
    'storekeeper_active@example.com',
    now(),
    now()
FROM positions p, departments d, storage_locations sl
WHERE p.name = 'Кладовщик'
  AND d.name = 'Склад'
  AND sl.id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT DO NOTHING;

INSERT INTO user_accounts (
    employee_id,
    login,
    password_hash,
    role_id,
    is_active
)
SELECT
    e.id,
    'storekeeper2',
    '$2b$10$0EuZmP13zlFOTHUWAPReFeyH8Lz/ACNTyVkX0R2dQTthIszZl6S.y', -- Admin123!
    r.id,
    TRUE
FROM employees e, roles r
WHERE e.email = 'storekeeper_active@example.com'
  AND r.code = 'STOREKEEPER'
ON CONFLICT (login) DO NOTHING;

-- =========================
-- DIRECTOR: ensure role/position/department, then create employee + user_account
-- =========================

-- 0) Роль DIRECTOR (если нет)
INSERT INTO roles (id, code, name, description)
SELECT
    gen_random_uuid(),
    'DIRECTOR',
    'Директор',
    'Управление магазином'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'DIRECTOR'
);

-- 1) Департамент "Торговый зал" (если нет)
INSERT INTO departments (id, name)
SELECT
    gen_random_uuid(),
    'Торговый зал'
WHERE NOT EXISTS (
    SELECT 1 FROM departments WHERE name = 'Торговый зал'
);

-- 2) Позиция "Директор магазина" (если нет)
INSERT INTO positions (id, name)
SELECT
    gen_random_uuid(),
    'Директор магазина'
WHERE NOT EXISTS (
    SELECT 1 FROM positions WHERE name = 'Директор магазина'
);

-- 3) Employee директора (если нет)
INSERT INTO employees (
    full_name,
    passport_data,
    birth_date,
    position_id,
    department_id,
    storage_location_id,
    employment_date,
    employment_status,
    work_phone,
    personal_phone,
    email,
    created_at,
    updated_at
)
SELECT
    'Смирнова Ольга Директор',
    '{
      "type": "internal_rf",
      "series": "8888",
      "number": "333444",
      "registrationAddress": "г. Краснодар",
      "issueDate": "2010-02-02"
    }'::jsonb,
    '1985-05-05',
    p.id,
    d.id,
    sl.id,
    '2020-05-01',
    'active',
    '+7-900-500-00-01',
    '+7-900-500-10-01',
    'director1@example.com',
    now(),
    now()
FROM positions p
JOIN departments d ON d.name = 'Торговый зал'
JOIN storage_locations sl ON sl.id = '11111111-1111-1111-1111-111111111111'
WHERE p.name = 'Директор магазина'
  AND NOT EXISTS (
      SELECT 1 FROM employees e WHERE e.email = 'director1@example.com'
  );

-- 4) UserAccount директора (если нет)
INSERT INTO user_accounts (
    employee_id,
    login,
    password_hash,
    role_id,
    is_active
)
SELECT
    e.id,
    'director1',
    '$2b$10$0EuZmP13zlFOTHUWAPReFeyH8Lz/ACNTyVkX0R2dQTthIszZl6S.y', -- Admin123!
    r.id,
    TRUE
FROM employees e
JOIN roles r ON r.code = 'DIRECTOR'
WHERE e.email = 'director1@example.com'
  AND NOT EXISTS (
      SELECT 1 FROM user_accounts ua WHERE ua.login = 'director1'
  );