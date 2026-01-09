-- Seed demo data for stop_list (for testing GET /stop-list)
-- Safe / idempotent migration.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    v_sl_id uuid;
    v_product_id uuid;
    v_pli_id uuid;
BEGIN
    -- storage location: prefer known one, else take any
    SELECT id INTO v_sl_id
    FROM storage_locations
    WHERE name = 'Магазин №1 Краснодар'
    LIMIT 1;

    IF v_sl_id IS NULL THEN
        SELECT id INTO v_sl_id
        FROM storage_locations
        ORDER BY id
        LIMIT 1;
    END IF;

    IF v_sl_id IS NULL THEN
        RAISE NOTICE 'Seed stop_list: storage_locations is empty, skip.';
        RETURN;
    END IF;

    -- product: prefer demo barcode, else take any
    SELECT id INTO v_product_id
    FROM product
    WHERE barcode = '4600000000011'
    LIMIT 1;

    IF v_product_id IS NULL THEN
        SELECT id INTO v_product_id
        FROM product
        ORDER BY id
        LIMIT 1;
    END IF;

    IF v_product_id IS NULL THEN
        RAISE NOTICE 'Seed stop_list: product is empty, skip.';
        RETURN;
    END IF;

    -- price_list_item for that product (optional)
    SELECT id INTO v_pli_id
    FROM price_list_items
    WHERE product_id = v_product_id
    ORDER BY id
    LIMIT 1;

    -- insert demo stop_list row if missing
    IF NOT EXISTS (
        SELECT 1
        FROM stop_list
        WHERE product_id = v_product_id
          AND storage_location_id = v_sl_id
    ) THEN
        INSERT INTO stop_list (
            id,
            storage_location_id,
            effective_date,
            task_id,
            product_id,
            price_list_item_id,
            candidate_price,
            prev_price,
            input_price,
            violations,
            reason,
            status,
            sent_to_hq_at,
            hq_response_at,
            hq_comment,
            created_at
        )
        VALUES (
            gen_random_uuid(),
            v_sl_id,
            CURRENT_DATE,
            NULL,
            v_product_id,
            v_pli_id,
            999.99,
            119.99,
            100.00,
            '{"rule":"demo_rule","details":"demo violation"}'::jsonb,
            'seed: demo stop-list violation',
            'pending',
            NULL,
            NULL,
            NULL,
            NOW()
        );
    END IF;
END $$;
