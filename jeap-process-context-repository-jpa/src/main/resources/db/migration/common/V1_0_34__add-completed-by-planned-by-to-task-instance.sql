ALTER TABLE task_instance ADD COLUMN planned_by UUID REFERENCES events (id);
ALTER TABLE task_instance ADD COLUMN completed_by UUID REFERENCES events (id);