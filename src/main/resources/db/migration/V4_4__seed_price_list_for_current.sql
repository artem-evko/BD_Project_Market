CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    v_product_id uuid;
    v_storage_location_id uuid;
    v_employee_id uuid;
    v_price_list_id uuid;
BEGIN
    -- 1) Берём любой товар
    SELECT id INTO v_product_id
    FROM product
    ORDER BY id
    LIMIT 1;

    IF v_product_id IS NULL THEN
        RAISE EXCEPTION 'Seed current prices: table product is empty. Add at least 1 product first.';
    END IF;

    -- 2) Берём любую локацию
    SELECT id INTO v_storage_location_id
    FROM storage_locations
    ORDER BY id
    LIMIT 1;

    IF v_storage_location_id IS NULL THEN
        RAISE EXCEPTION 'Seed current prices: table storage_locations is empty. Add at least 1 storage location first.';
    END IF;

    -- 3) Берём любого сотрудника (создателя прайс-листа)
    SELECT id INTO v_employee_id
    FROM employees
    ORDER BY id
    LIMIT 1;

    IF v_employee_id IS NULL THEN
        RAISE EXCEPTION 'Seed current prices: table employees is empty. Add at least 1 employee first.';
    END IF;

    -- 4) Ищем прайс-лист для этой локации
    SELECT id INTO v_price_list_id
    FROM price_lists
    WHERE storage_location_id = v_storage_location_id
      AND (status IS NULL OR status <> 'archived')
    ORDER BY effective_date DESC NULLS LAST, id
    LIMIT 1;

    -- 5) Если нет — создаём
    IF v_price_list_id IS NULL THEN
        INSERT INTO price_lists (
            id,
            type,
            storage_location_id,
            effective_date,
            status,
            end_date,
            created_by
        )
        VALUES (
            gen_random_uuid(),
            'REGULAR',
            v_storage_location_id,
            CURRENT_DATE,
            'active',
            NULL,
            v_employee_id
        )
        RETURNING id INTO v_price_list_id;
    END IF;

    -- 6) Вставляем item, если нет
    IF NOT EXISTS (
        SELECT 1
        FROM price_list_items
        WHERE product_id = v_product_id
          AND storage_location_id = v_storage_location_id
    ) THEN
        INSERT INTO price_list_items (
            id,
            price_list_id,
            product_id,
            input_price,
            final_price,
            price_type,
            storage_location_id,
            limit_markup_percent
        )
        VALUES (
            gen_random_uuid(),
            v_price_list_id,
            v_product_id,
            100.00,
            119.99,
            'REGULAR',
            v_storage_location_id,
            30.00
        );
    END IF;
END $$;
