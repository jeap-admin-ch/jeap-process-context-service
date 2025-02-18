-- Not used anywhere
DROP INDEX IF EXISTS relation_idempotence_id;

-- Not used anywhere
DROP INDEX IF EXISTS events_event_id;

-- Not used anywhere, the index created for the constraint 'events_event_name_idempotence_id_unique' is instead used
DROP INDEX IF EXISTS events_event_name;

-- Used in migration service, housekeeping, process relations, message correlation by process data
DROP INDEX IF EXISTS process_instance_state;
-- Used in migration service
DROP INDEX IF EXISTS process_instance_template_hash;
-- Used in migration service, message correlation by process data, process relations service
DROP INDEX IF EXISTS process_instance_template_name;

-- Not used as index 'process_update_origin_process_id_event_name_idempotence_id_key' is used instead
DROP INDEX IF EXISTS process_update_event_name;
DROP INDEX IF EXISTS process_update_idempotence_id;

-- Housekeeping (Some in V1.0.23)
DROP INDEX IF EXISTS process_instance_modified_at; -- Housekeeping and ProcessInstanceMigrationService
DROP INDEX IF EXISTS events_created_at; --V1.0.23 speed up housekeeping
DROP INDEX IF EXISTS process_update_handled; --V1.0.23 speed up housekeeping
DROP INDEX IF EXISTS process_update_created_at; --V1.0.23 speed up housekeeping

-- Not used
DROP INDEX IF EXISTS task_instance_origin_task_id;
DROP INDEX IF EXISTS task_instance_task_type;
