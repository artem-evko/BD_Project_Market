DO $$
BEGIN
  -- Если таблица вообще отсутствует - создадим как надо
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_name = 'exchange_log'
  ) THEN
    CREATE TABLE exchange_log (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      entity_name VARCHAR(100) NOT NULL,
      entity_id UUID NOT NULL,
      operation VARCHAR(20) NOT NULL,
      direction VARCHAR(10) NOT NULL,
      status VARCHAR(20) NOT NULL,
      message TEXT,
      "timestamp" TIMESTAMPTZ NOT NULL DEFAULT now()
    );
    CREATE INDEX IF NOT EXISTS ix_exchange_log_time ON exchange_log("timestamp");
    RETURN;
  END IF;

  -- Если колонки ошибочно BYTEA — конвертнём в текст (UTF8)
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='exchange_log' AND column_name='entity_name' AND data_type='bytea') THEN
    ALTER TABLE exchange_log ALTER COLUMN entity_name TYPE VARCHAR(100) USING convert_from(entity_name, 'UTF8');
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='exchange_log' AND column_name='operation' AND data_type='bytea') THEN
    ALTER TABLE exchange_log ALTER COLUMN operation TYPE VARCHAR(20) USING convert_from(operation, 'UTF8');
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='exchange_log' AND column_name='direction' AND data_type='bytea') THEN
    ALTER TABLE exchange_log ALTER COLUMN direction TYPE VARCHAR(10) USING convert_from(direction, 'UTF8');
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='exchange_log' AND column_name='status' AND data_type='bytea') THEN
    ALTER TABLE exchange_log ALTER COLUMN status TYPE VARCHAR(20) USING convert_from(status, 'UTF8');
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='exchange_log' AND column_name='message' AND data_type='bytea') THEN
    ALTER TABLE exchange_log ALTER COLUMN message TYPE TEXT USING convert_from(message, 'UTF8');
  END IF;

  -- timestamp под Instant лучше timestamptz (если вдруг было без tz)
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name='exchange_log' AND column_name='timestamp' AND data_type='timestamp without time zone'
  ) THEN
    ALTER TABLE exchange_log ALTER COLUMN "timestamp" TYPE TIMESTAMPTZ USING "timestamp" AT TIME ZONE 'UTC';
  END IF;

  CREATE INDEX IF NOT EXISTS ix_exchange_log_time ON exchange_log("timestamp");
END $$;
