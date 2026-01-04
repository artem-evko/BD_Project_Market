-- =========================
-- Безопасные добавления FK
-- (проверяем, что constraint ещё не существует)
-- =========================

DO $$
BEGIN
    -- price_restrictions.category_id -> product_categories.id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_price_restrictions_category'
    ) THEN
        ALTER TABLE price_restrictions
            ADD CONSTRAINT fk_price_restrictions_category
            FOREIGN KEY (category_id) REFERENCES product_categories(id);
    END IF;

    -- stop_list.task_id -> price_change_tasks.id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_stop_list_task'
    ) THEN
        ALTER TABLE stop_list
            ADD CONSTRAINT fk_stop_list_task
            FOREIGN KEY (task_id) REFERENCES price_change_tasks(id);
    END IF;

    -- employee_worktime.notification_id -> notifications.id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_employee_worktime_notification'
    ) THEN
        ALTER TABLE employee_worktime
            ADD CONSTRAINT fk_employee_worktime_notification
            FOREIGN KEY (notification_id) REFERENCES notifications(id);
    END IF;

    -- store_prices.storage_location_id -> storage_locations.id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_store_prices_location'
    ) THEN
        ALTER TABLE store_prices
            ADD CONSTRAINT fk_store_prices_location
            FOREIGN KEY (storage_location_id) REFERENCES storage_locations(id);
    END IF;

    -- store_prices.product_id -> product.id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_store_prices_product'
    ) THEN
        ALTER TABLE store_prices
            ADD CONSTRAINT fk_store_prices_product
            FOREIGN KEY (product_id) REFERENCES product(id);
    END IF;

    -- store_prices.source_price_list_item_id -> price_list_items.id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_store_prices_price_list_item'
    ) THEN
        ALTER TABLE store_prices
            ADD CONSTRAINT fk_store_prices_price_list_item
            FOREIGN KEY (source_price_list_item_id) REFERENCES price_list_items(id);
    END IF;

    -- store_prices.task_id -> price_change_tasks.id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_store_prices_task'
    ) THEN
        ALTER TABLE store_prices
            ADD CONSTRAINT fk_store_prices_task
            FOREIGN KEY (task_id) REFERENCES price_change_tasks(id);
    END IF;

    -- write_offs.batch_id -> batches.id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_write_offs_batch'
    ) THEN
        ALTER TABLE write_offs
            ADD CONSTRAINT fk_write_offs_batch
            FOREIGN KEY (batch_id) REFERENCES batches(id);
    END IF;

    -- write_offs.storage_zone_id -> storage_zones.id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_write_offs_storage_zone'
    ) THEN
        ALTER TABLE write_offs
            ADD CONSTRAINT fk_write_offs_storage_zone
            FOREIGN KEY (storage_zone_id) REFERENCES storage_zones(id);
    END IF;

    -- discrepancy_requests.supply_invoice_id -> supply_invoices.id
    -- (в V1.2 мог быть без FK, фиксируем)
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_discrepancy_requests_invoice'
    ) THEN
        ALTER TABLE discrepancy_requests
            ADD CONSTRAINT fk_discrepancy_requests_invoice
            FOREIGN KEY (supply_invoice_id) REFERENCES supply_invoices(id);
    END IF;

END $$;
