-- V3_3__seed_products.sql
-- Базовые категории, товары и цены для тестирования /api/products*

------------------------------------------------------------
-- 1. Категории товаров
------------------------------------------------------------
INSERT INTO product_categories (name)
SELECT v.name
FROM (
    VALUES
        ('Алкоголь'),
        ('Безалкогольные напитки'),
        ('Молочная продукция')
) AS v(name)
WHERE NOT EXISTS (
    SELECT 1
    FROM product_categories pc
    WHERE pc.name = v.name
);
------------------------------------------------------------
-- 2. Товары (привязка к производителям по manufacturer.code)
------------------------------------------------------------

-- 2.1. Jägermeister 0.7L (алкоголь)
INSERT INTO product (
    name,
    manufacturer_id,
    length,
    width,
    height,
    unit_of_measure,
    shelf_life_days,
    barcode,
    additional_info,
    created_at,
    archived
)
SELECT
    'Jägermeister 0.7L',
    m.id,
    '7.0',       -- длина условно
    '7.0',       -- ширина
    '30.0',      -- высота
    'bottle',
    365,
    '400000000001',
    'Настойка горькая, 0.7 л',
    now(),
    false
FROM manufacturer m
WHERE m.code = 'M_JAGER'
  AND NOT EXISTS (
      SELECT 1 FROM product p WHERE p.barcode = '400000000001'
  );

-- 2.2. Coca-Cola 1L (безалкогольные напитки)
INSERT INTO product (
    name,
    manufacturer_id,
    length,
    width,
    height,
    unit_of_measure,
    shelf_life_days,
    barcode,
    additional_info,
    created_at,
    archived
)
SELECT
    'Coca-Cola 1L',
    m.id,
    '8.0',
    '8.0',
    '32.0',
    'bottle',
    180,
    '400000000002',
    'Газированный напиток, 1 л',
    now(),
    false
FROM manufacturer m
WHERE m.code = 'M_COCA'
  AND NOT EXISTS (
      SELECT 1 FROM product p WHERE p.barcode = '400000000002'
  );

-- 2.3. Danone Йогурт 125г (молочка)
INSERT INTO product (
    name,
    manufacturer_id,
    length,
    width,
    height,
    unit_of_measure,
    shelf_life_days,
    barcode,
    additional_info,
    created_at,
    archived
)
SELECT
    'Danone Йогурт 125г',
    m.id,
    '5.0',
    '5.0',
    '9.0',
    'piece',
    30,
    '400000000003',
    'Йогурт клубничный, 125 г',
    now(),
    false
FROM manufacturer m
WHERE m.code = 'M_DAN'
  AND NOT EXISTS (
      SELECT 1 FROM product p WHERE p.barcode = '400000000003'
  );

-- 2.4. Архивный товар для проверки includeArchived
INSERT INTO product (
    name,
    manufacturer_id,
    length,
    width,
    height,
    unit_of_measure,
    shelf_life_days,
    barcode,
    additional_info,
    created_at,
    archived
)
SELECT
    'Старый товар (архивный пример)',
    m.id,
    '10.0',
    '10.0',
    '10.0',
    'piece',
    10,
    '400000000004',
    'Тестовый архивный товар',
    now(),
    true
FROM manufacturer m
WHERE m.code = 'M_COCA'
  AND NOT EXISTS (
      SELECT 1 FROM product p WHERE p.barcode = '400000000004'
  );

------------------------------------------------------------
-- 3. Привязка товаров к категориям (product_category_links)
------------------------------------------------------------

-- Jägermeister → Алкоголь
INSERT INTO product_category_links (product_id, category_id)
SELECT p.id, c.id
FROM product p
JOIN manufacturer m ON p.manufacturer_id = m.id
JOIN product_categories c ON c.name = 'Алкоголь'
WHERE m.code = 'M_JAGER'
  AND p.barcode = '400000000001'
  AND NOT EXISTS (
      SELECT 1 FROM product_category_links l
      WHERE l.product_id = p.id AND l.category_id = c.id
  );

-- Coca-Cola 1L → Безалкогольные напитки
INSERT INTO product_category_links (product_id, category_id)
SELECT p.id, c.id
FROM product p
JOIN manufacturer m ON p.manufacturer_id = m.id
JOIN product_categories c ON c.name = 'Безалкогольные напитки'
WHERE m.code = 'M_COCA'
  AND p.barcode = '400000000002'
  AND NOT EXISTS (
      SELECT 1 FROM product_category_links l
      WHERE l.product_id = p.id AND l.category_id = c.id
  );

-- Danone Йогурт → Молочная продукция
INSERT INTO product_category_links (product_id, category_id)
SELECT p.id, c.id
FROM product p
JOIN manufacturer m ON p.manufacturer_id = m.id
JOIN product_categories c ON c.name = 'Молочная продукция'
WHERE m.code = 'M_DAN'
  AND p.barcode = '400000000003'
  AND NOT EXISTS (
      SELECT 1 FROM product_category_links l
      WHERE l.product_id = p.id AND l.category_id = c.id
  );

-- Архивный товар тоже куда-нибудь положим, например в "Безалкогольные напитки"
INSERT INTO product_category_links (product_id, category_id)
SELECT p.id, c.id
FROM product p
JOIN manufacturer m ON p.manufacturer_id = m.id
JOIN product_categories c ON c.name = 'Безалкогольные напитки'
WHERE m.code = 'M_COCA'
  AND p.barcode = '400000000004'
  AND NOT EXISTS (
      SELECT 1 FROM product_category_links l
      WHERE l.product_id = p.id AND l.category_id = c.id
  );

------------------------------------------------------------
-- 4. Цены (store_prices) по "Магазин №1 Краснодар" на сегодня
------------------------------------------------------------

-- Jägermeister 0.7L
INSERT INTO store_prices (
    storage_location_id,
    product_id,
    effective_date,
    price,
    regular_price,
    price_type_applied,
    source_price_list_item_id,
    task_id,
    created_at
)
SELECT
    sl.id,
    p.id,
    CURRENT_DATE,
    1499.00,
    1499.00,
    'regular',
    NULL,
    NULL,
    now()
FROM storage_locations sl
JOIN product p ON p.barcode = '400000000001'
WHERE sl.name = 'Магазин №1 Краснодар'
  AND NOT EXISTS (
      SELECT 1 FROM store_prices sp
      WHERE sp.storage_location_id = sl.id
        AND sp.product_id = p.id
        AND sp.effective_date = CURRENT_DATE
  );

-- Coca-Cola 1L
INSERT INTO store_prices (
    storage_location_id,
    product_id,
    effective_date,
    price,
    regular_price,
    price_type_applied,
    source_price_list_item_id,
    task_id,
    created_at
)
SELECT
    sl.id,
    p.id,
    CURRENT_DATE,
    119.90,
    119.90,
    'regular',
    NULL,
    NULL,
    now()
FROM storage_locations sl
JOIN product p ON p.barcode = '400000000002'
WHERE sl.name = 'Магазин №1 Краснодар'
  AND NOT EXISTS (
      SELECT 1 FROM store_prices sp
      WHERE sp.storage_location_id = sl.id
        AND sp.product_id = p.id
        AND sp.effective_date = CURRENT_DATE
  );

-- Danone Йогурт 125г
INSERT INTO store_prices (
    storage_location_id,
    product_id,
    effective_date,
    price,
    regular_price,
    price_type_applied,
    source_price_list_item_id,
    task_id,
    created_at
)
SELECT
    sl.id,
    p.id,
    CURRENT_DATE,
    59.90,
    59.90,
    'regular',
    NULL,
    NULL,
    now()
FROM storage_locations sl
JOIN product p ON p.barcode = '400000000003'
WHERE sl.name = 'Магазин №1 Краснодар'
  AND NOT EXISTS (
      SELECT 1 FROM store_prices sp
      WHERE sp.storage_location_id = sl.id
        AND sp.product_id = p.id
        AND sp.effective_date = CURRENT_DATE
  );
