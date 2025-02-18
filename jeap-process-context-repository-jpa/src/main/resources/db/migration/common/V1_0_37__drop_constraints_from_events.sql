ALTER TABLE events_event_data DROP CONSTRAINT IF EXISTS events_event_data_events_id_fkey;
ALTER TABLE events_origin_task_ids DROP CONSTRAINT IF EXISTS events_origin_task_ids_events_id_fkey;
ALTER TABLE events_user_data DROP CONSTRAINT IF EXISTS events_user_data_events_id_fkey;
ALTER TABLE task_instance DROP CONSTRAINT IF EXISTS task_instance_planned_by_fkey;
ALTER TABLE task_instance DROP CONSTRAINT IF EXISTS task_instance_completed_by_fkey;
