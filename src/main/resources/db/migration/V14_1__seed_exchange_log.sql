-- Seed: exchange_log test data for UI/API checks

INSERT INTO exchange_log (id, entity_name, entity_id, operation, direction, status, message, "timestamp")
VALUES
  (gen_random_uuid(), 'product',   gen_random_uuid(), 'update', 'upload',   'success', 'Отправлено в ГК: обновление товара', now() - interval '2 hours'),
  (gen_random_uuid(), 'product',   gen_random_uuid(), 'update', 'upload',   'error',   'Ошибка отправки: таймаут',          now() - interval '1 hour 50 minutes'),
  (gen_random_uuid(), 'orders',    gen_random_uuid(), 'insert', 'upload',   'success', 'Отправлен заказ в ГК',              now() - interval '1 hour 30 minutes'),
  (gen_random_uuid(), 'orders',    gen_random_uuid(), 'insert', 'upload',   'error',   'Ошибка валидации: нет контрагента', now() - interval '1 hour 20 minutes'),
  (gen_random_uuid(), 'contracts', gen_random_uuid(), 'update', 'download', 'success', 'Получен контракт из ГК',            now() - interval '55 minutes'),
  (gen_random_uuid(), 'contracts', gen_random_uuid(), 'update', 'download', 'error',   'Ошибка парсинга ответа ГК',         now() - interval '45 minutes'),
  (gen_random_uuid(), 'price',     gen_random_uuid(), 'update', 'upload',   'success', 'Выгружены цены',                    now() - interval '25 minutes'),
  (gen_random_uuid(), 'price',     gen_random_uuid(), 'update', 'upload',   'error',   'Ошибка: ограничение цен',           now() - interval '10 minutes');
