EXECUTE IMMEDIATE 'ALTER TABLE task_instance DROP CONSTRAINT ' ||
    QUOTE_IDENT((SELECT CONSTRAINT_NAME
        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
        WHERE TABLE_NAME = 'task_instance' AND CONSTRAINT_TYPE = 'UNIQUE'));

ALTER TABLE task_instance ADD CONSTRAINT task_instance_unique UNIQUE(process_instance_id, origin_task_id, task_type);
