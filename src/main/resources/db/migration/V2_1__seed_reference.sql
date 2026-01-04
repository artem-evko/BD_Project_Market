-- V2.1 seed: справочники и базовые сущности
-- Вставляем "родителей" для FK

-- 1) roles
INSERT INTO roles (code, name, description) VALUES
 ('ADMIN',    'Администратор',        'Полный доступ'),
 ('DIRECTOR', 'Директор магазина',    'Управление магазином'),
 ('MANAGER',  'Менеджер',             'Работа с договорами/поставками'),
 ('CASHIER',  'Кассир',               'Продажи на кассе')
ON CONFLICT DO NOTHING;

-- 2) positions
INSERT INTO positions (name, weekly_hours_norm, description) VALUES
 ('Директор', 40, 'Руководитель магазина'),
 ('Менеджер', 40, 'Операционная работа'),
 ('Кладовщик', 40, 'Склад и перемещения'),
 ('Кассир',    40, 'Продажи')
ON CONFLICT DO NOTHING;

-- 3) departments
INSERT INTO departments (name, description) VALUES
 ('Администрация', 'Управление и контроль'),
 ('Торговый зал',  'Продажи и обслуживание клиентов'),
 ('Склад',         'Приёмка, хранение, перемещения')
ON CONFLICT DO NOTHING;

-- 4) manufacturer
INSERT INTO manufacturer (code, manufacturer_name, manufacturer_country) VALUES
 ('M_JAGER', 'Jägermeister', 'Германия'),
 ('M_COCA',  'Coca-Cola',    'США'),
 ('M_DAN',   'Danone',       'Франция')
ON CONFLICT DO NOTHING;

-- 5) storage_locations
INSERT INTO storage_locations (name, type, address, temperature_mode) VALUES
 ('Магазин №1 Краснодар', 'STORE',     'г. Краснодар, ул. Красная, 1', 'ROOM'),
 ('Склад центральный',    'WAREHOUSE', 'г. Краснодар, ул. Складская, 10', 'MIXED')
ON CONFLICT DO NOTHING;

-- 6) trucks
INSERT INTO trucks (plate_number, model, capacity_kg, status, note) VALUES
 ('A001AA23', 'ГАЗель Next', 1500, 'ACTIVE', 'Основная машина'),
 ('B777BB23', 'Hyundai HD78', 3000, 'ACTIVE', 'Резерв')
ON CONFLICT DO NOTHING;

-- 7) contractors
INSERT INTO contractors (name, inn, kpp, address, phone, email) VALUES
 ('ООО "Поставщик Напитков"', '2300000001', '230001001', 'г. Краснодар, ул. Поставщиков, 5', '+7-861-000-00-01', 'supply1@example.ru'),
 ('ООО "Молочный Мир"',       '2300000002', '230002001', 'г. Краснодар, ул. Молочная, 7',    '+7-861-000-00-02', 'milk@example.ru')
ON CONFLICT DO NOTHING;

-- 8) storage_zones (создаём зоны для каждого storage_location)
INSERT INTO storage_zones (storage_location_id, name, zone_type, temperature_mode, capacity, is_active)
SELECT sl.id, 'Торговый зал', 'SALE', 'ROOM', 1000, true
FROM storage_locations sl
WHERE sl.name = 'Магазин №1 Краснодар'
ON CONFLICT DO NOTHING;

INSERT INTO storage_zones (storage_location_id, name, zone_type, temperature_mode, capacity, is_active)
SELECT sl.id, 'Складская зона', 'STORAGE', 'MIXED', 5000, true
FROM storage_locations sl
WHERE sl.name = 'Склад центральный'
ON CONFLICT DO NOTHING;
