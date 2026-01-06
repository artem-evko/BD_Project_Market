-- V3_1__seed_admin_user.sql
-- Создаём сотрудника-админа и учётку admin

-- 1. Сотрудник-админ
INSERT INTO employees (
    id,
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
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    'Администратор Системы',
    '{
       "type": "internal_rf",
       "series": "0000",
       "number": "000000",
       "registration_address": "Адрес не указан",
       "issue_date": "2020-01-01"
    }'::jsonb,
    '1990-01-01',
    (SELECT id FROM positions WHERE name = 'Директор' LIMIT 1),
    (SELECT id FROM departments WHERE name = 'Администрация' LIMIT 1),
    CURRENT_DATE,
    'active',
    '+7-000-000-00-00',
    '+7-000-000-00-00',
    'admin@example.com',
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- 2. Учётка admin
-- пароль: Admin123!
INSERT INTO user_accounts (
    id,
    employee_id,
    login,
    password_hash,
    role_id,
    is_active
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222222',
    'admin',
    '$2b$10$0EuZmP13zlFOTHUWAPReFeyH8Lz/ACNTyVkX0R2dQTthIszZl6S.y', -- BCrypt(Admin123!)
    (SELECT id FROM roles WHERE code = 'ADMIN' LIMIT 1),
    TRUE
)
ON CONFLICT (id) DO NOTHING;
