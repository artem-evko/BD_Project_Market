-- выбери любую существующую ТТ
WITH sl AS (
  SELECT id FROM storage_locations
  WHERE name = 'Магазин №1 Краснодар'
  LIMIT 1
)
UPDATE employees
SET storage_location_id = (SELECT id FROM sl)
WHERE full_name IN (
  'Администратор Системы',
  'Петров Пётр Петрович',
  'Иванова Анна Сергеевна',
  'Сидоров Сергей Николаевич'
);
