--V9_1__seed_batchLocation.sql

insert into batch_locations (
    id,
    batch_id,
    storage_zone_id,
    quantity,
    updated_at
) values (
    'cccccccc-cccc-cccc-cccc-ccccccccccc5',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', -- партия йогурта
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', -- Витрина напитков
    20.00,
    now()
);
