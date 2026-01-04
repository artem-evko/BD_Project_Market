-- 2.27 price_history
CREATE TABLE IF NOT EXISTS price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id),
    old_price NUMERIC(10,2) NOT NULL,
    new_price NUMERIC(10,2) NOT NULL,
    changed_by UUID NOT NULL REFERENCES employees(id),
    change_date TIMESTAMP NOT NULL DEFAULT now(),
    comment TEXT
);

CREATE INDEX IF NOT EXISTS ix_price_history_product_date ON price_history(product_id, change_date);

-- 2.27.1 store_prices
-- FK на storage_locations/product/price_list_items/task добавим позже (V1.6), чтобы не ломать порядок.
CREATE TABLE IF NOT EXISTS store_prices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_location_id UUID NOT NULL,
    product_id UUID NOT NULL,
    effective_date DATE NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    regular_price NUMERIC(10,2),
    price_type_applied VARCHAR(20),
    source_price_list_item_id UUID,
    task_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_store_prices_loc_prod_date ON store_prices(storage_location_id, product_id, effective_date);

-- 2.28 returns
CREATE TABLE IF NOT EXISTS returns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(20) NOT NULL, -- customer/supplier
    product_id UUID NOT NULL REFERENCES product(id),
    quantity NUMERIC(10,2) NOT NULL,
    date_returned DATE NOT NULL,
    reason TEXT,
    employee_id UUID REFERENCES employees(id),
    contractor_id UUID REFERENCES contractors(id)
);

-- 2.29 write_offs
-- batch_id и storage_zone_id FK добавим позже (V1.6), так как batches/storage_zones будут в V1.5
CREATE TABLE IF NOT EXISTS write_offs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id),
    quantity NUMERIC(10,2) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    date_written_off DATE NOT NULL,
    employee_id UUID NOT NULL REFERENCES employees(id),
    batch_id UUID,
    storage_zone_id UUID,
    document_number VARCHAR(100),
    comment TEXT,
    status VARCHAR(50),
    approved_by_director_id UUID REFERENCES employees(id),
    approved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_write_offs_product_date ON write_offs(product_id, date_written_off);

-- 2.30 orders
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_name VARCHAR(255),
    phone VARCHAR(20),
    order_date TIMESTAMP NOT NULL DEFAULT now(),
    status VARCHAR(50) NOT NULL,
    total_sum NUMERIC(12,2) NOT NULL,
    created_by UUID NOT NULL REFERENCES employees(id)
);

CREATE INDEX IF NOT EXISTS ix_orders_date ON orders(order_date);

-- 2.31 order_items
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES product(id),
    quantity NUMERIC(10,2) NOT NULL,
    price NUMERIC(10,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_order_items_order ON order_items(order_id);

-- 2.32 exchange_log
CREATE TABLE IF NOT EXISTS exchange_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL, -- insert/update/delete
    direction VARCHAR(10) NOT NULL, -- upload/download
    status VARCHAR(20) NOT NULL,    -- success/error
    message TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_exchange_log_time ON exchange_log(timestamp);

-- 2.33 access_logs
-- user_id FK на user_accounts есть уже в core
CREATE TABLE IF NOT EXISTS access_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(100),
    entity_id UUID,
    timestamp TIMESTAMP NOT NULL DEFAULT now(),
    ip_address VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS ix_access_logs_time ON access_logs(timestamp);

-- 2.34 notifications
-- В некоторых местах notification_id появляется раньше (например, employee_worktime), FK добавим в V1.6
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES user_accounts(id),
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    is_read BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_notifications_recipient_read ON notifications(recipient_id, is_read);

-- 2.35 documents
-- created_by -> employees
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_type VARCHAR(50) NOT NULL,
    related_entity VARCHAR(100),
    related_id UUID,
    created_by UUID NOT NULL REFERENCES employees(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    status VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_documents_type ON documents(doc_type);

-- 2.36 document_files
CREATE TABLE IF NOT EXISTS document_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_document_files_document ON document_files(document_id);
