-- Single-column index covered by composite index event_reference(process_instance_id, events_id)
DROP INDEX IF EXISTS event_reference_process_instance_id;

-- Single-column index covered by UNIQUE (origin_process_id, message_id)
DROP INDEX IF EXISTS pending_message_origin_process_id;

-- Single-column index covered by UNIQUE (event_name, idempotence_id), no query filters on idempotence_id alone
DROP INDEX IF EXISTS events_idempotence_id;

-- Single-column index redundant with implicit UNIQUE index from origin_process_id UNIQUE constraint
DROP INDEX IF EXISTS process_instance_origin_process_id;
