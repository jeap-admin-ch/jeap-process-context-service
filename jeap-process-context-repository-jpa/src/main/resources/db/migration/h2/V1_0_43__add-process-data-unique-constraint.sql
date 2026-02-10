ALTER TABLE process_instance_process_data
    ADD CONSTRAINT uc_process_data UNIQUE NULLS NOT DISTINCT (process_instance_id, key_, value_, role);
