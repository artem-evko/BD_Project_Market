-- V3_4__update_table_employee.sql

ALTER TABLE employees
    ADD COLUMN storage_location_id UUID
        REFERENCES storage_locations(id);