-- Composite index on event_reference for the common join pattern: filter by process_instance_id, join on events_id
-- Replaces the single-column index event_reference_process_instance_id
CREATE INDEX event_reference_process_instance_events_id
    ON event_reference (process_instance_id, events_id);

-- Composite index on events_event_data for the common join+filter pattern: join on events_id, filter by template_name
-- Replaces the single-column index event_data_events_id
CREATE INDEX events_event_data_events_id_template_name
    ON events_event_data (events_id, template_name);

-- Covering index on events (id, event_name) to enable Index Only Scans for countMessagesByType
-- and similar queries that join event_reference to events filtered by event_name
CREATE INDEX events_id_event_name
    ON events (id, event_name);
