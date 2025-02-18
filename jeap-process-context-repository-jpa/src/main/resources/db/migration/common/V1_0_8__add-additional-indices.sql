-- process data
CREATE INDEX process_instance_process_data_key ON process_instance_process_data (key);
CREATE INDEX process_instance_process_data_value ON process_instance_process_data (value);

-- event data
CREATE INDEX events_event_data_key ON events_event_data (key);
CREATE INDEX events_event_data_value ON events_event_data (value);

-- fix index on events origin task ids
DROP INDEX origin_task_ids_events_id;
CREATE INDEX events_origin_task_ids_events_id ON events_origin_task_ids (events_id);
