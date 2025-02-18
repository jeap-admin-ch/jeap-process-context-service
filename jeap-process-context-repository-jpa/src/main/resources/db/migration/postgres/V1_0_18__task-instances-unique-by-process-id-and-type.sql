ALTER TABLE task_instance
    DROP CONSTRAINT task_instance_process_instance_id_origin_task_id_key;

ALTER TABLE task_instance
    ADD CONSTRAINT task_instance_unique UNIQUE (process_instance_id, origin_task_id, task_type);
