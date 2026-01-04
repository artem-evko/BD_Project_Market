-- UUID генерация
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- manufacturer (2.2)
CREATE TABLE IF NOT EXISTS manufacturer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    manufacturer_name VARCHAR(255) NOT NULL,
    manufacturer_country VARCHAR(100) NOT NULL
);

-- product (2.1)
CREATE TABLE IF NOT EXISTS product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    manufacturer_id UUID NOT NULL REFERENCES manufacturer(id),
    length VARCHAR(20),
    height VARCHAR(20),
    width VARCHAR(20),
    unit_of_measure VARCHAR(20),
    shelf_life_days INTEGER,
    barcode VARCHAR(20),
    additional_info TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_product_barcode ON product (barcode);

-- positions (2.3)
CREATE TABLE IF NOT EXISTS positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    weekly_hours_norm INTEGER NOT NULL DEFAULT 40,
    description TEXT
);

-- departments (2.4)
CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT
);

-- roles (2.7)
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

-- employees (2.5)
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    passport_data JSONB NOT NULL,
    birth_date DATE,
    position_id UUID REFERENCES positions(id),
    department_id UUID REFERENCES departments(id),
    employment_date DATE,
    termination_date DATE,
    employment_status VARCHAR(100) NOT NULL DEFAULT 'active',
    work_phone VARCHAR(20),
    personal_phone VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- user_accounts (2.6)
CREATE TABLE IF NOT EXISTS user_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID REFERENCES employees(id),
    login VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id UUID REFERENCES roles(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_accounts_login ON user_accounts(login);

-- contractors (2.8)
CREATE TABLE IF NOT EXISTS contractors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    inn VARCHAR(20),
    kpp VARCHAR(20),
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(100)
);
