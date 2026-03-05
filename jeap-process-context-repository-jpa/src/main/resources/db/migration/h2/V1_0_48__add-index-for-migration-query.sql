-- Composite index for the migration query (H2 does not support partial indexes or INCLUDE)
CREATE INDEX idx_process_instance_migration
    ON process_instance (template_name, created_at, state, template_hash);
