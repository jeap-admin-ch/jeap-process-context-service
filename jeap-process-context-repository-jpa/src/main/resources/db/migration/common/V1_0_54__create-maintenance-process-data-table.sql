CREATE TABLE pcs_maintenance_process_data
(
    id      UUID PRIMARY KEY,
    task_id UUID          NOT NULL REFERENCES pcs_maintenance_task (task_id) ON DELETE CASCADE,
    key_    VARCHAR(2000) NOT NULL,
    value_  VARCHAR(2000) NOT NULL,
    role_   VARCHAR(2000)
);

CREATE INDEX pcs_maintenance_process_data_task_id
    ON pcs_maintenance_process_data (task_id);
