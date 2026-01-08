-- V3_6__seed_batches&&locations&&operations.sql

-- Зона 1: Холодильник №1 (для йогуртов)
insert into storage_zones (
    id,
    storage_location_id,
    name,
    zone_type,
    temperature_mode,
    capacity,
    is_active
) values (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7',
    '11111111-1111-1111-1111-111111111111',
    'Холодильник №1',
    'refrigerator',
    'cool',
    100.00,
    true
)
on conflict (id) do nothing;

-- Зона 2: Витрина напитков
insert into storage_zones (
    id,
    storage_location_id,
    name,
    zone_type,
    temperature_mode,
    capacity,
    is_active
) values (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8',
    '11111111-1111-1111-1111-111111111111',
    'Витрина напитков',
    'display',
    'normal',
    200.00,
    true
)
on conflict (id) do nothing;

-- Партия Coca-Cola
insert into batches (
    id,
    product_id,
    supply_invoice_id,
    manufacture_date,
    expiration_date,
    purchase_price,
    initial_quantity,
    created_at
) values (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    '2855c128-3058-47fa-afc7-594189d08cd4', -- Coca-Cola
    null,
    '2025-01-01',
    '2026-01-01',
    80.00,
    100.00,
    now()
)
on conflict (id) do nothing;

-- Партия Jägermeister
insert into batches (
    id,
    product_id,
    supply_invoice_id,
    manufacture_date,
    expiration_date,
    purchase_price,
    initial_quantity,
    created_at
) values (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
    '25c55fe0-dd9e-45de-9817-6ddfaf8bf18b', -- Jägermeister
    null,
    '2024-06-01',
    '2030-06-01',
    3000.00,
    50.00,
    now()
)
on conflict (id) do nothing;

-- Партия Danone йогурт
insert into batches (
    id,
    product_id,
    supply_invoice_id,
    manufacture_date,
    expiration_date,
    purchase_price,
    initial_quantity,
    created_at
) values (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
    '9e1c64f3-064d-4177-8bee-df77da6d5d72', -- Danone
    null,
    '2025-01-01',
    '2025-02-15',
    50.00,
    40.00,
    now()
)
on conflict (id) do nothing;

-- Coca-Cola на витрине
insert into batch_locations (
    id,
    batch_id,
    storage_zone_id,
    quantity,
    updated_at
) values (
    'cccccccc-cccc-cccc-cccc-ccccccccccc1',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', -- партия колы
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', -- витрина напитков
    60.00,
    now()
)
on conflict (id) do nothing;

-- та же партия Coca-Cola в подсобке (типа остатки отдельно)
insert into batch_locations (
    id,
    batch_id,
    storage_zone_id,
    quantity,
    updated_at
) values (
    'cccccccc-cccc-cccc-cccc-ccccccccccc2',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', -- холодильник, пусть будет так
    40.00,
    now()
)
on conflict (id) do nothing;

-- Jägermeister на витрине
insert into batch_locations (
    id,
    batch_id,
    storage_zone_id,
    quantity,
    updated_at
) values (
    'cccccccc-cccc-cccc-cccc-ccccccccccc3',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', -- партия ягеря
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', -- витрина напитков
    20.00,
    now()
)
on conflict (id) do nothing;

-- Danone йогурт в холодильнике
insert into batch_locations (
    id,
    batch_id,
    storage_zone_id,
    quantity,
    updated_at
) values (
    'cccccccc-cccc-cccc-cccc-ccccccccccc4',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', -- партия йогурта
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', -- холодильник
    35.00,
    now()
)
on conflict (id) do nothing;

-- Приёмка Coca-Cola на витрину
insert into warehouse_operations (
    id,
    type,
    product_id,
    from_zone_id,
    to_zone_id,
    quantity,
    operation_date,
    employee_id,
    reason,
    batch_id,
    source_document_type,
    source_document_id
) values (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
    'RECEIPT',
    '2855c128-3058-47fa-afc7-594189d08cd4', -- Coca-Cola
    null,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
    60.00,
    now() - interval '5 days',
    '22222222-2222-2222-2222-222222222222',
    'Приёмка поставки',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    'SUPPLY_INVOICE',
    null
)
on conflict (id) do nothing;

-- Перемещение части йогурта внутри магазина (типа с подсобки в холодильник)
insert into warehouse_operations (
    id,
    type,
    product_id,
    from_zone_id,
    to_zone_id,
    quantity,
    operation_date,
    employee_id,
    reason,
    batch_id,
    source_document_type,
    source_document_id
) values (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2',
    'TRANSFER',
    '9e1c64f3-064d-4177-8bee-df77da6d5d72', -- Danone
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', -- витрина
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', -- холодильник
    10.00,
    now() - interval '2 days',
    '22222222-2222-2222-2222-222222222222', -- тот же сотрудник
    'Перемещение в холодильник',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3',
    'INTERNAL_OPERATION',
    null
)
on conflict (id) do nothing;
