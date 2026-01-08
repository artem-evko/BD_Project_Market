ALTER TABLE employees
    ADD COLUMN storage_location_id UUID
        REFERENCES storage_locations(id);
