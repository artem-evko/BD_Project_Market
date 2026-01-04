-- 2.20 work_schedule_templates
CREATE TABLE IF NOT EXISTS work_schedule_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    position_id UUID NOT NULL REFERENCES positions(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    weekday INTEGER NOT NULL, -- 1..7
    start_time TIME,
    end_time TIME,
    hours NUMERIC(4,2),
    is_day_off BOOLEAN DEFAULT FALSE
);

-- 2.21 employee_schedule
CREATE TABLE IF NOT EXISTS employee_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    template_id UUID REFERENCES work_schedule_templates(id),
    date DATE NOT NULL,
    planned_start TIME,
    planned_end TIME,
    correction_reason TEXT,
    corrected_by UUID REFERENCES employees(id),
    schedule_type VARCHAR(20)
);

-- 2.22 employee_worktime
CREATE TABLE IF NOT EXISTS employee_worktime (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    date DATE NOT NULL,
    scheduled_start TIME,
    scheduled_end TIME,
    actual_login TIMESTAMP,
    actual_logout TIMESTAMP,
    status VARCHAR(50),
    auto_closed BOOLEAN DEFAULT FALSE,
    auto_close_reason VARCHAR(100),
    confirmation_sent_at TIMESTAMP,
    confirmation_deadline_at TIMESTAMP,
    confirmation_response_at TIMESTAMP,
    confirmation_result VARCHAR(20),
    notification_id UUID
);

-- 2.23.1 work_report_headers
CREATE TABLE IF NOT EXISTS work_report_headers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    week_start DATE NOT NULL,
    storage_location_id UUID NOT NULL REFERENCES storage_locations(id),
    director_id UUID REFERENCES employees(id),
    status VARCHAR(20),
    confirmed_at TIMESTAMP,
    sent_to_hq_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- 2.23.2 work_report_lines
CREATE TABLE IF NOT EXISTS work_report_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES work_report_headers(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    computed_hours NUMERIC(5,2),
    norm_hours NUMERIC(5,2),
    delta_hours NUMERIC(5,2),
    director_override_delta NUMERIC(5,2),
    override_reason TEXT
);

-- 2.24 vacations
CREATE TABLE IF NOT EXISTS vacations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    approved_by_employee_id UUID REFERENCES employees(id),
    approved_at TIMESTAMP,
    reject_comment TEXT,
    date_start DATE,
    date_end DATE,
    status VARCHAR(50)
);

-- 2.24.1 vacation_balance
CREATE TABLE IF NOT EXISTS vacation_balance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    year INTEGER NOT NULL,
    carried_over NUMERIC(5,2),
    accrued NUMERIC(5,2),
    used NUMERIC(5,2)
);

-- 2.25 sick_leaves
CREATE TABLE IF NOT EXISTS sick_leaves (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    approved_by_employee_id UUID REFERENCES employees(id),
    approved_at TIMESTAMP,
    date_start DATE,
    date_end DATE,
    status VARCHAR(50),
    doc_required BOOLEAN DEFAULT FALSE,
    doc_due_date DATE,
    doc_received_at TIMESTAMP,
    doc_status VARCHAR(20),
    reject_comment TEXT
);

-- 2.26 price_restrictions
CREATE TABLE IF NOT EXISTS price_restrictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type VARCHAR(20) NOT NULL, -- all / product / category
    storage_location_id UUID REFERENCES storage_locations(id),
    product_id UUID REFERENCES product(id),
    category_id UUID, -- FK добавим после product_categories (в V1.5)
    max_daily_change_percent NUMERIC(5,2),
    max_markup_percent NUMERIC(5,2),
    allow_daily_change_exception_for_auto_markdown BOOLEAN,
    min_price NUMERIC(10,2),
    max_price NUMERIC(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- 2.19 stop_list
-- Важно: task_id (price_change_tasks) и price_list_item_id (price_list_items) уже будут/есть.
CREATE TABLE IF NOT EXISTS stop_list (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_location_id UUID NOT NULL REFERENCES storage_locations(id),
    effective_date DATE,
    task_id UUID, -- FK добавим после price_change_tasks (V1.6 или V1.5 в зависимости от твоего порядка)
    product_id UUID NOT NULL REFERENCES product(id),
    price_list_item_id UUID REFERENCES price_list_items(id),
    candidate_price NUMERIC(10,2),
    prev_price NUMERIC(10,2),
    input_price NUMERIC(10,2),
    violations JSONB,
    reason TEXT,
    status VARCHAR(20),
    sent_to_hq_at TIMESTAMP,
    hq_response_at TIMESTAMP,
    hq_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Индексы (полезно для ежедневных процессов)
CREATE INDEX IF NOT EXISTS ix_stop_list_effective_date ON stop_list(effective_date);
CREATE INDEX IF NOT EXISTS ix_employee_worktime_employee_date ON employee_worktime(employee_id, date);
CREATE INDEX IF NOT EXISTS ix_work_report_headers_week_start ON work_report_headers(week_start);
