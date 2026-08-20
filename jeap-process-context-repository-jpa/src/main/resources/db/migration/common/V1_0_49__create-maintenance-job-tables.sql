CREATE TABLE pcs_maintenance_job
(
    job_id                UUID PRIMARY KEY,
    job_type              VARCHAR(40)              NOT NULL,
    process_template_name VARCHAR(2000),
    request_hash          VARCHAR(64)              NOT NULL,
    job_state             VARCHAR(32)              NOT NULL,
    job_result            VARCHAR(32),
    started_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at          TIMESTAMP WITH TIME ZONE,
    started_by_name       VARCHAR(255),
    started_by_ext_id     VARCHAR(255),
    version               INTEGER                  NOT NULL DEFAULT 0
);

CREATE TABLE pcs_maintenance_task
(
    task_id           UUID PRIMARY KEY,
    job_id            UUID                     NOT NULL REFERENCES pcs_maintenance_job (job_id) ON DELETE CASCADE,
    target_type       VARCHAR(20)              NOT NULL,
    target_key        VARCHAR(2000)            NOT NULL,
    origin_process_id VARCHAR(2000),
    task_state        VARCHAR(32)              NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at       TIMESTAMP WITH TIME ZONE,
    error_message     TEXT,
    error_trace_id    VARCHAR(255),
    version           INTEGER                  NOT NULL DEFAULT 0,
    UNIQUE (job_id, target_type, target_key)
);

CREATE INDEX pcs_maintenance_task_job_id ON pcs_maintenance_task (job_id);
