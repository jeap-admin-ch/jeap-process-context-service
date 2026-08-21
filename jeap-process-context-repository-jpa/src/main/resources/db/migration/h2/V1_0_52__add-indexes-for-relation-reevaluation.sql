CREATE INDEX process_data_relation_key_id
    ON process_instance_process_data (process_instance_id, key_, id, value_, role);

CREATE INDEX process_data_relation_role_id
    ON process_instance_process_data (process_instance_id, key_, role, id, value_);
