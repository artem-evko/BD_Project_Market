-- 2.9 contract_contacts
CREATE TABLE IF NOT EXISTS contract_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contractor_id UUID NOT NULL REFERENCES contractors(id),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    position VARCHAR(100)
);

-- 2.10 storage_locations
CREATE TABLE IF NOT EXISTS storage_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    address TEXT,
    temperature_mode VARCHAR(50)
);

-- 2.11 trucks
CREATE TABLE IF NOT EXISTS trucks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plate_number VARCHAR(10) NOT NULL,
    model VARCHAR(100),
    capacity_kg INTEGER,
    status VARCHAR(50),
    note TEXT
);

-- 2.12 contracts
CREATE TABLE IF NOT EXISTS contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contractor_id UUID NOT NULL REFERENCES contractors(id),
    storage_location_id UUID NOT NULL REFERENCES storage_locations(id),
    contract_number VARCHAR(50) NOT NULL,
    date_start DATE NOT NULL,
    date_end DATE,
    delivery_type VARCHAR(50),
    truck_id UUID REFERENCES trucks(id),
    employee_id UUID REFERENCES employees(id)
);

-- 2.13 contract_products
CREATE TABLE IF NOT EXISTS contract_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    product_id UUID NOT NULL REFERENCES product(id),
    quantity NUMERIC(10,2),
    purchase_price NUMERIC(10,2)
);

-- 2.44 storage_zones (нужна для warehouse_operations FK)
-- Если storage_zones уже будет в V1.5 — тут можно убрать.
-- Но чтобы V1.2 была самодостаточной для FK, создаём зону заранее.
CREATE TABLE IF NOT EXISTS storage_zones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_location_id UUID NOT NULL REFERENCES storage_locations(id),
    name VARCHAR(100) NOT NULL,
    zone_type VARCHAR(50) NOT NULL,
    temperature_mode VARCHAR(50) NOT NULL,
    capacity NUMERIC(10,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2.41 batches (нужна для warehouse_operations и inventory_items FK)
-- Если batches создаёшь позже отдельным файлом — тут можно убрать и перенести FK.
CREATE TABLE IF NOT EXISTS batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES product(id),
    supply_invoice_id UUID,
    manufacture_date DATE,
    expiration_date DATE,
    purchase_price NUMERIC(10,2),
    initial_quantity NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- 2.14 warehouse_operations
CREATE TABLE IF NOT EXISTS warehouse_operations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(20) NOT NULL,
    product_id UUID NOT NULL REFERENCES product(id),
    from_zone_id UUID REFERENCES storage_zones(id),
    to_zone_id UUID REFERENCES storage_zones(id),
    quantity NUMERIC(10,2) NOT NULL,
    operation_date TIMESTAMP NOT NULL DEFAULT now(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    reason TEXT,
    batch_id UUID REFERENCES batches(id),
    source_document_type VARCHAR(50),
    source_document_id UUID
);

-- 2.15 inventory
CREATE TABLE IF NOT EXISTS inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_date DATE NOT NULL,
    employee_id UUID NOT NULL REFERENCES employees(id),
    storage_location_id UUID NOT NULL REFERENCES storage_locations(id),
    status VARCHAR(100) NOT NULL
);

-- 2.43 discrepancy_requests (нужна для inventory_items FK)
-- Если эту таблицу ты создаёшь позже — можно убрать и добавлять FK позже ALTER TABLE.
CREATE TABLE IF NOT EXISTS discrepancy_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supply_invoice_id UUID,
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

-- 2.16 inventory_items
CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id UUID NOT NULL REFERENCES inventory(id),
    product_id UUID NOT NULL REFERENCES product(id),
    expected_qty NUMERIC(10,2),
    actual_qty NUMERIC(10,2),
    batch_id UUID REFERENCES batches(id),
    discrepancy_request_id UUID REFERENCES discrepancy_requests(id)
);

-- 2.17 price_lists
CREATE TABLE IF NOT EXISTS price_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(20) NOT NULL,
    storage_location_id UUID REFERENCES storage_locations(id),
    effective_date DATE,
    status VARCHAR(20),
    end_date DATE,
    created_by UUID REFERENCES employees(id)
);

-- 2.18 price_list_items
CREATE TABLE IF NOT EXISTS price_list_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    price_list_id UUID NOT NULL REFERENCES price_lists(id),
    product_id UUID NOT NULL REFERENCES product(id),
    input_price NUMERIC(10,2),
    final_price NUMERIC(10,2),
    price_type VARCHAR(20),
    storage_location_id UUID REFERENCES storage_locations(id),
    limit_markup_percent NUMERIC(10,2)
);
