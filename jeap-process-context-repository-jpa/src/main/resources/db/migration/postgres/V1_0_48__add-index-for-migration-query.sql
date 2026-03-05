-- Partial covering index for the migration query:
--   SELECT origin_process_id FROM process_instance
--   WHERE state <> 'COMPLETED' AND created_at > ? AND template_name = ? AND template_hash <> ?
-- Excludes completed instances (majority of rows) to keep the index small.
-- INCLUDE columns enable index-only scans without heap lookups.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_process_instance_migration
    ON process_instance (template_name, created_at)
    INCLUDE (template_hash, origin_process_id)
    WHERE state <> 'COMPLETED';
