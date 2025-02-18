ALTER TABLE process_instance ADD COLUMN process_completion_conclusion varchar;
ALTER TABLE process_instance ADD COLUMN process_completion_reason varchar;
ALTER TABLE process_instance ADD COLUMN process_completion_at timestamp with time zone;
