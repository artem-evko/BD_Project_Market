-- 2.37 product_categories
CREATE TABLE IF NOT EXISTS product_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    parent_id UUID REFERENCES product_categories(id)
);

-- 2.38 product_category_links
CREATE TABLE IF NOT EXISTS product_category_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id),
    category_id UUID NOT NULL REFERENCES product_categories(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_product_category_links ON product_category_links(product_id, category_id);

-- 2.39 shifts
CREATE TABLE IF NOT EXISTS shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME,
    status VARCHAR(50) NOT NULL,
    responsible_id UUID REFERENCES employees(id)
);

CREATE INDEX IF NOT EXISTS ix_shifts_employee_date ON shifts(employee_id, date);

-- 2.40 shift_operations
-- reference_table/reference_id - полиморфная ссылка
CREATE TABLE IF NOT EXISTS shift_operations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_id UUID NOT NULL REFERENCES shifts(id) ON DELETE CASCADE,
    operation_type VARCHAR(50) NOT NULL,
    reference_id UUID,
    reference_table VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_shift_operations_shift_time ON shift_operations(shift_id, timestamp);

-- 2.44 storage_zones (если уже создали раньше - IF NOT EXISTS не упадет)
CREATE TABLE IF NOT EXISTS storage_zones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_location_id UUID NOT NULL REFERENCES storage_locations(id),
    name VARCHAR(100) NOT NULL,
    zone_type VARCHAR(50) NOT NULL,
    temperature_mode VARCHAR(50) NOT NULL,
    capacity NUMERIC(10,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS ix_storage_zones_location ON storage_zones(storage_location_id);

-- 2.42 supply_invoices
CREATE TABLE IF NOT EXISTS supply_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    storage_location_id UUID NOT NULL REFERENCES storage_locations(id),
    invoice_number VARCHAR(100) NOT NULL,
    expected_date DATE,
    actual_date DATE,
    status VARCHAR(50) NOT NULL,
    received_zone_id UUID REFERENCES storage_zones(id),
    storekeeper_id UUID REFERENCES employees(id),
    merchandiser_id UUID REFERENCES employees(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_supply_invoices_number ON supply_invoices(invoice_number);

-- 2.41 batches (если уже создавали раньше - IF NOT EXISTS ок)
CREATE TABLE IF NOT EXISTS batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id),
    supply_invoice_id UUID REFERENCES supply_invoices(id),
    manufacture_date DATE,
    expiration_date DATE,
    purchase_price NUMERIC(10,2),
    initial_quantity NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_batches_product ON batches(product_id);

-- 2.43 discrepancy_requests (если уже создавали раньше - IF NOT EXISTS ок)
CREATE TABLE IF NOT EXISTS discrepancy_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supply_invoice_id UUID REFERENCES supply_invoices(id),
    supply_invoice_item_id UUID,
    product_id UUID REFERENCES product(id),
    batch_id UUID REFERENCES batches(id),
    discrepancy_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    quantity_expected NUMERIC(10,2),
    quantity_actual NUMERIC(10,2),
    status VARCHAR(50) NOT NULL,
    created_by_employee_id UUID REFERENCES employees(id),
    decision_by_employee_id UUID REFERENCES employees(id),
    decision_at TIMESTAMP,
    decision_comment TEXT,
    hq_decision_at TIMESTAMP,
    hq_decision_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- 2.42.1 supply_invoice_items
CREATE TABLE IF NOT EXISTS supply_invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supply_invoice_id UUID NOT NULL REFERENCES supply_invoices(id) ON DELETE CASCADE,
    line_no INTEGER NOT NULL,
    product_id UUID NOT NULL REFERENCES product(id),

    quantity_expected NUMERIC(10,2) NOT NULL,
    purchase_price NUMERIC(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    quantity_actual NUMERIC(10,2),
    manufacture_date DATE,
    expiration_date DATE,
    line_status VARCHAR(30) NOT NULL DEFAULT 'pending',

    discrepancy_request_id UUID REFERENCES discrepancy_requests(id),
    batch_id UUID REFERENCES batches(id),
    fact_entered_by UUID REFERENCES employees(id),
    fact_entered_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_supply_invoice_items_invoice_line ON supply_invoice_items(supply_invoice_id, line_no);

-- теперь можем зафиксировать FK supply_invoice_item_id в discrepancy_requests
ALTER TABLE discrepancy_requests
    ADD CONSTRAINT fk_discrepancy_requests_item
    FOREIGN KEY (supply_invoice_item_id) REFERENCES supply_invoice_items(id);

-- 2.45 price_change_tasks
CREATE TABLE IF NOT EXISTS price_change_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_date DATE NOT NULL,
    created_by_employee_id UUID NOT NULL REFERENCES employees(id),
    storage_location_id UUID REFERENCES storage_locations(id),
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    total_products INTEGER,
    prices_updated INTEGER,
    prices_restricted INTEGER
);

CREATE INDEX IF NOT EXISTS ix_price_change_tasks_date ON price_change_tasks(created_date);

-- 2.49 coupons
CREATE TABLE IF NOT EXISTS coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    discount_percent NUMERIC(5,2),
    discount_amount NUMERIC(10,2),
    valid_from DATE,
    valid_until DATE,
    min_purchase NUMERIC(10,2),
    product_id UUID REFERENCES product(id),
    batch_id UUID REFERENCES batches(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- 2.45.1 price_change_task_items
CREATE TABLE IF NOT EXISTS price_change_task_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES price_change_tasks(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES product(id),
    selected_price_list_item_id UUID REFERENCES price_list_items(id),
    selected_final_price NUMERIC(10,2),
    regular_price NUMERIC(10,2),
    label_type VARCHAR(20),
    restriction_applied BOOLEAN,
    restriction_reason VARCHAR(50),
    stop_list_id UUID REFERENCES stop_list(id),
    coupon_id UUID REFERENCES coupons(id),
    batch_id UUID REFERENCES batches(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_price_change_task_items_task ON price_change_task_items(task_id);

-- 2.46 batch_locations
CREATE TABLE IF NOT EXISTS batch_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    storage_zone_id UUID NOT NULL REFERENCES storage_zones(id),
    quantity NUMERIC(10,2) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_batch_locations_batch_zone ON batch_locations(batch_id, storage_zone_id);

-- 2.47 sales_receipts
CREATE TABLE IF NOT EXISTS sales_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_number VARCHAR(50) NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT now(),
    employee_id UUID REFERENCES employees(id),
    storage_location_id UUID REFERENCES storage_locations(id),
    total_amount NUMERIC(12,2),
    payment_method VARCHAR(20),
    status VARCHAR(20),
    customer_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_sales_receipts_number ON sales_receipts(receipt_number);

-- 2.48 sales_items
CREATE TABLE IF NOT EXISTS sales_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL REFERENCES sales_receipts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES product(id),
    batch_id UUID REFERENCES batches(id),
    quantity NUMERIC(10,2) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    total_price NUMERIC(10,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_sales_items_receipt ON sales_items(receipt_id);

-- 2.50 coupon_instances
CREATE TABLE IF NOT EXISTS coupon_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id UUID NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    instance_code VARCHAR(50) NOT NULL UNIQUE,
    printed_date TIMESTAMP,
    printed_by UUID REFERENCES employees(id),
    redeemed_at TIMESTAMP,
    redeemed_receipt_id UUID REFERENCES sales_receipts(id)
);

-- 2.51 label_print_queue
CREATE TABLE IF NOT EXISTS label_print_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id),
    price_list_item_id UUID REFERENCES price_list_items(id),
    label_type VARCHAR(20),
    print_status VARCHAR(20),
    print_date TIMESTAMP,
    printed_by UUID REFERENCES employees(id)
);
