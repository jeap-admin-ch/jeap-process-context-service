UPDATE pcs_maintenance_job
SET job_state = 'COMPLETED',
    job_result = 'FAILED',
    completed_at = CURRENT_TIMESTAMP
WHERE job_state = 'OPEN'
  AND EXISTS (SELECT 1
              FROM pcs_maintenance_task
              WHERE pcs_maintenance_task.job_id = pcs_maintenance_job.job_id
                AND task_state = 'CREATED');

UPDATE pcs_maintenance_task
SET task_state = 'FAILED',
    modified_at = CURRENT_TIMESTAMP,
    error_message = 'Job was submitted before maintenance execution was available'
WHERE task_state = 'CREATED';
