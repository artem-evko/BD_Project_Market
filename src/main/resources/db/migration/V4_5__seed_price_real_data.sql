CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    v_storage_location_id uuid;
    v_employee_id uuid;

    v_task_id uuid;

    v_price_list_today uuid;
    v_price_list_yesterday uuid;

    r record;
    idx int := 0;
    v_final numeric(10,2);
    v_input numeric(10,2);

    v_stop_product_id uuid;
    v_stop_prev_price numeric(10,2);
BEGIN
    -- 1) Берём любую локацию (магазин/склад)
    SELECT id INTO v_storage_location_id
    FROM storage_locations
    ORDER BY id
    LIMIT 1;

    IF v_storage_location_id IS NULL THEN
        RAISE EXCEPTION 'Seed price real data: storage_locations is empty';
    END IF;

    -- 2) Берём любого сотрудника (кто менял/создал)
    SELECT id INTO v_employee_id
    FROM employees
    ORDER BY id
    LIMIT 1;

    IF v_employee_id IS NULL THEN
        RAISE EXCEPTION 'Seed price real data: employees is empty';
    END IF;

    -- 3) Создаём/берём прайс-лист на сегодня
    SELECT id INTO v_price_list_today
    FROM price_lists
    WHERE storage_location_id = v_storage_location_id
      AND effective_date = CURRENT_DATE
    ORDER BY id
    LIMIT 1;

    IF v_price_list_today IS NULL THEN
        INSERT INTO price_lists (id, type, storage_location_id, effective_date, status, end_date, created_by)
        VALUES (gen_random_uuid(), 'REGULAR', v_storage_location_id, CURRENT_DATE, 'active', NULL, v_employee_id)
        RETURNING id INTO v_price_list_today;
    END IF;

    -- 4) Создаём/берём прайс-лист на вчера (нужен для сравнений/истории)
    SELECT id INTO v_price_list_yesterday
    FROM price_lists
    WHERE storage_location_id = v_storage_location_id
      AND effective_date = (CURRENT_DATE - INTERVAL '1 day')::date
    ORDER BY id
    LIMIT 1;

    IF v_price_list_yesterday IS NULL THEN
        INSERT INTO price_lists (id, type, storage_location_id, effective_date, status, end_date, created_by)
        VALUES (gen_random_uuid(), 'REGULAR', v_storage_location_id, (CURRENT_DATE - INTERVAL '1 day')::date, 'archived', CURRENT_DATE, v_employee_id)
        RETURNING id INTO v_price_list_yesterday;
    END IF;

    -- 5) Создаём задачу daily-setup (price_change_tasks) на сегодня
    SELECT id INTO v_task_id
    FROM price_change_tasks
    WHERE created_date = CURRENT_DATE
      AND (storage_location_id = v_storage_location_id OR storage_location_id IS NULL)
    ORDER BY id
    LIMIT 1;

    IF v_task_id IS NULL THEN
        INSERT INTO price_change_tasks (
            id, created_date, created_by_employee_id, storage_location_id,
            status, started_at, completed_at, total_products, prices_updated, prices_restricted
        )
        VALUES (
            gen_random_uuid(), CURRENT_DATE, v_employee_id, v_storage_location_id,
            'completed', now(), now(), 0, 0, 0
        )
        RETURNING id INTO v_task_id;
    END IF;

    -- 6) Наполняем price_list_items: берём первые 8 товаров и даём им цены на вчера и сегодня
    FOR r IN
        SELECT id AS product_id
        FROM product
        ORDER BY id
        LIMIT 8
    LOOP
        idx := idx + 1;

        -- базовые цены
        v_input := (80 + idx * 10)::numeric(10,2);
        v_final := (100 + idx * 10)::numeric(10,2);

        -- вчерашняя цена (чуть меньше)
        IF NOT EXISTS (
            SELECT 1 FROM price_list_items
            WHERE price_list_id = v_price_list_yesterday
              AND product_id = r.product_id
              AND storage_location_id = v_storage_location_id
        ) THEN
            INSERT INTO price_list_items (
                id, price_list_id, product_id, input_price, final_price, price_type, storage_location_id, limit_markup_percent
            )
            VALUES (
                gen_random_uuid(), v_price_list_yesterday, r.product_id,
                v_input, (v_final - 5)::numeric(10,2), 'REGULAR', v_storage_location_id, 30.00
            );
        END IF;

        -- сегодняшняя цена
        IF NOT EXISTS (
            SELECT 1 FROM price_list_items
            WHERE price_list_id = v_price_list_today
              AND product_id = r.product_id
              AND storage_location_id = v_storage_location_id
        ) THEN
            INSERT INTO price_list_items (
                id, price_list_id, product_id, input_price, final_price, price_type, storage_location_id, limit_markup_percent
            )
            VALUES (
                gen_random_uuid(), v_price_list_today, r.product_id,
                v_input, v_final, 'REGULAR', v_storage_location_id, 30.00
            );
        END IF;

        -- пару записей в историю цены (price_history) — на первые 3 товара
        IF idx <= 3 THEN
            IF NOT EXISTS (SELECT 1 FROM price_history WHERE product_id = r.product_id) THEN
                INSERT INTO price_history (id, product_id, old_price, new_price, changed_by, change_date, comment)
                VALUES
                  (gen_random_uuid(), r.product_id, (v_final - 10)::numeric(10,2), (v_final - 5)::numeric(10,2), v_employee_id, now() - interval '10 days', 'seed: initial change'),
                  (gen_random_uuid(), r.product_id, (v_final - 5)::numeric(10,2), v_final,                      v_employee_id, now() - interval '5 days',  'seed: price update');
            END IF;
        END IF;

        -- 7) Один товар специально отправляем в stop_list
        IF idx = 1 THEN
            v_stop_product_id := r.product_id;

            SELECT final_price INTO v_stop_prev_price
            FROM price_list_items
            WHERE price_list_id = v_price_list_today
              AND product_id = v_stop_product_id
              AND storage_location_id = v_storage_location_id
            LIMIT 1;

            IF NOT EXISTS (
                SELECT 1 FROM stop_list
                WHERE product_id = v_stop_product_id
                  AND storage_location_id = v_storage_location_id
            ) THEN
                INSERT INTO stop_list (
                    id, storage_location_id, effective_date, task_id,
                    product_id, price_list_item_id,
                    candidate_price, prev_price, input_price,
                    violations, reason, status,
                    sent_to_hq_at, hq_response_at, hq_comment, created_at
                )
                SELECT
                    gen_random_uuid(),
                    v_storage_location_id,
                    CURRENT_DATE,
                    v_task_id,
                    pli.product_id,
                    pli.id,
                    (pli.final_price + 500)::numeric(10,2),
                    pli.final_price,
                    pli.input_price,
                    '{"rule":"max_markup_percent","details":"candidate too high"}',
                    'seed: demo stop-list violation',
                    'pending',
                    NULL, NULL, NULL,
                    now()
                FROM price_list_items pli
                WHERE pli.price_list_id = v_price_list_today
                  AND pli.product_id = v_stop_product_id
                  AND pli.storage_location_id = v_storage_location_id
                LIMIT 1;
            END IF;
        END IF;

    END LOOP;

    -- обновим counters у задачи
    UPDATE price_change_tasks
    SET total_products = 8,
        prices_updated = 0,
        prices_restricted = (SELECT count(*) FROM stop_list WHERE task_id = v_task_id)
    WHERE id = v_task_id;

END $$;
