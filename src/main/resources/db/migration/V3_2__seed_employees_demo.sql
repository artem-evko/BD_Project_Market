-- V3_2__seed_employees_demo.sql
-- Демонстрационные сотрудники для тестирования фильтрации

-- =====================================================
-- 1. Петров Пётр Петрович (Менеджер, активный)
-- =====================================================
INSERT INTO employees (
    full_name,
    passport_data,
    birth_date,
    position_id,
    department_id,
    employment_date,
    employment_status,
    work_phone,
    personal_phone,
    email,
    created_at,
    updated_at
)
SELECT
    'Петров Пётр Петрович',
    '{
      "type": "internal_rf",
      "series": "1234",
      "number": "567890",
      "registrationAddress": "г. Краснодар",
      "issueDate": "2012-04-10"
    }'::jsonb,
    '1992-05-12',
    p.id,
    d.id,
    '2022-03-01',
    'active',
    '+7-900-111-11-11',
    '+7-900-111-22-22',
    'petrov@example.com',
    NOW(),
    NOW()
FROM positions p, departments d
WHERE p.name = 'Менеджер'
  AND d.name = 'Торговый зал'
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
    'manager1',
    '$2b$10$0EuZmP13zlFOTHUWAPReFeyH8Lz/ACNTyVkX0R2dQTthIszZl6S.y', -- Admin123!
    r.id,
    TRUE
FROM employees e, roles r
WHERE e.full_name = 'Петров Пётр Петрович'
  AND r.code = 'MANAGER'
ON CONFLICT (login) DO NOTHING;


-- =====================================================
-- 2. Иванова Анна Сергеевна (Кассир, активная)
-- =====================================================
INSERT INTO employees (
    full_name,
    passport_data,
    birth_date,
    position_id,
    department_id,
    employment_date,
    employment_status,
    work_phone,
    personal_phone,
    email,
    created_at,
    updated_at
)
SELECT
    'Иванова Анна Сергеевна',
    '{
      "type": "internal_rf",
      "series": "4321",
      "number": "098765",
      "registrationAddress": "г. Краснодар",
      "issueDate": "2016-09-01"
    }'::jsonb,
    '1998-11-20',
    p.id,
    d.id,
    '2023-06-10',
    'active',
    '+7-900-222-11-11',
    '+7-900-222-22-22',
    'ivanova@example.com',
    NOW(),
    NOW()
FROM positions p, departments d
WHERE p.name = 'Кассир'
  AND d.name = 'Торговый зал'
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
    'cashier1',
    '$2b$10$0EuZmP13zlFOTHUWAPReFeyH8Lz/ACNTyVkX0R2dQTthIszZl6S.y',
    r.id,
    TRUE
FROM employees e, roles r
WHERE e.full_name = 'Иванова Анна Сергеевна'
  AND r.code = 'CASHIER'
ON CONFLICT (login) DO NOTHING;


-- =====================================================
-- 3. Сидоров Сергей Николаевич (Кладовщик, уволен)
-- =====================================================
INSERT INTO employees (
    full_name,
    passport_data,
    birth_date,
    position_id,
    department_id,
    employment_date,
    termination_date,
    employment_status,
    work_phone,
    personal_phone,
    email,
    created_at,
    updated_at
)
SELECT
    'Сидоров Сергей Николаевич',
    '{
      "type": "internal_rf",
      "series": "5555",
      "number": "111222",
      "registrationAddress": "г. Краснодар",
      "issueDate": "2008-01-15"
    }'::jsonb,
    '1987-02-03',
    p.id,
    d.id,
    '2020-01-15',
    '2024-09-01',
    'terminated',
    '+7-900-333-11-11',
    '+7-900-333-22-22',
    'sidorov@example.com',
    NOW(),
    NOW()
FROM positions p, departments d
WHERE p.name = 'Кладовщик'
  AND d.name = 'Склад'
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
    'storekeeper1',
    '$2b$10$0EuZmP13zlFOTHUWAPReFeyH8Lz/ACNTyVkX0R2dQTthIszZl6S.y',
    r.id,
    FALSE
FROM employees e, roles r
WHERE e.full_name = 'Сидоров Сергей Николаевич'
  AND r.code = 'MANAGER'
ON CONFLICT (login) DO NOTHING;
