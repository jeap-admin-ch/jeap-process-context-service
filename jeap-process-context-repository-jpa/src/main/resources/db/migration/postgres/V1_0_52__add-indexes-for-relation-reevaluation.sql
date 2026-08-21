CREATE INDEX CONCURRENTLY IF NOT EXISTS process_data_relation_key_id
    ON process_instance_process_data (process_instance_id, key_, id)
    INCLUDE (value_, role);

CREATE INDEX CONCURRENTLY IF NOT EXISTS process_data_relation_role_id
    ON process_instance_process_data (process_instance_id, key_, role, id)
    INCLUDE (value_);
