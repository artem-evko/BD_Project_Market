-- V3_7__seed_storageloc&&updEmployee.sql

INSERT INTO storage_locations (id, name, type, address)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Магазин №1 (тестовая ТТ)',
    'STORE',
    'г. Тестоград, ул. Тестовая, д. 1'
)
ON CONFLICT (id) DO NOTHING;

UPDATE employees
SET storage_location_id = '11111111-1111-1111-1111-111111111111'
WHERE id = '22222222-2222-2222-2222-222222222222';