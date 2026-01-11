-- Seed для проверки эндпоинтов /api/price-restrictions
-- ВАЖНО: один SQL statement, чтобы CTE tt/p_jager/c_alc были видимы всем вставкам

WITH
tt AS (
    SELECT id
    FROM storage_locations
    WHERE id = '11111111-1111-1111-1111-111111111111'
    LIMIT 1
),
p_jager AS (
    SELECT id
    FROM product
    WHERE barcode = '400000000001'
    LIMIT 1
),
c_alc AS (
    SELECT id
    FROM product_categories
    WHERE name = 'Алкоголь'
    LIMIT 1
),

-- 1) GLOBAL ALL
ins_all AS (
    INSERT INTO price_restrictions(
        id, priority, scope_type, storage_location_id, product_id, category_id,
        max_daily_change_percent, max_markup_percent,
        allow_daily_change_exception_for_auto_markdown,
        min_price, max_price, created_at
    )
    SELECT
        gen_random_uuid(),
        10,
        'all',
        NULL,
        NULL,
        NULL,
        10.00,
        25.00,
        TRUE,
        1.00,
        999999.99,
        NOW()
    WHERE NOT EXISTS (
        SELECT 1
        FROM price_restrictions pr
        WHERE pr.scope_type = 'all'
          AND pr.storage_location_id IS NULL
          AND pr.product_id IS NULL
          AND pr.category_id IS NULL
    )
    RETURNING id
),

-- 2) GLOBAL CATEGORY (Алкоголь)
ins_cat_global AS (
    INSERT INTO price_restrictions(
        id, priority, scope_type, storage_location_id, product_id, category_id,
        max_daily_change_percent, max_markup_percent,
        allow_daily_change_exception_for_auto_markdown,
        min_price, max_price, created_at
    )
    SELECT
        gen_random_uuid(),
        20,
        'category',
        NULL,
        NULL,
        c_alc.id,
        8.00,
        20.00,
        TRUE,
        5.00,
        50000.00,
        NOW()
    FROM c_alc
    WHERE NOT EXISTS (
        SELECT 1
        FROM price_restrictions pr
        WHERE pr.scope_type = 'category'
          AND pr.storage_location_id IS NULL
          AND pr.category_id = c_alc.id
    )
    RETURNING id
),

-- 3) PRODUCT rule для test TT (Jäger)
ins_prod_tt AS (
    INSERT INTO price_restrictions(
        id, priority, scope_type, storage_location_id, product_id, category_id,
        max_daily_change_percent, max_markup_percent,
        allow_daily_change_exception_for_auto_markdown,
        min_price, max_price, created_at
    )
    SELECT
        gen_random_uuid(),
        100,
        'product',
        tt.id,
        p_jager.id,
        NULL,
        5.00,
        15.00,
        FALSE,
        50.00,
        2000.00,
        NOW()
    FROM tt, p_jager
    WHERE NOT EXISTS (
        SELECT 1
        FROM price_restrictions pr
        WHERE pr.scope_type = 'product'
          AND pr.storage_location_id = tt.id
          AND pr.product_id = p_jager.id
    )
    RETURNING id
),

-- 4) CATEGORY rule для test TT (Алкоголь)
ins_cat_tt AS (
    INSERT INTO price_restrictions(
        id, priority, scope_type, storage_location_id, product_id, category_id,
        max_daily_change_percent, max_markup_percent,
        allow_daily_change_exception_for_auto_markdown,
        min_price, max_price, created_at
    )
    SELECT
        gen_random_uuid(),
        60,
        'category',
        tt.id,
        NULL,
        c_alc.id,
        6.00,
        18.00,
        TRUE,
        10.00,
        30000.00,
        NOW()
    FROM tt, c_alc
    WHERE NOT EXISTS (
        SELECT 1
        FROM price_restrictions pr
        WHERE pr.scope_type = 'category'
          AND pr.storage_location_id = tt.id
          AND pr.category_id = c_alc.id
    )
    RETURNING id
)

SELECT 1;
