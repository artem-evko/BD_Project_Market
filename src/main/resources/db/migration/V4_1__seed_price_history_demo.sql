-- V4.1 seed: demo price_history to test /api/prices/history/{productId}

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    v_product_id uuid;
    v_employee_id uuid;
BEGIN
    -- берём демо-товар (наш seed barcode)
    SELECT id INTO v_product_id
    FROM product
    WHERE barcode='4600000000011'
    LIMIT 1;

    IF v_product_id IS NULL THEN
        RAISE NOTICE 'Seed price_history: demo product not found. Skipping.';
        RETURN;
    END IF;

    -- берём демо-сотрудника
    SELECT id INTO v_employee_id
    FROM employees
    WHERE full_name='Петров Пётр Петрович'
    LIMIT 1;

    IF v_employee_id IS NULL THEN
        RAISE NOTICE 'Seed price_history: demo employee not found. Skipping.';
        RETURN;
    END IF;

    -- если истории ещё нет для этого товара — добавим 3 записи
    IF NOT EXISTS (SELECT 1 FROM price_history WHERE product_id = v_product_id) THEN
        INSERT INTO price_history (id, product_id, old_price, new_price, changed_by, change_date, comment)
        VALUES
          (gen_random_uuid(), v_product_id, 100.00, 110.00, v_employee_id, now() - interval '10 days', 'seed: initial change'),
          (gen_random_uuid(), v_product_id, 110.00, 120.00, v_employee_id, now() - interval '5 days',  'seed: promo change'),
          (gen_random_uuid(), v_product_id, 120.00, 119.99, v_employee_id, now() - interval '1 day',   'seed: markdown');
    END IF;
END $$;
