ALTER TABLE process_update
    ADD COLUMN failed boolean DEFAULT false;

UPDATE process_update SET failed=false;

ALTER TABLE process_update
    ALTER COLUMN failed SET NOT NULL;
