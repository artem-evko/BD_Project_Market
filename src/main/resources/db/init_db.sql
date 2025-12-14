DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_catalog.pg_roles WHERE rolname = 'shop_user'
    ) THEN
        CREATE ROLE shop_user WITH LOGIN PASSWORD 'shop_password';
    END IF;

    IF NOT EXISTS (
        SELECT FROM pg_database WHERE datname = 'shop_db'
    ) THEN
        CREATE DATABASE shop_db OWNER shop_user;
    END IF;
END
$$;
