ALTER TABLE process_instance
    ADD COLUMN template_hash varchar;

CREATE INDEX process_instance_template_hash ON process_instance (template_hash);
