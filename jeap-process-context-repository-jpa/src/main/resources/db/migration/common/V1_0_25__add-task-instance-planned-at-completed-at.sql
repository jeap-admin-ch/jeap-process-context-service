ALTER TABLE task_instance ADD COLUMN planned_at timestamp with time zone;
ALTER TABLE task_instance ADD COLUMN completed_at timestamp with time zone;
