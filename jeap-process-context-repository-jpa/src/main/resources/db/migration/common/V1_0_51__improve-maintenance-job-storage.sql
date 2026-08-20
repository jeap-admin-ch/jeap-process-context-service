ALTER TABLE pcs_maintenance_job
    ALTER COLUMN started_by_name TYPE TEXT;

ALTER TABLE pcs_maintenance_job
    ALTER COLUMN started_by_ext_id TYPE TEXT;

CREATE INDEX pcs_maintenance_job_state_completed_at
    ON pcs_maintenance_job (job_state, completed_at);
